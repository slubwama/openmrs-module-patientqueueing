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

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.customdto.ProviderQueueEntry;
import org.openmrs.module.patientqueueing.model.NonPatientQueue;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider queue management endpoint. Provides UgandaEMR-compatible provider dashboard
 * functionality. Paths: - GET /ws/rest/v1/providerqueuebylocation?uuid=... - POST
 * /ws/rest/v1/providerqueuebylocation/:uuid?action=... Supports actions: call, start, complete,
 * forward, skip Unifies patient and non-patient queues for provider view.
 */
@Resource(name = RestConstants.VERSION_1 + "/providerqueuebylocation", supportedClass = ProviderQueueEntry.class, supportedOpenmrsVersions = { "1.9.* - 9.*" })
public class ProviderQueueResource extends DelegatingCrudResource<ProviderQueueEntry> {
	
	@Override
	public ProviderQueueEntry newDelegate() {
		return new ProviderQueueEntry();
	}
	
	@Override
	public ProviderQueueEntry save(ProviderQueueEntry entry) {
		throw new ResourceDoesNotSupportOperationException("Create not supported - use specific endpoints");
	}
	
	@Override
	public ProviderQueueEntry getByUniqueId(String uniqueId) {
		throw new ResourceDoesNotSupportOperationException("Retrieve by UUID not supported - use GET with parameters");
	}
	
	@Override
	protected void delete(ProviderQueueEntry entry, String s, RequestContext requestContext) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException("Delete not supported");
	}
	
	@Override
	public void purge(ProviderQueueEntry entry, RequestContext requestContext) throws ResponseException {
		throw new ResourceDoesNotSupportOperationException("Purge not supported");
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("queueNumber");
			description.addProperty("ticketNumber");
			description.addProperty("displayName");
			description.addProperty("identifiedBy");
			description.addProperty("currentLocation");
			description.addProperty("serviceLocation");
			description.addProperty("status");
			description.addProperty("waitTime");
			description.addProperty("patient");
			description.addProperty("sourceType");
			description.addProperty("locationToUuid");
			description.addProperty("queueRoomUuid");
			description.addProperty("dateCreated");
			description.addProperty("comment");
			description.addProperty("priority");
			description.addProperty("action");
			return description;
		}
		return null;
	}
	
	/**
	 * Get provider's queue for a location. Returns unified list of both patient and non-patient
	 * queues.
	 */
	@Override
	public PageableResult doGetAll(RequestContext context) {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		String locationUuid = context.getParameter("uuid");
		if (locationUuid == null) {
			throw new IllegalArgumentException("uuid parameter required");
		}
		
		Location location = Context.getLocationService().getLocationByUuid(locationUuid);
		if (location == null) {
			throw new IllegalArgumentException("Location not found");
		}
		
		Date today = new Date();
		Date fromDate = org.openmrs.util.OpenmrsUtil.firstSecondOfDay(today);
		Date toDate = org.openmrs.util.OpenmrsUtil.getLastMomentOfDay(today);
		
		// Get all queues for this location
		List<PatientQueue> patientQueues = service.getPatientQueueList(null, fromDate, toDate, location, null, null, null);
		
		List<NonPatientQueue> nonPatientQueues = service.getNonPatientQueues(null, null, location, null, fromDate, toDate);
		
		// Combine into unified list
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		
		for (PatientQueue pq : patientQueues) {
			// Only show non-completed queues
			if (pq.getStatus() != PatientQueue.Status.COMPLETED) {
				results.add(createProviderQueueEntry(pq));
			}
		}
		
		for (NonPatientQueue npq : nonPatientQueues) {
			// Only show non-completed queues
			if (npq.getStatus() != NonPatientQueue.NonPatientQueueStatus.COMPLETED
			        && npq.getStatus() != NonPatientQueue.NonPatientQueueStatus.CANCELLED) {
				results.add(createProviderQueueEntry(npq));
			}
		}
		
		// Return as paged result for kiosk app compatibility
		return new AlreadyPaged<Map<String, Object>>(context, results, false);
	}
	
	/**
	 * Update queue entry with action (call, start, complete, forward, skip). Handles both patient
	 * and non-patient queues.
	 */
	@Override
	public ProviderQueueEntry update(String uuid, SimpleObject propertiesToUpdate, RequestContext context) {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		String action = (String) propertiesToUpdate.get("action");
		if (action == null) {
			throw new IllegalArgumentException("action parameter required (call, start, complete, forward, skip)");
		}
		
		// Determine if this is a patient or non-patient queue
		PatientQueue patientQueue = null;
		NonPatientQueue nonPatientQueue = null;
		
		try {
			patientQueue = service.getPatientQueueByUuid(uuid);
		}
		catch (Exception e) {
			// Not a patient queue, try non-patient
		}
		
		if (patientQueue == null) {
			try {
				nonPatientQueue = service.getNonPatientQueueByUuid(uuid);
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Queue entry not found");
			}
		}
		
		// Execute action
		Map<String, Object> result;
		if (patientQueue != null) {
			result = executePatientQueueAction(patientQueue, action, service);
		} else {
			result = executeNonPatientQueueAction(nonPatientQueue, action, service);
		}
		
		// Convert result to ProviderQueueEntry
		ProviderQueueEntry entry = new ProviderQueueEntry();
		entry.setUuid((String) result.get("uuid"));
		entry.setQueueNumber((String) result.get("queueNumber"));
		entry.setTicketNumber((String) result.get("ticketNumber"));
		entry.setDisplayName((String) result.get("displayName"));
		entry.setIdentifiedBy((String) result.get("identifiedBy"));
		entry.setCurrentLocation((String) result.get("currentLocation"));
		entry.setServiceLocation((String) result.get("serviceLocation"));
		entry.setStatus((String) result.get("status"));
		entry.setWaitTime((String) result.get("waitTime"));
		entry.setPatient((Boolean) result.get("patient"));
		entry.setSourceType((String) result.get("sourceType"));
		entry.setLocationToUuid((String) result.get("locationToUuid"));
		entry.setQueueRoomUuid((String) result.get("queueRoomUuid"));
		entry.setComment((String) result.get("comment"));
		entry.setPriority((Integer) result.get("priority"));
		entry.setAction((String) result.get("action"));
		
		return entry;
	}
	
	private Map<String, Object> executePatientQueueAction(PatientQueue queue, String action, PatientQueueingService service) {
		
		if ("call".equalsIgnoreCase(action)) {
			// Change PENDING → PICKED
			queue.setStatus(PatientQueue.Status.PICKED);
			queue.setDateChanged(new Date());
		} else if ("start".equalsIgnoreCase(action) || "start_serving".equalsIgnoreCase(action)) {
			// For patient queues, PICKED → COMPLETED (simplified - no SERVING status)
			// But if we want to track serving time, we could use dateChanged
			queue.setStatus(PatientQueue.Status.COMPLETED);
			queue.setDateChanged(new Date());
		} else if ("complete".equalsIgnoreCase(action)) {
			queue.setStatus(PatientQueue.Status.COMPLETED);
			queue.setDateChanged(new Date());
		} else if ("skip".equalsIgnoreCase(action)) {
			// Change back to PENDING (back to queue)
			queue.setStatus(PatientQueue.Status.PENDING);
		} else {
			throw new APIException("Unknown action: " + action);
		}
		
		queue = service.savePatientQue(queue);
		return createProviderQueueEntry(queue);
	}
	
	private Map<String, Object> executeNonPatientQueueAction(NonPatientQueue queue, String action,
	        PatientQueueingService service) {
		
		if ("call".equalsIgnoreCase(action)) {
			queue.setStatus(NonPatientQueue.NonPatientQueueStatus.CALLED);
			queue.setDateChanged(new Date());
		} else if ("start".equalsIgnoreCase(action) || "start_serving".equalsIgnoreCase(action)) {
			queue.setStatus(NonPatientQueue.NonPatientQueueStatus.SERVING);
			queue.setDateChanged(new Date());
		} else if ("complete".equalsIgnoreCase(action)) {
			queue.setStatus(NonPatientQueue.NonPatientQueueStatus.COMPLETED);
			queue.setDateChanged(new Date());
		} else if ("skip".equalsIgnoreCase(action)) {
			// Change back to WAITING (back to queue)
			queue.setStatus(NonPatientQueue.NonPatientQueueStatus.WAITING);
		} else {
			throw new APIException("Unknown action: " + action);
		}
		
		queue = service.saveNonPatientQueue(queue);
		return createProviderQueueEntry(queue);
	}
	
	private Map<String, Object> createProviderQueueEntry(PatientQueue pq) {
		Map<String, Object> entry = new HashMap<String, Object>();
		entry.put("uuid", pq.getUuid());
		entry.put("queueNumber", pq.getVisitNumber());
		entry.put("ticketNumber", pq.getVisitNumber());
		entry.put("displayName", pq.getPatient() != null ? pq.getPatient().getPersonName().getFullName() : "Patient");
		entry.put("identifiedBy", "Patient Record");
		entry.put("currentLocation", pq.getLocationTo() != null ? pq.getLocationTo().getDisplayString() : null);
		entry.put("serviceLocation", pq.getQueueRoom() != null ? pq.getQueueRoom().getDisplayString() : null);
		entry.put("status", normalizeStatusForApp(pq.getStatus().name()));
		entry.put("waitTime", calculateWaitTime(pq.getDateCreated()));
		entry.put("patient", true);
		entry.put("sourceType", "PATIENT");
		entry.put("locationToUuid", pq.getLocationTo() != null ? pq.getLocationTo().getUuid() : null);
		entry.put("queueRoomUuid", pq.getQueueRoom() != null ? pq.getQueueRoom().getUuid() : null);
		entry.put("dateCreated", pq.getDateCreated() != null ? pq.getDateCreated().toString() : null);
		entry.put("comment", null);
		entry.put("priority", null);
		
		return entry;
	}
	
	private Map<String, Object> createProviderQueueEntry(NonPatientQueue npq) {
		Map<String, Object> entry = new HashMap<String, Object>();
		entry.put("uuid", npq.getUuid());
		entry.put("queueNumber", npq.getTicketNumber());
		entry.put("ticketNumber", npq.getTicketNumber());
		entry.put("displayName", npq.getDisplayName());
		entry.put("identifiedBy", "Other");
		entry.put("currentLocation", npq.getLocationTo() != null ? npq.getLocationTo().getDisplayString() : null);
		entry.put("serviceLocation", npq.getQueueRoom() != null ? npq.getQueueRoom().getDisplayString() : null);
		entry.put("status", normalizeStatusForApp(npq.getStatus().name()));
		entry.put("waitTime", calculateWaitTime(npq.getDateCreated()));
		entry.put("patient", false);
		entry.put("sourceType", "NON_PATIENT");
		entry.put("locationToUuid", npq.getLocationTo() != null ? npq.getLocationTo().getUuid() : null);
		entry.put("queueRoomUuid", npq.getQueueRoom() != null ? npq.getQueueRoom().getUuid() : null);
		entry.put("dateCreated", npq.getDateCreated() != null ? npq.getDateCreated().toString() : null);
		entry.put("comment", npq.getComment());
		entry.put("priority", npq.getPriority());
		
		return entry;
	}
	
	private String normalizeStatusForApp(String status) {
		// Normalize status values to match app expectations
		if ("PENDING".equalsIgnoreCase(status) || "WAITING".equalsIgnoreCase(status)) {
			return "WAITING";
		} else if ("PICKED".equalsIgnoreCase(status) || "CALLED".equalsIgnoreCase(status)
		        || "ARRIVED".equalsIgnoreCase(status)) {
			return "CALLED";
		} else if ("SERVING".equalsIgnoreCase(status)) {
			return "IN_SERVICE";
		} else if ("COMPLETED".equalsIgnoreCase(status)) {
			return "COMPLETED";
		}
		return status;
	}
	
	private String calculateWaitTime(Date dateCreated) {
		if (dateCreated == null) {
			return "—";
		}
		
		long diffMs = System.currentTimeMillis() - dateCreated.getTime();
		long diffMin = Math.max(0, diffMs / 60000);
		
		if (diffMin <= 0) {
			return "Just now";
		}
		if (diffMin < 60) {
			return diffMin + " min";
		}
		
		long hours = diffMin / 60;
		long minutes = diffMin % 60;
		return minutes == 0 ? hours + " hr" : hours + " hr " + minutes + " min";
	}
}
