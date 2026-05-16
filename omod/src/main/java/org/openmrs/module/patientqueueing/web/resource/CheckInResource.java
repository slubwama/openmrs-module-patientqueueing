package org.openmrs.module.patientqueueing.web.resource;

import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientqueueing.PatientQueueingConfig;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.web.customdto.CheckInResult;
import org.openmrs.module.patientqueueing.web.customdto.QueueEntry;
import org.openmrs.module.patientqueueing.model.NonPatientQueue;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * REST resource for self check-in Handles both patient and non-patient check-ins
 */
@Resource(name = RestConstants.VERSION_1 + "/patientqueueing/checkin", supportedClass = CheckInResult.class, supportedOpenmrsVersions = { "1.9.* - 9.*" })
public class CheckInResource {
	
	private static final Logger log = LoggerFactory.getLogger(CheckInResource.class);
	
	/**
	 * Perform self check-in for a patient
	 * 
	 * @param context the request context
	 * @return CheckInResult with visit, queue entry, and ticket information
	 */
	public CheckInResult checkInPatient(RequestContext context) {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		// Get parameters
		String patientUuid = context.getParameter("patient");
		String locationUuid = context.getParameter("location");
		String queueRoomUuid = context.getParameter("queueRoom");
		
		// Validate required parameters
		if (patientUuid == null || patientUuid.isEmpty()) {
			return new CheckInResult("patient parameter is required");
		}
		
		if (locationUuid == null || locationUuid.isEmpty()) {
			return new CheckInResult("location parameter is required");
		}
		
		// Get patient
		Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
		if (patient == null) {
			return new CheckInResult("Patient not found");
		}
		
		// Get location
		Location location = Context.getLocationService().getLocationByUuid(locationUuid);
		if (location == null) {
			return new CheckInResult("Location not found");
		}
		
		// Get queue room (optional)
		Location queueRoom = null;
		if (queueRoomUuid != null && !queueRoomUuid.isEmpty()) {
			queueRoom = Context.getLocationService().getLocationByUuid(queueRoomUuid);
			if (queueRoom == null) {
				return new CheckInResult("Queue room not found");
			}
		}
		
		try {
			// Ensure patient has a visit for today (create or reuse existing)
			Visit visit = ensurePatientVisitForToday(patient, location);
			if (visit == null) {
				log.warn("Could not create or find visit for patient " + patient.getPatientId()
				        + ". Proceeding with queue entry only.");
			}
			
			// Create patient queue entry
			PatientQueue patientQueue = new PatientQueue();
			patientQueue.setPatient(patient);
			patientQueue.setLocationTo(location);
			patientQueue.setQueueRoom(queueRoom);
			patientQueue.setStatus(PatientQueue.Status.PENDING);
			patientQueue.setDateCreated(new Date());
			
			// Generate visit number (ticket number)
			String visitNumber = service.generateVisitNumber(location, patient);
			patientQueue.setVisitNumber(visitNumber);
			
			// Save the queue entry
			patientQueue = service.savePatientQue(patientQueue);
			
			// Calculate queue position
			int queuePosition = calculatePatientQueuePosition(service, location, queueRoom);
			
			// Calculate estimated wait time (configured minutes per person ahead)
			int estimatedWaitMinutes = queuePosition * getEstimatedWaitMinutesPerPerson();
			
			// Create result
			CheckInResult result = new CheckInResult();
			result.setPatient(patient);
			result.setQueueEntry(new QueueEntry(patientQueue));
			result.setTicketNumber(patientQueue.getVisitNumber());
			result.setQueuePosition(queuePosition);
			result.setEstimatedWaitMinutes(estimatedWaitMinutes);
			result.setSuccess(true);
			
			return result;
		}
		catch (APIException e) {
			return new CheckInResult("Check-in failed: " + e.getMessage());
		}
		catch (Exception e) {
			return new CheckInResult("Check-in failed: " + e.getMessage());
		}
	}
	
	/**
	 * Perform self check-in for a non-patient
	 * 
	 * @param context the request context
	 * @return CheckInResult with queue entry and ticket information
	 */
	public CheckInResult checkInNonPatient(RequestContext context) {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		// Get parameters
		String displayName = context.getParameter("displayName");
		String phoneNumber = context.getParameter("phoneNumber");
		String queueTypeUuid = context.getParameter("queueType");
		String locationUuid = context.getParameter("location");
		String queueRoomUuid = context.getParameter("queueRoom");
		
		// Validate required parameters
		if (displayName == null || displayName.isEmpty()) {
			return new CheckInResult("displayName parameter is required");
		}
		
		if (locationUuid == null || locationUuid.isEmpty()) {
			return new CheckInResult("location parameter is required");
		}
		
		// Get location
		Location location = Context.getLocationService().getLocationByUuid(locationUuid);
		if (location == null) {
			return new CheckInResult("Location not found");
		}
		
		// Get queue room (optional)
		Location queueRoom = null;
		if (queueRoomUuid != null && !queueRoomUuid.isEmpty()) {
			queueRoom = Context.getLocationService().getLocationByUuid(queueRoomUuid);
			if (queueRoom == null) {
				return new CheckInResult("Queue room not found");
			}
		}
		
		// Get queue type (optional)
		Concept queueType = null;
		if (queueTypeUuid != null && !queueTypeUuid.isEmpty()) {
			queueType = Context.getConceptService().getConceptByUuid(queueTypeUuid);
			if (queueType == null) {
				return new CheckInResult("Queue type concept not found");
			}
		}
		
		try {
			// Generate ticket number
			String ticketNumber = service.generateNonPatientQueueTicketNumber(location, queueType);
			
			// Create non-patient queue entry
			NonPatientQueue nonPatientQueue = new NonPatientQueue();
			nonPatientQueue.setUuid(UUID.randomUUID().toString());
			nonPatientQueue.setDisplayName(displayName);
			nonPatientQueue.setPhoneNumber(phoneNumber);
			nonPatientQueue.setQueueType(queueType);
			nonPatientQueue.setCurrentLocation(location);
			nonPatientQueue.setLocationTo(location);
			nonPatientQueue.setQueueRoom(queueRoom);
			nonPatientQueue.setStatus(NonPatientQueue.NonPatientQueueStatus.WAITING);
			nonPatientQueue.setTicketNumber(ticketNumber);
			nonPatientQueue.setDateCreated(new Date());
			
			// Save the queue entry
			nonPatientQueue = service.saveNonPatientQueue(nonPatientQueue);
			
			// Calculate queue position
			int queuePosition = calculateNonPatientQueuePosition(service, location, queueRoom);
			
			// Calculate estimated wait time (configured minutes per person ahead)
			int estimatedWaitMinutes = queuePosition * getEstimatedWaitMinutesPerPerson();
			
			// Create result
			CheckInResult result = new CheckInResult();
			result.setQueueEntry(new QueueEntry(nonPatientQueue));
			result.setTicketNumber(nonPatientQueue.getTicketNumber());
			result.setQueuePosition(queuePosition);
			result.setEstimatedWaitMinutes(estimatedWaitMinutes);
			result.setSuccess(true);
			
			return result;
		}
		catch (APIException e) {
			return new CheckInResult("Check-in failed: " + e.getMessage());
		}
		catch (Exception e) {
			return new CheckInResult("Check-in failed: " + e.getMessage());
		}
	}
	
	/**
	 * Calculate queue position for patient
	 */
	private int calculatePatientQueuePosition(PatientQueueingService service, Location location, Location queueRoom) {
		Date today = new Date();
		Date fromDate = OpenmrsUtil.firstSecondOfDay(today);
		Date toDate = OpenmrsUtil.getLastMomentOfDay(today);
		
		List<PatientQueue> queues = service.getPatientQueueList(null, fromDate, toDate, null, null, null,
		    PatientQueue.Status.PENDING, queueRoom);
		
		int position = 0;
		for (PatientQueue q : queues) {
			if (location.equals(q.getLocationTo()) || location.equals(q.getQueueRoom())) {
				position++;
			}
		}
		
		return position + 1;
	}
	
	/**
	 * Calculate queue position for non-patient
	 */
	private int calculateNonPatientQueuePosition(PatientQueueingService service, Location location, Location queueRoom) {
		Date today = new Date();
		Date fromDate = OpenmrsUtil.firstSecondOfDay(today);
		Date toDate = OpenmrsUtil.getLastMomentOfDay(today);
		
		List<NonPatientQueue> queues = service.getNonPatientQueues(NonPatientQueue.NonPatientQueueStatus.WAITING, null,
		    location, queueRoom, fromDate, toDate);
		
		return queues.size();
	}
	
	/**
	 * Get the estimated wait minutes per person from global property.
	 * 
	 * @return the configured minutes per person, default 5 if not configured or invalid
	 */
	private int getEstimatedWaitMinutesPerPerson() {
		try {
			AdministrationService adminService = Context.getAdministrationService();
			String value = adminService.getGlobalProperty(PatientQueueingConfig.GP_ESTIMATED_WAIT_MINUTES_PER_PERSON,
			    String.valueOf(PatientQueueingConfig.DEFAULT_ESTIMATED_WAIT_MINUTES_PER_PERSON));
			int minutes = Integer.parseInt(value);
			if (minutes <= 0) {
				log.warn("Invalid estimated wait minutes per person: {}, using default: {}", minutes,
				    PatientQueueingConfig.DEFAULT_ESTIMATED_WAIT_MINUTES_PER_PERSON);
				return PatientQueueingConfig.DEFAULT_ESTIMATED_WAIT_MINUTES_PER_PERSON;
			}
			return minutes;
		}
		catch (NumberFormatException e) {
			log.warn("Invalid estimated wait minutes per person format, using default: {}",
			    PatientQueueingConfig.DEFAULT_ESTIMATED_WAIT_MINUTES_PER_PERSON, e);
			return PatientQueueingConfig.DEFAULT_ESTIMATED_WAIT_MINUTES_PER_PERSON;
		}
		catch (Exception e) {
			log.warn("Error reading estimated wait minutes per person, using default: {}",
			    PatientQueueingConfig.DEFAULT_ESTIMATED_WAIT_MINUTES_PER_PERSON, e);
			return PatientQueueingConfig.DEFAULT_ESTIMATED_WAIT_MINUTES_PER_PERSON;
		}
	}
	
	/**
	 * Ensure patient has a visit for check-in. If an active visit exists, reuse it. Otherwise,
	 * create a new visit starting at the first second of the day.
	 * 
	 * @param patient the patient to check in
	 * @param location the location for the visit
	 * @return the existing or newly created visit, or null if visit type is not configured
	 */
	private Visit ensurePatientVisitForToday(Patient patient, Location location) {
		try {
			AdministrationService administrationService = Context.getAdministrationService();
			String visitTypeUuid = administrationService.getGlobalProperty(
			    PatientQueueingConfig.GP_SELF_CHECK_IN_VISIT_TYPE_UUID, "");
			
			// If no visit type is configured, skip visit creation
			if (visitTypeUuid == null || visitTypeUuid.trim().isEmpty()) {
				log.info("No visit type configured for check-in, skipping visit creation");
				return null;
			}
			
			VisitService visitService = Context.getVisitService();
			
			// Check for active visits - reuse the first active visit (regardless of when it started)
			List<Visit> activeVisits = visitService.getActiveVisitsByPatient(patient);
			if (activeVisits != null && !activeVisits.isEmpty()) {
				Visit activeVisit = activeVisits.get(0);
				log.info("Reusing existing active visit " + activeVisit.getUuid() + " for patient " + patient.getPatientId());
				return activeVisit;
			}
			
			// No active visit exists, create a new one
			VisitType visitType = visitService.getVisitTypeByUuid(visitTypeUuid);
			if (visitType == null) {
				log.error("Configured visit type not found: " + visitTypeUuid);
				return null;
			}
			
			Visit visit = new Visit();
			visit.setPatient(patient);
			visit.setVisitType(visitType);
			// Set visit start to beginning of day (00:00:00.000)
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			visit.setStartDatetime(calendar.getTime());
			visit.setLocation(location);
			
			Visit savedVisit = visitService.saveVisit(visit);
			log.info("Created new visit " + savedVisit.getUuid() + " of type " + visitType.getName() + " for patient "
			        + patient.getPatientId());
			return savedVisit;
		}
		catch (Exception e) {
			log.error("Error ensuring patient visit for today: " + e.getMessage(), e);
			return null;
		}
	}
}
