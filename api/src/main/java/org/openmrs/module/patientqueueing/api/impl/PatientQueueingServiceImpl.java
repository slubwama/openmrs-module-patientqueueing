/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientqueueing.api.impl;

import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.PatientService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.api.dao.PatientQueueingDao;
import org.openmrs.module.patientqueueing.model.NonPatientQueue;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.util.OpenmrsUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.openmrs.module.patientqueueing.PatientQueueingConfig.ROOM_TAG_UUID;

public class PatientQueueingServiceImpl extends BaseOpenmrsService implements PatientQueueingService {
	
	PatientQueueingDao dao;
	
	private static final int VISIT_NUMBER_INTEGER_START_POSITION = 15;
	
	private static final int INTEGER_IN_VISIT_NUMBER_LENGTH = 3;
	
	public void setDao(PatientQueueingDao dao) {
		this.dao = dao;
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#savePatientQue(org.openmrs.module.patientqueueing.model.PatientQueue)
	 */
	public PatientQueue savePatientQue(PatientQueue patientQueue) {
		PatientQueue currentQueue = dao.getIncompletePatientQueue(patientQueue.getPatient(), patientQueue.getLocationTo(),
		    null);
		
		if (currentQueue != null && !patientQueue.equals(currentQueue)) {
			completePatientQueue(currentQueue);
		}
		
		return dao.savePatientQueue(patientQueue);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueById(java.lang.Integer)
	 */
	public PatientQueue getPatientQueueById(Integer queueId) {
		return dao.getPatientQueueById(queueId);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueList(org.openmrs.Provider,
	 *      java.util.Date, java.util.Date, org.openmrs.Location, org.openmrs.Location,
	 *      org.openmrs.Patient, org.openmrs.module.patientqueueing.model.PatientQueue.Status)
	 */
	public List<PatientQueue> getPatientQueueList(Provider provider, Date fromDate, Date toDate, Location locationTo,
	        Location locationFrom, Patient patient, PatientQueue.Status status) {
		return dao.getPatientQueueList(provider, fromDate, toDate, locationTo, locationFrom, patient, status, null);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#completePatientQueue(org.openmrs.module.patientqueueing.model.PatientQueue)
	 */
	@Override
	public PatientQueue completePatientQueue(PatientQueue patientQueue) {
		patientQueue.setStatus(PatientQueue.Status.COMPLETED);
		patientQueue.setDateCompleted(new Date());
		return dao.savePatientQueue(patientQueue);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getIncompletePatientQueue(org.openmrs.Patient,
	 *      org.openmrs.Location)
	 */
	public PatientQueue getIncompletePatientQueue(Patient patient, Location locationTo) {
		
		return dao.getIncompletePatientQueue(patient, locationTo, null);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getMostRecentQueue(org.openmrs.Patient)
	 */
	@Override
	public PatientQueue getMostRecentQueue(Patient patient) {
		return dao.getMostRecentQueue(patient);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#assignVisitNumberForToday(org.openmrs.module.patientqueueing.model.PatientQueue)
	 */
	@Override
	public PatientQueue assignVisitNumberForToday(PatientQueue patientQueue) {
		Date today = new Date();
		List<PatientQueue> patientQueueList = getPatientQueueList(null, OpenmrsUtil.firstSecondOfDay(today),
		    OpenmrsUtil.getLastMomentOfDay(today), null, null, patientQueue.getPatient(), null);
		
		if (!patientQueueList.isEmpty() && patientQueueList.get(0).getVisitNumber() != null) {
			patientQueue.setVisitNumber(patientQueueList.get(0).getVisitNumber());
		} else {
			patientQueue.setVisitNumber(generateVisitNumber(patientQueue.getLocationFrom(), patientQueue.getPatient()));
		}
		return patientQueue;
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#generateVisitNumber(org.openmrs.Location,
	 *      org.openmrs.Patient)
	 */
	public String generateVisitNumber(Location location, Patient patient) {
		
		Date today = new Date();
		
		SimpleDateFormat formatterExt = new SimpleDateFormat("dd/MM/yyyy");
		
		List<PatientQueue> patientQueues = getPatientQueueList(null, OpenmrsUtil.firstSecondOfDay(today),
		    OpenmrsUtil.getLastMomentOfDay(today), null, location, null, null);
		
		int nextNumberInQueue = 1;
		if (!patientQueues.isEmpty()) {
			
			int visitNumberLength = patientQueues.get(0).getVisitNumber().length();
			
			if (visitNumberLength == VISIT_NUMBER_INTEGER_START_POSITION + INTEGER_IN_VISIT_NUMBER_LENGTH) {
				nextNumberInQueue = Integer.parseInt(patientQueues.get(0).getVisitNumber()
				        .subSequence(VISIT_NUMBER_INTEGER_START_POSITION, visitNumberLength).toString());
				nextNumberInQueue += 1;
			}
		}
		
		String dateString = formatterExt.format(today);
		
		String locationName = location.getName();
		
		if (locationName.length() > 3) {
			locationName = locationName.substring(0, 3);
		}
		
		String zeroesToAppend = "";
		if (nextNumberInQueue <= 9) {
			zeroesToAppend = "00";
		} else if (nextNumberInQueue < 100) {
			zeroesToAppend = "0";
		}
		
		return dateString + "-" + locationName + "-" + zeroesToAppend + nextNumberInQueue;
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueListBySearchParams(java.lang.String,
	 *      java.util.Date, java.util.Date, org.openmrs.Location, org.openmrs.Location,
	 *      org.openmrs.module.patientqueueing.model.PatientQueue.Status)
	 */
	@Override
	public List<PatientQueue> getPatientQueueListBySearchParams(String searchString, Date fromDate, Date toDate,
	        Location locationTo, Location locationFrom, PatientQueue.Status status) {
		
		List<Patient> patientList = new ArrayList<Patient>();
		
		if (searchString != null && !searchString.equals("")) {
			PatientService patientService = Context.getPatientService();
			List list = Arrays.asList(searchString.split(","));
			for (Object o : list) {
				List<Patient> patients = patientService.getPatients(o.toString());
				patientList.addAll(patients);
			}
		}
		
		return dao.getPatientQueueList(patientList, fromDate, toDate, locationTo, locationFrom, status, null);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueList(org.openmrs.Provider,
	 *      java.util.Date, java.util.Date, org.openmrs.Location, org.openmrs.Location,
	 *      org.openmrs.Patient,
	 *      org.openmrs.module.patientqueueing.model.PatientQueue.Status,org.openmrs.Location)
	 */
	public List<PatientQueue> getPatientQueueList(Provider provider, Date fromDate, Date toDate, Location locationTo,
	        Location locationFrom, Patient patient, PatientQueue.Status status, Location queueRoom) {
		return dao.getPatientQueueList(provider, fromDate, toDate, locationTo, locationFrom, patient, status, queueRoom);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getIncompletePatientQueue(org.openmrs.Patient,
	 *      org.openmrs.Location,org.openmrs.Location )
	 */
	public PatientQueue getIncompletePatientQueue(Patient patient, Location locationTo, Location queueRoom) {
		
		return dao.getIncompletePatientQueue(patient, locationTo, queueRoom);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueListBySearchParams(java.lang.String,
	 *      java.util.Date, java.util.Date, org.openmrs.Location, org.openmrs.Location,
	 *      org.openmrs.module.patientqueueing.model.PatientQueue.Status,org.openmrs.Location)
	 */
	@Override
	public List<PatientQueue> getPatientQueueListBySearchParams(String searchString, Date fromDate, Date toDate,
	        Location locationTo, Location locationFrom, PatientQueue.Status status, Location queueRoom) {
		
		List<Patient> patientList = new ArrayList<Patient>();
		
		if (searchString != null && !searchString.equals("")) {
			PatientService patientService = Context.getPatientService();
			List list = Arrays.asList(searchString.split(","));
			for (Object o : list) {
				List<Patient> patients = patientService.getPatients(o.toString());
				patientList.addAll(patients);
			}
		}
		
		return dao.getPatientQueueList(patientList, fromDate, toDate, locationTo, locationFrom, status, queueRoom);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueByUuid(java.lang.String)
	 */
	@Override
	public PatientQueue getPatientQueueByUuid(String uuid) {
		return dao.getPatientQueueByUUID(uuid);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#pickPatientQueue(org.openmrs.module.patientqueueing.model.PatientQueue,org.openmrs.Provider,org.openmrs.Location)
	 */
	@Override
	public PatientQueue pickPatientQueue(PatientQueue patientQueue, Provider provider, Location queueRoom) {
		patientQueue.setStatus(PatientQueue.Status.PICKED);
		patientQueue.setDatePicked(new Date());
		patientQueue.setProvider(provider);
		patientQueue.setQueueRoom(queueRoom);
		return dao.savePatientQueue(patientQueue);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#
	 *      getPatientQueueByParentLocation(org.openmrs.Location,
	 *      org.openmrs.module.patientqueueing.model.PatientQueue.Status, java.util.Date dateFrom,
	 *      java.util.Date)
	 */
	@Override
    public List<PatientQueue> getPatientQueueByParentLocation(Location parentLocation, PatientQueue.Status status, Date fromDate, Date toDate, boolean onlyInQueueRooms) {
        LocationTag queueRomTag = Context.getLocationService().getLocationTagByUuid(ROOM_TAG_UUID);
        List<Location> childLocations = new ArrayList<>();
        flattenLocationHierarchy(parentLocation, childLocations, queueRomTag, onlyInQueueRooms);

        if (childLocations.isEmpty()) {
            return null;
        }
        return dao.getPatientsInQueueRoom(childLocations, status, fromDate, toDate);
    }
	
	/**
	 * Supportive class that helps to loop through locations recursively to ensure that all child
	 * locations are collected
	 * 
	 * @param parentLocation the parent location to check for children
	 * @param childLocations the childLocation List to be updated
	 * @param locationTag the tag to check if location has it
	 * @param onlyInQueueRooms condition to determine if to only include locations with locationTag
	 */
	private void flattenLocationHierarchy(Location parentLocation, List<Location> childLocations, LocationTag locationTag,
	        boolean onlyInQueueRooms) {
		if (onlyInQueueRooms) {
			if (parentLocation.getTags().contains(locationTag)) {
				childLocations.add(parentLocation);
			}
		} else {
			childLocations.add(parentLocation);
		}
		
		for (Location childLocation : parentLocation.getChildLocations(false)) {
			flattenLocationHierarchy(childLocation, childLocations, locationTag, onlyInQueueRooms);
		}
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueByVisitNumber(java.lang.String,
	 *      java.util.Date, java.util.Date)
	 */
	public List<PatientQueue> getPatientQueueByVisitNumber(String visitNumber, Date fromDate, Date toDate) {
		return dao.getPatientQueueByVisitNumber(visitNumber, fromDate, toDate);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueListFifo(org.openmrs.Provider,
	 *      java.util.Date,
	 *      java.util.Date,org.openmrs.Location,org.openmrs.Location,org.openmrs.Patient,org.openmrs.module.patientqueueing.model.PatientQueue.Status,org.openmrs.Location)
	 */
	public List<PatientQueue> getPatientQueueListFifo(Provider provider, Date fromDate, Date toDate, Location locationTo,
	        Location locationFrom, Patient patient, PatientQueue.Status status, Location queueRoom) {
		return dao.getPatientQueueListFifo(provider, fromDate, toDate, locationTo, locationFrom, patient, status, queueRoom);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueByParentLocationFifo(org.openmrs.Location,org.openmrs.module.patientqueueing.model.PatientQueue.Status,java.util.Date,
	 *      java.util.Date,boolean)
	 */
	public List<PatientQueue> getPatientQueueByParentLocationFifo(Location parentLocation, PatientQueue.Status status,
																  Date fromDate, Date toDate, boolean onlyInQueueRooms) {
		LocationTag queueRomTag = Context.getLocationService().getLocationTagByUuid(ROOM_TAG_UUID);
		List<Location> childLocations = new ArrayList<>();
		flattenLocationHierarchy(parentLocation, childLocations, queueRomTag, onlyInQueueRooms);

		if (childLocations.isEmpty()) {
			return null;
		}
		return dao.getPatientsInQueueRoomFifo(childLocations, status, fromDate, toDate);
	}
	
	// ========== Non-Patient Queue Service Methods ==========
	
	@Override
	public NonPatientQueue getNonPatientQueueById(Integer queueId) {
		return dao.getNonPatientQueueById(queueId);
	}
	
	@Override
	public NonPatientQueue getNonPatientQueueByUuid(String uuid) {
		return dao.getNonPatientQueueByUuid(uuid);
	}
	
	@Override
	public NonPatientQueue createNonPatientQueueEntry(String displayName, String phoneNumber, Concept queueType,
	        Location currentLocation, Location locationTo, Location queueRoom, Integer priority, String comment) {
		NonPatientQueue queue = new NonPatientQueue();
		queue.setDisplayName(displayName);
		queue.setPhoneNumber(phoneNumber);
		queue.setQueueType(queueType);
		queue.setCurrentLocation(currentLocation);
		queue.setLocationTo(locationTo);
		queue.setQueueRoom(queueRoom);
		queue.setPriority(priority);
		queue.setComment(comment);
		queue.setStatus(NonPatientQueue.NonPatientQueueStatus.WAITING);
		
		String ticketNumber = generateNonPatientQueueTicketNumber(currentLocation, queueType);
		queue.setTicketNumber(ticketNumber);
		
		return saveNonPatientQueue(queue);
	}
	
	@Override
	public NonPatientQueue saveNonPatientQueue(NonPatientQueue nonPatientQueue) {
		return dao.saveNonPatientQueue(nonPatientQueue);
	}
	
	@Override
	public List<NonPatientQueue> getNonPatientQueueByTicketNumber(String ticketNumber, Date fromDate, Date toDate) {
		return dao.getNonPatientQueueByTicketNumber(ticketNumber, fromDate, toDate);
	}
	
	@Override
	public List<NonPatientQueue> getNonPatientQueuesByQueueRoom(Location queueRoom, Date fromDate, Date toDate) {
		return dao.getNonPatientQueuesByQueueRoom(queueRoom, fromDate, toDate);
	}
	
	@Override
	public List<NonPatientQueue> getNonPatientQueuesByQueueRoomAndStatus(Location queueRoom,
	        NonPatientQueue.NonPatientQueueStatus status) {
		return dao.getNonPatientQueuesByQueueRoomAndStatus(queueRoom, status);
	}
	
	@Override
	public List<NonPatientQueue> getAllActiveNonPatientQueues() {
		return dao.getAllActiveNonPatientQueues();
	}
	
	@Override
	public List<NonPatientQueue> getNonPatientQueues(NonPatientQueue.NonPatientQueueStatus status, Concept queueType,
	        Location locationTo, Location queueRoom, Date fromDate, Date toDate) {
		return dao.getNonPatientQueues(status, queueType, locationTo, queueRoom, fromDate, toDate);
	}
	
	@Override
	public NonPatientQueue callNonPatientQueue(NonPatientQueue queue, Provider provider) {
		if (queue.getStatus() != NonPatientQueue.NonPatientQueueStatus.WAITING) {
			throw new APIException("Can only call queue entries with status WAITING");
		}
		
		queue.setStatus(NonPatientQueue.NonPatientQueueStatus.CALLED);
		queue.setCalledBy(provider);
		queue.setCalledAt(new Date());
		
		return saveNonPatientQueue(queue);
	}
	
	@Override
	public NonPatientQueue markNonPatientQueueArrived(NonPatientQueue queue) {
		if (queue.getStatus() != NonPatientQueue.NonPatientQueueStatus.CALLED) {
			throw new APIException("Can only mark as arrived queue entries with status CALLED");
		}
		
		queue.setStatus(NonPatientQueue.NonPatientQueueStatus.ARRIVED);
		queue.setArrivedAt(new Date());
		
		return saveNonPatientQueue(queue);
	}
	
	@Override
	public NonPatientQueue startServingNonPatientQueue(NonPatientQueue queue, Provider provider) {
		if (queue.getStatus() != NonPatientQueue.NonPatientQueueStatus.ARRIVED) {
			throw new APIException("Can only start serving queue entries with status ARRIVED");
		}
		
		queue.setStatus(NonPatientQueue.NonPatientQueueStatus.SERVING);
		queue.setServedBy(provider);
		queue.setStartedAt(new Date());
		
		return saveNonPatientQueue(queue);
	}
	
	@Override
	public NonPatientQueue completeNonPatientQueue(NonPatientQueue queue, Provider provider) {
		if (queue.getStatus() != NonPatientQueue.NonPatientQueueStatus.SERVING) {
			throw new APIException("Can only complete queue entries with status SERVING");
		}
		
		queue.setStatus(NonPatientQueue.NonPatientQueueStatus.COMPLETED);
		if (queue.getServedBy() == null) {
			queue.setServedBy(provider);
		}
		queue.setEndedAt(new Date());
		
		return saveNonPatientQueue(queue);
	}
	
	@Override
	public NonPatientQueue skipNonPatientQueue(NonPatientQueue queue) {
		if (queue.getStatus() != NonPatientQueue.NonPatientQueueStatus.CALLED) {
			throw new APIException("Can only skip queue entries with status CALLED");
		}
		
		queue.setStatus(NonPatientQueue.NonPatientQueueStatus.WAITING);
		queue.setCalledAt(null);
		queue.setCalledBy(null);
		
		return saveNonPatientQueue(queue);
	}
	
	@Override
	public NonPatientQueue cancelNonPatientQueue(NonPatientQueue queue) {
		if (queue.getStatus() == NonPatientQueue.NonPatientQueueStatus.COMPLETED
		        || queue.getStatus() == NonPatientQueue.NonPatientQueueStatus.CANCELLED) {
			throw new APIException("Cannot cancel queue entries that are completed or already cancelled");
		}
		
		queue.setStatus(NonPatientQueue.NonPatientQueueStatus.CANCELLED);
		queue.setEndedAt(new Date());
		
		return saveNonPatientQueue(queue);
	}
	
	@Override
	public String generateNonPatientQueueTicketNumber(Location location, Concept queueType) {
		Date today = new Date();
		SimpleDateFormat formatterExt = new SimpleDateFormat("dd/MM/yyyy");
		
		List<NonPatientQueue> existingQueues = getNonPatientQueues(null, queueType, null, null,
		    OpenmrsUtil.firstSecondOfDay(today), OpenmrsUtil.getLastMomentOfDay(today));
		
		int nextNumberInQueue = 1;
		if (!existingQueues.isEmpty()) {
			// Get the most recently created queue (last in the list since it's sorted by dateCreated ASC)
			NonPatientQueue lastQueue = existingQueues.get(existingQueues.size() - 1);
			String lastTicketNumber = lastQueue.getTicketNumber();
			if (lastTicketNumber != null && lastTicketNumber.length() >= 15 + 3) {
				try {
					nextNumberInQueue = Integer.parseInt(lastTicketNumber.substring(15));
					nextNumberInQueue += 1;
				}
				catch (NumberFormatException e) {
					// If parsing fails, start from 1
					nextNumberInQueue = 1;
				}
			}
		}
		
		String dateString = formatterExt.format(today);
		
		String locationName = location != null ? location.getName() : "LOC";
		if (locationName != null && locationName.length() > 3) {
			locationName = locationName.substring(0, 3);
		}
		
		String queueTypeCode = queueType != null && queueType.getUuid() != null ? queueType.getUuid().substring(0, 3)
		        .toUpperCase() : "XXX";
		
		String zeroesToAppend = "";
		if (nextNumberInQueue <= 9) {
			zeroesToAppend = "00";
		} else if (nextNumberInQueue < 100) {
			zeroesToAppend = "0";
		}
		
		return dateString + "-" + locationName + "-" + queueTypeCode + "-" + zeroesToAppend + nextNumberInQueue;
	}
}
