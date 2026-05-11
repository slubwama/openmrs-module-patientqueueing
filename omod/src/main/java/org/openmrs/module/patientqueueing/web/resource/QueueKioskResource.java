package org.openmrs.module.patientqueueing.web.resource;

import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientqueueing.PatientQueueingConstants;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.web.customdto.QueueEntry;
import org.openmrs.module.patientqueueing.model.NonPatientQueue;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.util.OpenmrsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST resource for queue kiosk displays Provides unified access to both patient and non-patient
 * queues Supports ticket lookup by ticket number Handles both our clean API and
 * UgandaEMR-compatible requests for backward compatibility
 */
@Resource(name = RestConstants.VERSION_1 + "/kiosk", supportedClass = Object.class, supportedOpenmrsVersions = { "1.9.* - 9.*" })
public class QueueKioskResource {
	
	/**
	 * Lookup patient by identifier or phone number before check-in. Used by kiosk to verify patient
	 * identity before creating queue entry. GET /kiosk/lookup?query=xxx&location=xxx
	 * 
	 * @param context the request context containing query and location parameters
	 * @return SimpleObject with found status, patient uuid, display name, and identifiers
	 */
	
	public SimpleObject lookupPatient(RequestContext context) {
		String query = context.getParameter("query");
		String locationUuid = context.getParameter("location");
		
		SimpleObject result = new SimpleObject();
		
		if (query == null || query.trim().isEmpty()) {
			result.put("found", false);
			result.put("message", "Query parameter is required");
			return result;
		}
		
		query = query.trim();
		
		try {
			// Try to find patient by configured identifier types first
			Patient patient = findPatientByIdentifier(query);
			
			// If not found, try person attributes (phone number)
			if (patient == null) {
				patient = findPatientByPersonAttribute(query);
			}
			
			if (patient != null) {
				result.put("found", true);
				result.put("uuid", patient.getUuid());
				result.put("displayName", patient.getPersonName().getFullName());
				result.put("identifiers", buildIdentifiersList(patient));
			} else {
				result.put("found", false);
				result.put("message", "Patient not found");
			}
		}
		catch (APIException e) {
			result.put("found", false);
			result.put("message", "Lookup failed: " + e.getMessage());
		}
		
		return result;
	}
	
	/**
	 * Find patient by configured identifier types
	 */
	private Patient findPatientByIdentifier(String identifierValue) {
		AdministrationService administrationService = Context.getService(AdministrationService.class);
		
		// Get configured identifier type UUIDs from global property
		String identifierTypeUuidsStr = administrationService.getGlobalProperty(
		    PatientQueueingConstants.GP_SELF_CHECK_IN_IDENTIFIER_TYPE_UUIDS,
		    PatientQueueingConstants.DEFAULT_PATIENT_ID_IDENTIFIER_TYPE_UUID + ","
		            + PatientQueueingConstants.DEFAULT_NATIONAL_ID_IDENTIFIER_TYPE_UUID);
		
		String[] uuids = identifierTypeUuidsStr.split(",");
		for (String uuid : uuids) {
			uuid = uuid.trim();
			if (uuid.isEmpty()) {
				continue;
			}
			
			try {
				List<PatientIdentifier> identifiers = Context.getPatientService().getPatientIdentifiers(identifierValue,
				    Collections.singletonList(Context.getPatientService().getPatientIdentifierTypeByUuid(uuid)), null, null,
				    null);
				
				if (identifiers != null && !identifiers.isEmpty()) {
					if (identifiers.size() > 1) {
						// Multiple found - return first but log warning
						// Let caller handle multiple match scenario
					}
					return identifiers.get(0).getPatient();
				}
			}
			catch (Exception e) {
				// Continue to next identifier type
			}
		}
		
		return null;
	}
	
	/**
	 * Find patient by person attributes (phone number)
	 */
	private Patient findPatientByPersonAttribute(String attributeValue) {
		AdministrationService administrationService = Context.getService(AdministrationService.class);
		
		// Get configured person attribute type UUIDs from global property
		String personAttributeTypeUuidsStr = administrationService.getGlobalProperty(
		    PatientQueueingConstants.GP_SELF_CHECK_IN_PERSON_ATTRIBUTE_TYPE_UUIDS,
		    PatientQueueingConstants.DEFAULT_PHONE_ATTRIBUTE_TYPE_UUID);
		
		String[] uuids = personAttributeTypeUuidsStr.split(",");
		
		for (String uuid : uuids) {
			uuid = uuid.trim();
			if (uuid.isEmpty()) {
				continue;
			}
			
			try {
				PersonAttributeType attributeType = Context.getPersonService().getPersonAttributeTypeByUuid(uuid);
				if (attributeType == null) {
					continue;
				}
				
				// Search patients by person attribute
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
						// Multiple found - return first but log warning
					}
					return matchingPatients.get(0);
				}
			}
			catch (Exception e) {
				// Continue to next attribute type
			}
		}
		
		return null;
	}
	
	/**
	 * Build list of patient identifiers for display
	 */
	private List<Map<String, String>> buildIdentifiersList(Patient patient) {
		List<Map<String, String>> identifiers = new ArrayList<Map<String, String>>();
		
		if (patient.getActiveIdentifiers() != null) {
			for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
				Map<String, String> idMap = new HashMap<String, String>();
				idMap.put("type", identifier.getIdentifierType().getName());
				idMap.put("value", identifier.getIdentifier());
				identifiers.add(idMap);
			}
		}
		
		return identifiers;
	}
	
	/**
	 * Get available locations for kiosk check-in based on configured location tag. Returns only
	 * locations that have the configured kiosk location tag. GET /kiosk/locations
	 * 
	 * @param context the request context
	 * @return SimpleObject with list of locations
	 */
	
	public SimpleObject getKioskLocations(RequestContext context) {
		String tagUuid = Context.getService(AdministrationService.class).getGlobalProperty(
		    PatientQueueingConstants.GP_KIOSK_LOCATION_TAG_UUID);
		
		SimpleObject result = new SimpleObject();
		List<Map<String, Object>> locations = new ArrayList<Map<String, Object>>();
		
		if (tagUuid != null && !tagUuid.isEmpty()) {
			LocationTag tag = Context.getLocationService().getLocationTagByUuid(tagUuid);
			if (tag != null) {
				List<Location> allLocations = Context.getLocationService().getAllLocations(false);
				for (Location loc : allLocations) {
					if (loc.getTags() != null && loc.getTags().contains(tag)) {
						Map<String, Object> locMap = new HashMap<String, Object>();
						locMap.put("uuid", loc.getUuid());
						locMap.put("name", loc.getName());
						locMap.put("description", loc.getDescription());
						locations.add(locMap);
					}
				}
			}
		}
		
		result.put("locations", locations);
		result.put("count", locations.size());
		return result;
	}
	
	/**
	 * Get the least busy location from a list of locations with a given tag. Used for load
	 * balancing when creating queue entries.
	 * 
	 * @param tagUuid the location tag UUID
	 * @return the location with the fewest pending queue entries, or null if no locations found
	 */
	public Location getLeastBusyLocationByTag(String tagUuid) {
		if (tagUuid == null || tagUuid.isEmpty()) {
			return null;
		}
		
		LocationTag tag = Context.getLocationService().getLocationTagByUuid(tagUuid);
		if (tag == null) {
			return null;
		}
		
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		Date today = new Date();
		Date fromDate = OpenmrsUtil.firstSecondOfDay(today);
		Date toDate = OpenmrsUtil.getLastMomentOfDay(today);
		
		// Get all locations with this tag
		List<Location> taggedLocations = new ArrayList<Location>();
		for (Location loc : Context.getLocationService().getAllLocations(false)) {
			if (loc.getTags() != null && loc.getTags().contains(tag)) {
				taggedLocations.add(loc);
			}
		}
		
		if (taggedLocations.isEmpty()) {
			return null;
		}
		
		// If only one location, return it
		if (taggedLocations.size() == 1) {
			return taggedLocations.get(0);
		}
		
		// Find location with least pending queues
		Location leastBusy = null;
		int minCount = Integer.MAX_VALUE;
		
		for (Location loc : taggedLocations) {
			List<PatientQueue> queues = service.getPatientQueueList(null, fromDate, toDate, loc, null, null, null);
			int pendingCount = 0;
			for (PatientQueue q : queues) {
				if (q.getStatus() == PatientQueue.Status.PENDING) {
					pendingCount++;
				}
			}
			
			if (pendingCount < minCount) {
				minCount = pendingCount;
				leastBusy = loc;
			}
		}
		
		return leastBusy;
	}
	
	/**
	 * Main GET method - handles both our clean API and UgandaEMR-compatible requests UgandaEMR
	 * requests use "visitNumber" parameter and expect wrapped response Our API uses "ticketNumber"
	 * and returns QueueEntry directly
	 */
	public Object get(RequestContext context) {
		// Detect UgandaEMR request by parameter names
		String visitNumber = context.getParameter("visitNumber");
		String ticketNumber = context.getParameter("ticketNumber");
		
		if (visitNumber != null) {
			// UgandaEMR format request - return wrapped response
			return getKioskStatusUgandaEMRStyle(context);
		} else {
			// Our clean API format - return QueueEntry directly
			return getQueueByTicketNumber(context);
		}
	}
	
	/**
	 * UgandaEMR-compatible kiosk status lookup Returns wrapped response with results array
	 */
	private Object getKioskStatusUgandaEMRStyle(RequestContext context) {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		// Support both parameter names for compatibility
		String visitNumber = context.getParameter("visitNumber");
		String ticketNumberParam = context.getParameter("ticketNumber");
		
		String ticket = (visitNumber != null) ? visitNumber : ticketNumberParam;
		
		if (ticket == null || ticket.isEmpty()) {
			throw new IllegalArgumentException("visitNumber or ticketNumber parameter required");
		}
		
		Date today = new Date();
		Date fromDate = OpenmrsUtil.firstSecondOfDay(today);
		Date toDate = OpenmrsUtil.getLastMomentOfDay(today);
		
		// Search in PatientQueue
		List<PatientQueue> patientQueues = service.getPatientQueueByVisitNumber(ticket, fromDate, toDate);
		
		// Search in NonPatientQueue
		List<NonPatientQueue> nonPatientQueues = service.getNonPatientQueueByTicketNumber(ticket, fromDate, toDate);
		
		// Combine results into unified list
		List<Object> results = new ArrayList<Object>();
		
		for (PatientQueue pq : patientQueues) {
			results.add(createKioskStatusFromPatientQueue(pq));
		}
		
		for (NonPatientQueue npq : nonPatientQueues) {
			results.add(createKioskStatusFromNonPatientQueue(npq));
		}
		
		// Wrap in results array like UgandaEMR
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("results", results);
		
		return response;
	}
	
	/**
	 * Search for a queue entry by ticket number This searches both patient and non-patient queues
	 * 
	 * @param ticketNumber the ticket number to search for
	 * @param dateFrom optional start date (timestamp in milliseconds)
	 * @param dateTo optional end date (timestamp in milliseconds)
	 * @return the QueueEntry if found
	 */
	public QueueEntry getQueueByTicketNumber(RequestContext context) {
		String ticketNumber = context.getParameter("ticketNumber");
		
		if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
			throw new IllegalArgumentException("ticketNumber parameter is required");
		}
		
		// Parse date parameters
		Long dateFromMs = null;
		Long dateToMs = null;
		String dateFromParam = context.getParameter("dateFrom");
		String dateToParam = context.getParameter("dateTo");
		
		if (dateFromParam != null && !dateFromParam.isEmpty()) {
			try {
				dateFromMs = Long.parseLong(dateFromParam);
			}
			catch (NumberFormatException e) {
				// If invalid, use start of today
				dateFromMs = OpenmrsUtil.firstSecondOfDay(new Date()).getTime();
			}
		} else {
			// Default to start of today
			dateFromMs = OpenmrsUtil.firstSecondOfDay(new Date()).getTime();
		}
		
		if (dateToParam != null && !dateToParam.isEmpty()) {
			try {
				dateToMs = Long.parseLong(dateToParam);
			}
			catch (NumberFormatException e) {
				// If invalid, use end of today
				dateToMs = OpenmrsUtil.getLastMomentOfDay(new Date()).getTime();
			}
		} else {
			// Default to end of today
			dateToMs = OpenmrsUtil.getLastMomentOfDay(new Date()).getTime();
		}
		
		Date fromDate = new Date(dateFromMs);
		Date toDate = new Date(dateToMs);
		
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		// Search in patient queues
		List<PatientQueue> patientQueues = service.getPatientQueueByVisitNumber(ticketNumber, fromDate, toDate);
		
		// Search in non-patient queues
		List<NonPatientQueue> nonPatientQueues = service.getNonPatientQueueByTicketNumber(ticketNumber, fromDate, toDate);
		
		// Check results
		if (!patientQueues.isEmpty() && !nonPatientQueues.isEmpty()) {
			// Both found - this shouldn't happen with proper ticket numbering
			throw new IllegalArgumentException("Multiple queue entries found for ticket number: " + ticketNumber);
		}
		
		if (!patientQueues.isEmpty()) {
			// Return patient queue entry
			return new QueueEntry(patientQueues.get(0));
		}
		
		if (!nonPatientQueues.isEmpty()) {
			// Return non-patient queue entry
			return new QueueEntry(nonPatientQueues.get(0));
		}
		
		// Not found
		throw new IllegalArgumentException("No queue entry found for ticket number: " + ticketNumber);
	}
	
	/**
	 * Get all queue entries for a specific location (facility or room) Returns both patient and
	 * non-patient queues, sorted by creation date
	 * 
	 * @param context the request context
	 * @return list of QueueEntry objects
	 */
	public List<QueueEntry> getAllQueueEntries(RequestContext context) {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		String locationUuid = context.getParameter("location");
		String queueRoomUuid = context.getParameter("queueRoom");
		String statusParam = context.getParameter("status");
		String dateFromParam = context.getParameter("dateFrom");
		String dateToParam = context.getParameter("dateTo");
		
		// Parse date parameters
		Date fromDate = null;
		Date toDate = null;
		
		if (dateFromParam != null && !dateFromParam.isEmpty()) {
			try {
				fromDate = new Date(Long.parseLong(dateFromParam));
			}
			catch (NumberFormatException e) {
				fromDate = OpenmrsUtil.firstSecondOfDay(new Date());
			}
		} else {
			fromDate = OpenmrsUtil.firstSecondOfDay(new Date());
		}
		
		if (dateToParam != null && !dateToParam.isEmpty()) {
			try {
				toDate = new Date(Long.parseLong(dateToParam));
			}
			catch (NumberFormatException e) {
				toDate = OpenmrsUtil.getLastMomentOfDay(new Date());
			}
		} else {
			toDate = OpenmrsUtil.getLastMomentOfDay(new Date());
		}
		
		Location location = null;
		Location queueRoom = null;
		
		if (locationUuid != null && !locationUuid.isEmpty()) {
			location = Context.getLocationService().getLocationByUuid(locationUuid);
		}
		
		if (queueRoomUuid != null && !queueRoomUuid.isEmpty()) {
			queueRoom = Context.getLocationService().getLocationByUuid(queueRoomUuid);
		}
		
		List<QueueEntry> entries = new ArrayList<QueueEntry>();
		
		// Get patient queues
		List<PatientQueue> patientQueues = service.getPatientQueueList(null, fromDate, toDate, null, null, null,
		    getPatientQueueStatus(statusParam), queueRoom);
		
		for (PatientQueue pq : patientQueues) {
			if (location == null || (location.equals(pq.getLocationTo()) || location.equals(pq.getQueueRoom()))) {
				entries.add(new QueueEntry(pq));
			}
		}
		
		// Get non-patient queues
		List<NonPatientQueue> nonPatientQueues = service.getNonPatientQueues(getNonPatientQueueStatus(statusParam), null,
		    location, queueRoom, fromDate, toDate);
		
		for (NonPatientQueue npq : nonPatientQueues) {
			entries.add(new QueueEntry(npq));
		}
		
		// Sort by date created (oldest first for FIFO)
		Collections.sort(entries, new Comparator<QueueEntry>() {
			
			@Override
			public int compare(QueueEntry e1, QueueEntry e2) {
				return e1.getDateCreated().compareTo(e2.getDateCreated());
			}
		});
		
		return entries;
	}
	
	/**
	 * Parse patient queue status string to enum
	 */
	private PatientQueue.Status getPatientQueueStatus(String status) {
		if (status == null || status.isEmpty()) {
			return null;
		}
		try {
			return PatientQueue.Status.valueOf(status.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	/**
	 * Parse non-patient queue status string to enum
	 */
	private NonPatientQueue.NonPatientQueueStatus getNonPatientQueueStatus(String status) {
		if (status == null || status.isEmpty()) {
			return null;
		}
		try {
			return NonPatientQueue.NonPatientQueueStatus.valueOf(status.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	/**
	 * Create UgandaEMR-compatible kiosk status from PatientQueue
	 */
	private Map<String, Object> createKioskStatusFromPatientQueue(PatientQueue pq) {
		Map<String, Object> status = new HashMap<String, Object>();
		status.put("uuid", pq.getUuid());
		status.put("ticketNumber", pq.getVisitNumber());
		status.put("visitNumber", pq.getVisitNumber());
		status.put("status", pq.getStatus().name());
		status.put("queueLocation", pq.getLocationTo() != null ? pq.getLocationTo().getDisplayString() : null);
		status.put("serviceLocation", pq.getQueueRoom() != null ? pq.getQueueRoom().getDisplayString() : null);
		status.put("checkedInAt", pq.getDateCreated() != null ? pq.getDateCreated().toString() : null);
		status.put("datePicked", pq.getDateChanged() != null ? pq.getDateChanged().toString() : null);
		status.put("dateCreated", pq.getDateCreated() != null ? pq.getDateCreated().toString() : null);
		status.put("dateCompleted", pq.getStatus() == PatientQueue.Status.COMPLETED ? pq.getDateChanged() != null ? pq
		        .getDateChanged().toString() : null : null);
		
		// Add nested objects like UgandaEMR
		status.put("locationTo", createLocationRef(pq.getLocationTo()));
		status.put("queueRoom", createLocationRef(pq.getQueueRoom()));
		status.put("displayName", pq.getPatient() != null ? pq.getPatient().getPersonName().getFullName() : null);
		status.put("queueType", "PATIENT");
		
		return status;
	}
	
	/**
	 * Create UgandaEMR-compatible kiosk status from NonPatientQueue
	 */
	private Map<String, Object> createKioskStatusFromNonPatientQueue(NonPatientQueue npq) {
		Map<String, Object> status = new HashMap<String, Object>();
		status.put("uuid", npq.getUuid());
		status.put("ticketNumber", npq.getTicketNumber());
		status.put("status", npq.getStatus().name());
		status.put("queueLocation", npq.getLocationTo() != null ? npq.getLocationTo().getDisplayString() : null);
		status.put("serviceLocation", npq.getQueueRoom() != null ? npq.getQueueRoom().getDisplayString() : null);
		status.put("checkedInAt", npq.getDateCreated() != null ? npq.getDateCreated().toString() : null);
		status.put("dateCreated", npq.getDateCreated() != null ? npq.getDateCreated().toString() : null);
		status.put("dateCompleted",
		    npq.getStatus() == NonPatientQueue.NonPatientQueueStatus.COMPLETED ? npq.getDateChanged() != null ? npq
		            .getDateChanged().toString() : null : null);
		status.put("displayName", npq.getDisplayName());
		status.put("queueType", "NON_PATIENT");
		
		// Add nested objects like UgandaEMR
		status.put("locationTo", createLocationRef(npq.getLocationTo()));
		status.put("queueRoom", createLocationRef(npq.getQueueRoom()));
		
		return status;
	}
	
	/**
	 * Create location reference object for UgandaEMR compatibility
	 */
	private Map<String, Object> createLocationRef(Location location) {
		if (location == null) {
			return null;
		}
		
		Map<String, Object> ref = new HashMap<String, Object>();
		ref.put("uuid", location.getUuid());
		ref.put("display", location.getDisplayString());
		ref.put("name", location.getName());
		return ref;
	}
}
