/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientqueueing.web.resource;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAttributeType;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.customdto.CheckInPatient;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.module.patientqueueing.PatientQueueingConstants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * UgandaEMR-compatible endpoint for patient self check-in. Follows UgandaEMR pattern with
 * CheckInPatient DTO and DelegatingCrudResource extension. Resource path:
 * /ws/rest/v1/patientqueue/selfcheckinpatient Uses global properties to configure: - Person
 * attribute type for phone number lookup - Patient identifier types for identifier lookup
 */
@Resource(name = RestConstants.VERSION_1 + "/patientqueueing/selfcheckin", supportedClass = CheckInPatient.class, supportedOpenmrsVersions = { "1.9.* - 9.*" })
public class SelfCheckInPatientResource extends DelegatingCrudResource<CheckInPatient> {
	
	private static final Logger log = LoggerFactory.getLogger(SelfCheckInPatientResource.class);
	
	@Override
	public CheckInPatient newDelegate() {
		return new CheckInPatient();
	}
	
	@Override
	public CheckInPatient save(CheckInPatient delegate) {
		throw new ResourceDoesNotSupportOperationException("Use create() instead");
	}
	
	@Override
	public Object create(SimpleObject propertiesToCreate, RequestContext context) throws ResponseException {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		// Get patient identification using UUID-based approach
		String identifierTypeUuid = trimToNull(propertiesToCreate.get("identifierTypeUuid"));
		String identifierValue = trimToNull(propertiesToCreate.get("identifierValue"));
		
		if (identifierTypeUuid == null || identifierValue == null) {
			throw new IllegalArgumentException("identifierTypeUuid and identifierValue are required");
		}
		
		// Resolve patient using UUID-based lookup
		Patient patient = resolvePatientByUuid(identifierTypeUuid, identifierValue);
		if (patient == null) {
			throw new IllegalArgumentException("No patient found matching the provided details");
		}
		
		// Get location parameters
		String currentLocationUuid = getRequiredProperty(propertiesToCreate, "currentLocation");
		String locationToUuid = getRequiredProperty(propertiesToCreate, "locationTo");
		String queueRoomUuid = getRequiredProperty(propertiesToCreate, "queueRoom");
		String providerUuid = trimToNull(propertiesToCreate.get("provider"));
		String patientStatus = getRequiredProperty(propertiesToCreate, "patientStatus");
		String visitType = getRequiredProperty(propertiesToCreate, "visitType");
		String visitComment = trimToNull(propertiesToCreate.get("visitComment"));
		Integer priority = parsePriority(propertiesToCreate.get("priority"));
		
		// Resolve locations
		Location currentLocation = Context.getLocationService().getLocationByUuid(currentLocationUuid);
		Location locationTo = Context.getLocationService().getLocationByUuid(locationToUuid);
		Location queueRoom = Context.getLocationService().getLocationByUuid(queueRoomUuid);
		
		if (currentLocation == null) {
			throw new IllegalArgumentException("Current Location not found for UUID: " + currentLocationUuid);
		}
		if (locationTo == null) {
			throw new IllegalArgumentException("LocationTo not found for UUID: " + locationToUuid);
		}
		if (queueRoom == null) {
			throw new IllegalArgumentException("Queue Room not found for UUID: " + queueRoomUuid);
		}
		
		// Create patient queue
		PatientQueue patientQueue = new PatientQueue();
		patientQueue.setPatient(patient);
		patientQueue.setLocationFrom(currentLocation);
		patientQueue.setLocationTo(locationTo);
		patientQueue.setQueueRoom(queueRoom);
		patientQueue.setStatus(PatientQueue.Status.PENDING);
		patientQueue.setDateCreated(new Date());
		
		// Generate visit number (ticket number)
		String visitNumber = service.generateVisitNumber(locationTo, patient);
		patientQueue.setVisitNumber(visitNumber);
		
		// Start a new visit for the patient (close any active visit first)
		Visit visit = ensurePatientVisit(patient, locationTo);
		
		// Save the queue entry
		patientQueue = service.savePatientQue(patientQueue);
		
		// Build response manually for better control
		SimpleObject response = new SimpleObject();
		response.add("uuid", patientQueue.getUuid());
		response.add("patientQueue", buildPatientQueueObject(patientQueue));
		response.add("patient", buildPatientObject(patient));
		response.add("visit", visit != null ? buildVisitObject(visit) : null);
		response.add("ticketNumber", visitNumber);
		response.add("visitNumber", visitNumber);
		
		if (hasTypesDefined()) {
			response.add(RestConstants.PROPERTY_FOR_TYPE, "CheckInPatient");
		}
		
		return response;
	}
	
	/**
	 * Ensure patient has an active visit. If an active visit exists, reject check-in and ask to go
	 * to reception. Otherwise, start a new visit using configured visit type.
	 */
	private Visit ensurePatientVisit(Patient patient, Location location) {
		try {
			AdministrationService administrationService = Context.getAdministrationService();
			String visitTypeUuid = administrationService.getGlobalProperty(
			    PatientQueueingConstants.GP_SELF_CHECK_IN_VISIT_TYPE_UUID, "");
			
			// If no visit type is configured, skip visit creation
			if (StringUtils.isBlank(visitTypeUuid)) {
				log.info("No visit type configured for self check-in, skipping visit creation");
				return null;
			}
			
			VisitService visitService = Context.getVisitService();
			
			// Check for active visits - if any exist, reject the check-in
			List<Visit> activeVisits = visitService.getActiveVisitsByPatient(patient);
			if (activeVisits != null && !activeVisits.isEmpty()) {
				log.info("Patient " + patient.getPatientId() + " has " + activeVisits.size()
				        + " active visit(s). Rejecting self check-in and asking to go to reception.");
				throw new IllegalArgumentException(
				        "You already have an active visit. Please go to the reception for assistance.");
			}
			
			// Start a new visit using the configured visit type
			VisitType visitType = Context.getVisitService().getVisitTypeByUuid(visitTypeUuid);
			if (visitType == null) {
				log.error("Configured visit type not found: " + visitTypeUuid);
				throw new IllegalArgumentException("Visit type configuration error. Please contact reception.");
			}
			
			Visit visit = new Visit();
			visit.setPatient(patient);
			visit.setVisitType(visitType);
			// Set visit start to beginning of day to avoid conflicts with encounter timestamps
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			visit.setStartDatetime(calendar.getTime());
			visit.setLocation(location);
			
			Visit savedVisit = visitService.saveVisit(visit);
			log.info("Started new visit " + savedVisit.getUuid() + " of type " + visitType.getName() + " for patient "
			        + patient.getPatientId());
			return savedVisit;
		}
		catch (IllegalArgumentException e) {
			// Re-throw validation exceptions
			throw e;
		}
		catch (Exception e) {
			log.error("Error ensuring patient visit: " + e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Get the configured visit type for self check-in visits
	 */
	private VisitType getVisitType() {
		try {
			AdministrationService administrationService = Context.getAdministrationService();
			String visitTypeUuid = administrationService.getGlobalProperty(
			    PatientQueueingConstants.GP_SELF_CHECK_IN_VISIT_TYPE_UUID, "");
			
			if (StringUtils.isBlank(visitTypeUuid)) {
				return null;
			}
			
			return Context.getVisitService().getVisitTypeByUuid(visitTypeUuid);
		}
		catch (Exception e) {
			log.warn("Error getting visit type: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Build a SimpleObject representation of a PatientQueue
	 */
	private SimpleObject buildPatientQueueObject(PatientQueue queue) {
		SimpleObject queueObj = new SimpleObject();
		queueObj.add("uuid", queue.getUuid());
		queueObj.add("visitNumber", queue.getVisitNumber());
		queueObj.add("status", queue.getStatus());
		queueObj.add("dateCreated", queue.getDateCreated());
		return queueObj;
	}
	
	/**
	 * Build a SimpleObject representation of a Patient
	 */
	private SimpleObject buildPatientObject(Patient patient) {
		SimpleObject patientObj = new SimpleObject();
		patientObj.add("uuid", patient.getUuid());
		patientObj.add("display", patient.getPersonName().getFullName());
		// Add patient identifier if available
		if (patient.getPatientIdentifier() != null) {
			patientObj.add("patientId", patient.getPatientIdentifier().getIdentifier());
		}
		return patientObj;
	}
	
	/**
	 * Build a SimpleObject representation of a Visit
	 */
	private SimpleObject buildVisitObject(Visit visit) {
		SimpleObject visitObj = new SimpleObject();
		visitObj.add("uuid", visit.getUuid());
		visitObj.add("startDatetime", visit.getStartDatetime());
		visitObj.add("stopDatetime", visit.getStopDatetime());
		if (visit.getVisitType() != null) {
			visitObj.add("visitType", visit.getVisitType().getName());
		}
		return visitObj;
	}
	
	/**
	 * Resolve patient using UUID-based lookup strategy. This checks if the provided UUID is a
	 * configured attribute type or identifier type, then searches accordingly.
	 */
	private Patient resolvePatientByUuid(String identifierTypeUuid, String identifierValue) {
		AdministrationService administrationService = Context.getAdministrationService();
		
		// Get configured person attribute type UUIDs from global property
		String personAttributeTypeUuidsStr = administrationService.getGlobalProperty(
		    PatientQueueingConstants.GP_SELF_CHECK_IN_PERSON_ATTRIBUTE_TYPE_UUIDS,
		    PatientQueueingConstants.DEFAULT_PHONE_ATTRIBUTE_TYPE_UUID);
		
		// Get configured identifier type UUIDs from global property
		String identifierTypeUuidsStr = administrationService.getGlobalProperty(
		    PatientQueueingConstants.GP_SELF_CHECK_IN_IDENTIFIER_TYPE_UUIDS,
		    PatientQueueingConstants.DEFAULT_PATIENT_ID_IDENTIFIER_TYPE_UUID + ","
		            + PatientQueueingConstants.DEFAULT_NATIONAL_ID_IDENTIFIER_TYPE_UUID);
		
		// Check if it's a configured attribute type (person attribute like phone number)
		if (Arrays.asList(personAttributeTypeUuidsStr.split(",")).contains(identifierTypeUuid.trim())) {
			log.info("Looking up patient by person attribute type: " + identifierTypeUuid);
			Patient patient = getPatientByPersonAttributes(identifierValue, personAttributeTypeUuidsStr);
			if (patient != null) {
				return patient;
			}
		}
		
		// Check if it's a configured identifier type (patient identifier like National ID)
		if (Arrays.asList(identifierTypeUuidsStr.split(",")).contains(identifierTypeUuid.trim())) {
			log.info("Looking up patient by identifier type: " + identifierTypeUuid);
			Patient patient = getPatientByIdentifier(identifierValue, identifierTypeUuid);
			if (patient != null) {
				return patient;
			}
		}
		
		log.warn("Identifier type UUID not found in configured types: " + identifierTypeUuid);
		return null;
	}
	
	/**
	 * Find patient by person attribute value using configured attribute types (supports multiple)
	 */
	private Patient getPatientByPersonAttributes(String attributeValue, String attributeTypeUuidsStr) {
		try {
			String[] uuids = attributeTypeUuidsStr.split(",");
			
			for (String uuid : uuids) {
				uuid = uuid.trim();
				if (StringUtils.isBlank(uuid)) {
					continue;
				}
				
				PersonAttributeType attributeType = Context.getPersonService().getPersonAttributeTypeByUuid(uuid);
				if (attributeType == null) {
					log.warn("PersonAttributeType not found for UUID: " + uuid);
					continue;
				}
				
				// Search patients by person attribute - iterate through all patients
				List<Patient> allPatients = Context.getPatientService().getAllPatients(false);
				if (allPatients == null || allPatients.isEmpty()) {
					continue;
				}
				
				List<Patient> matchingPatients = new ArrayList<Patient>();
				for (Patient p : allPatients) {
					if (p.getPerson() != null && p.getPerson().getAttribute(attributeType) != null) {
						String attrValue = p.getPerson().getAttribute(attributeType).getValue();
						if (attributeValue.equals(attrValue)) {
							matchingPatients.add(p);
						}
					}
				}
				
				if (!matchingPatients.isEmpty()) {
					if (matchingPatients.size() > 1) {
						log.warn("Multiple patients found for attribute value: " + attributeValue + ", returning first");
					}
					return matchingPatients.get(0);
				}
			}
			
			return null;
		}
		catch (Exception e) {
			log.error("Error finding patient by person attributes", e);
			return null;
		}
	}
	
	/**
	 * Find patient by identifier using configured identifier types
	 */
	private Patient getPatientByIdentifierTypes(String identifierValue, String identifierTypeUuidsStr) {
		try {
			String[] uuids = identifierTypeUuidsStr.split(",");
			
			for (String uuid : uuids) {
				uuid = uuid.trim();
				if (StringUtils.isBlank(uuid)) {
					continue;
				}
				
				Patient patient = getPatientByIdentifier(identifierValue, uuid);
				if (patient != null) {
					return patient;
				}
			}
		}
		catch (Exception e) {
			log.error("Error finding patient by identifier", e);
		}
		
		return null;
	}
	
	/**
	 * Find patient by specific identifier type
	 */
	private Patient getPatientByIdentifier(String identifierValue, String identifierTypeUuid) {
		try {
			List<org.openmrs.PatientIdentifier> identifiers = Context.getPatientService().getPatientIdentifiers(
			    identifierValue,
			    Arrays.asList(Context.getPatientService().getPatientIdentifierTypeByUuid(identifierTypeUuid)), null, null,
			    null);
			
			if (identifiers == null || identifiers.isEmpty()) {
				return null;
			}
			
			if (identifiers.size() > 1) {
				log.warn("Multiple patients found for identifier: " + identifierValue + " in type " + identifierTypeUuid);
			}
			
			return identifiers.get(0).getPatient();
		}
		catch (Exception e) {
			log.error("Error finding patient by identifier type: " + identifierTypeUuid, e);
			return null;
		}
	}
	
	private Integer parsePriority(Object priorityValue) {
		if (priorityValue == null) {
			return null;
		}
		
		try {
			return Integer.parseInt(priorityValue.toString());
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private String getRequiredProperty(SimpleObject properties, String key) {
		String value = properties.get(key);
		if (value == null) {
			throw new IllegalArgumentException(key + " cannot be null");
		}
		return value;
	}
	
	private String trimToNull(Object value) {
		if (value == null) {
			return null;
		}
		
		String s = value.toString().trim();
		return s.isEmpty() ? null : s;
	}
	
	@Override
	public Object update(String uuid, SimpleObject propertiesToUpdate, RequestContext context) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException("Operation not supported");
	}
	
	@Override
	public CheckInPatient getByUniqueId(String uniqueId) {
		throw new ResourceDoesNotSupportOperationException("Operation not supported");
	}
	
	@Override
	public NeedsPaging<CheckInPatient> doGetAll(RequestContext context) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException("Operation not supported");
	}
	
	@Override
	public List<Representation> getAvailableRepresentations() {
		return Arrays.asList(Representation.DEFAULT, Representation.FULL);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");
		description.addProperty("patientQueue");
		description.addProperty("patient");
		description.addProperty("visit");
		description.addSelfLink();
		
		if (rep instanceof FullRepresentation) {
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		}
		
		return description;
	}
	
	@Override
	protected void delete(CheckInPatient delegate, String s, RequestContext requestContext) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException("Operation not supported");
	}
	
	@Override
	public void purge(CheckInPatient delegate, RequestContext requestContext) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException("Operation not supported");
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("identifierTypeUuid");
		description.addProperty("identifierValue");
		description.addProperty("currentLocation");
		description.addProperty("locationTo");
		description.addProperty("queueRoom");
		description.addProperty("provider");
		description.addProperty("patientStatus");
		description.addProperty("visitType");
		description.addProperty("visitComment");
		description.addProperty("priority");
		return description;
	}
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		throw new ResourceDoesNotSupportOperationException("Operation not supported");
	}
}
