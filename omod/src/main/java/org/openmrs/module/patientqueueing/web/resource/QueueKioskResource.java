package org.openmrs.module.patientqueueing.web.resource;

import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.customdto.QueueEntry;
import org.openmrs.module.patientqueueing.model.NonPatientQueue;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.util.OpenmrsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * REST resource for queue kiosk displays Provides unified access to both patient and non-patient
 * queues Supports ticket lookup by ticket number
 */
@Resource(name = RestConstants.VERSION_1 + "/patientqueueing/kiosk", supportedClass = QueueEntry.class, supportedOpenmrsVersions = { "1.9.* - 9.*" })
public class QueueKioskResource {
	
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
}
