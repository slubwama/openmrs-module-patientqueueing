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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST resource for public queue displays Provides unified view of both patient and non-patient
 * queues Displays "now serving" and "up next" information
 */
@Resource(name = RestConstants.VERSION_1 + "/patientqueueing/display", supportedClass = QueueEntry.class, supportedOpenmrsVersions = { "1.9.* - 9.*" })
public class QueueDisplayResource {
	
	/**
	 * Get queue display data for a specific location Returns now serving, up next, and statistics
	 * 
	 * @param context the request context
	 * @return map containing display data
	 */
	public Map<String, Object> getDisplayData(RequestContext context) {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		String locationUuid = context.getParameter("location");
		String queueRoomUuid = context.getParameter("queueRoom");
		String dateFromParam = context.getParameter("dateFrom");
		String dateToParam = context.getParameter("dateTo");
		
		// Parse date parameters
		Date fromDate;
		Date toDate;
		
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
			if (location == null) {
				throw new IllegalArgumentException("Location not found: " + locationUuid);
			}
		}
		
		if (queueRoomUuid != null && !queueRoomUuid.isEmpty()) {
			queueRoom = Context.getLocationService().getLocationByUuid(queueRoomUuid);
			if (queueRoom == null) {
				throw new IllegalArgumentException("Queue room not found: " + queueRoomUuid);
			}
		}
		
		// Get all queues for the location
		List<QueueEntry> allEntries = new ArrayList<QueueEntry>();
		
		// Get patient queues
		List<PatientQueue> patientQueues = service.getPatientQueueList(null, fromDate, toDate, null, null, null,
		    PatientQueue.Status.PENDING, queueRoom);
		
		for (PatientQueue pq : patientQueues) {
			if (location == null || (location.equals(pq.getLocationTo()) || location.equals(pq.getQueueRoom()))) {
				allEntries.add(new QueueEntry(pq));
			}
		}
		
		// Get non-patient queues
		List<NonPatientQueue> nonPatientQueues = service.getNonPatientQueues(NonPatientQueue.NonPatientQueueStatus.WAITING,
		    null, location, queueRoom, fromDate, toDate);
		
		for (NonPatientQueue npq : nonPatientQueues) {
			allEntries.add(new QueueEntry(npq));
		}
		
		// Get now serving (CALLED, ARRIVED, SERVING status)
		List<QueueEntry> nowServing = new ArrayList<QueueEntry>();
		
		// Patient queues - now serving
		List<PatientQueue> patientServing = service.getPatientQueueList(null, fromDate, toDate, null, null, null,
		    PatientQueue.Status.PICKED, queueRoom);
		for (PatientQueue pq : patientServing) {
			if (location == null || (location.equals(pq.getLocationTo()) || location.equals(pq.getQueueRoom()))) {
				nowServing.add(new QueueEntry(pq));
			}
		}
		
		// Non-patient queues - now serving (CALLED, ARRIVED, SERVING)
		List<NonPatientQueue> nonPatientServing = service.getNonPatientQueues(null, null, location, queueRoom, fromDate,
		    toDate);
		for (NonPatientQueue npq : nonPatientServing) {
			if (npq.getStatus() == NonPatientQueue.NonPatientQueueStatus.CALLED
			        || npq.getStatus() == NonPatientQueue.NonPatientQueueStatus.ARRIVED
			        || npq.getStatus() == NonPatientQueue.NonPatientQueueStatus.SERVING) {
				nowServing.add(new QueueEntry(npq));
			}
		}
		
		// Sort by date created (oldest first for FIFO)
		Collections.sort(allEntries, new Comparator<QueueEntry>() {
			
			@Override
			public int compare(QueueEntry e1, QueueEntry e2) {
				return e1.getDateCreated().compareTo(e2.getDateCreated());
			}
		});
		Collections.sort(nowServing, new Comparator<QueueEntry>() {
			
			@Override
			public int compare(QueueEntry e1, QueueEntry e2) {
				if (e1.getCalledAt() != null && e2.getCalledAt() != null) {
					return e1.getCalledAt().compareTo(e2.getCalledAt());
				}
				return e1.getDateCreated().compareTo(e2.getDateCreated());
			}
		});
		
		// Build response
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("location", location);
		response.put("queueRoom", queueRoom);
		response.put("nowServing", nowServing.isEmpty() ? null : nowServing.get(0));
		response.put("upNext", allEntries.isEmpty() ? null : allEntries.subList(0, Math.min(5, allEntries.size())));
		response.put("statistics", buildStatistics(service, location, queueRoom, fromDate, toDate));
		response.put("lastUpdated", new Date());
		
		return response;
	}
	
	/**
	 * Get queue statistics for a location
	 * 
	 * @param context the request context
	 * @return statistics map
	 */
	public Map<String, Object> getStatistics(RequestContext context) {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		String locationUuid = context.getParameter("location");
		String queueRoomUuid = context.getParameter("queueRoom");
		String dateFromParam = context.getParameter("dateFrom");
		String dateToParam = context.getParameter("dateTo");
		
		// Parse date parameters
		Date fromDate;
		Date toDate;
		
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
			if (location == null) {
				throw new IllegalArgumentException("Location not found: " + locationUuid);
			}
		}
		
		if (queueRoomUuid != null && !queueRoomUuid.isEmpty()) {
			queueRoom = Context.getLocationService().getLocationByUuid(queueRoomUuid);
			if (queueRoom == null) {
				throw new IllegalArgumentException("Queue room not found: " + queueRoomUuid);
			}
		}
		
		return buildStatistics(service, location, queueRoom, fromDate, toDate);
	}
	
	/**
	 * Build statistics map for a location
	 */
	private Map<String, Object> buildStatistics(PatientQueueingService service, Location location, Location queueRoom,
	        Date fromDate, Date toDate) {
		Map<String, Object> stats = new HashMap<String, Object>();
		
		// Patient queue statistics
		int patientWaiting = 0;
		int patientServing = 0;
		int patientCompleted = 0;
		
		List<PatientQueue> patientQueues = service.getPatientQueueList(null, fromDate, toDate, null, null, null, null,
		    queueRoom);
		for (PatientQueue pq : patientQueues) {
			if (location == null || (location.equals(pq.getLocationTo()) || location.equals(pq.getQueueRoom()))) {
				if (pq.getStatus() == PatientQueue.Status.PENDING) {
					patientWaiting++;
				} else if (pq.getStatus() == PatientQueue.Status.PICKED) {
					patientServing++;
				} else if (pq.getStatus() == PatientQueue.Status.COMPLETED) {
					patientCompleted++;
				}
			}
		}
		
		// Non-patient queue statistics
		int nonPatientWaiting = 0;
		int nonPatientServing = 0;
		int nonPatientCompleted = 0;
		
		List<NonPatientQueue> nonPatientQueues = service.getNonPatientQueues(null, null, location, queueRoom, fromDate,
		    toDate);
		for (NonPatientQueue npq : nonPatientQueues) {
			if (npq.getStatus() == NonPatientQueue.NonPatientQueueStatus.WAITING) {
				nonPatientWaiting++;
			} else if (npq.getStatus() == NonPatientQueue.NonPatientQueueStatus.CALLED
			        || npq.getStatus() == NonPatientQueue.NonPatientQueueStatus.ARRIVED
			        || npq.getStatus() == NonPatientQueue.NonPatientQueueStatus.SERVING) {
				nonPatientServing++;
			} else if (npq.getStatus() == NonPatientQueue.NonPatientQueueStatus.COMPLETED) {
				nonPatientCompleted++;
			}
		}
		
		stats.put("patientWaiting", patientWaiting);
		stats.put("patientServing", patientServing);
		stats.put("patientCompleted", patientCompleted);
		stats.put("nonPatientWaiting", nonPatientWaiting);
		stats.put("nonPatientServing", nonPatientServing);
		stats.put("nonPatientCompleted", nonPatientCompleted);
		stats.put("totalWaiting", patientWaiting + nonPatientWaiting);
		stats.put("totalServing", patientServing + nonPatientServing);
		stats.put("totalCompleted", patientCompleted + nonPatientCompleted);
		
		return stats;
	}
}
