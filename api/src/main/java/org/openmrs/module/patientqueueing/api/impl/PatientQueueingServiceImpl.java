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

import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.api.PatientService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.api.dao.PatientQueueingDao;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.module.patientqueueing.model.PatientQueueEvent;

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
	 * @see PatientQueueingService#savePatientQue(PatientQueue)
	 */
	public PatientQueue savePatientQue(PatientQueue patientQueue) {
		// Ensure key derived fields are populated for downstream displays and kiosks
		hydrateDerivedFields(patientQueue);
		
		PatientQueue currentQueue = dao.getIncompletePatientQueue(patientQueue.getPatient(), patientQueue.getLocationTo(),
		// Complete only within the same service location (room) if provided
		    patientQueue.getQueueRoom());
		
		if (currentQueue != null && !patientQueue.equals(currentQueue)) {
			completePatientQueue(currentQueue);
		}
		
		PatientQueue saved = dao.savePatientQueue(patientQueue);
		// Create a minimal audit event on create/update
		createAuditEvent(saved, PatientQueueEvent.EventType.UPDATED, null);
		return saved;
	}
	
	private void hydrateDerivedFields(PatientQueue patientQueue) {
		// queueDate defaults to today
		if (patientQueue.getQueueDate() == null) {
			java.sql.Date d = new java.sql.Date(new Date().getTime());
			patientQueue.setQueueDate(d);
		}
		
		// ticketNumber defaults to legacy visitNumber if not set
		if ((patientQueue.getTicketNumber() == null || patientQueue.getTicketNumber().trim().isEmpty())
		        && patientQueue.getVisitNumber() != null) {
			patientQueue.setTicketNumber(patientQueue.getVisitNumber());
		}
		
		// facilityLocation defaults to the highest parent of locationTo, falling back to locationFrom
		if (patientQueue.getFacilityLocation() == null) {
			Location seed = patientQueue.getLocationTo() != null ? patientQueue.getLocationTo() : patientQueue
			        .getLocationFrom();
			if (seed != null) {
				Location root = seed;
				while (root.getParentLocation() != null) {
					root = root.getParentLocation();
				}
				patientQueue.setFacilityLocation(root);
			}
		}
		
		// If status is null, default to legacy PENDING
		if (patientQueue.getStatus() == null) {
			patientQueue.setStatus(PatientQueue.Status.PENDING);
		}
	}
	
	private void createAuditEvent(PatientQueue patientQueue, PatientQueueEvent.EventType type, String details) {
		try {
			PatientQueueEvent event = new PatientQueueEvent();
			event.setPatientQueue(patientQueue);
			event.setEventType(type);
			event.setEventTime(new Date());
			event.setActorUser(Context.getAuthenticatedUser());
			event.setFromQueueLocation(patientQueue.getLocationTo());
			event.setToQueueLocation(patientQueue.getLocationTo());
			event.setFromServiceLocation(patientQueue.getQueueRoom());
			event.setToServiceLocation(patientQueue.getQueueRoom());
			event.setDetails(details);
			dao.savePatientQueueEvent(event);
		}
		catch (Exception ignore) {
			// Do not block core queue operations if audit logging fails
		}
	}
	
	/**
	 * @see PatientQueueingService#getPatientQueueById(Integer)
	 */
	public PatientQueue getPatientQueueById(Integer queueId) {
		return dao.getPatientQueueById(queueId);
	}
	
	/**
	 * @see PatientQueueingService#getPatientQueueList(Provider, Date, Date, Location, Location,
	 *      Patient, PatientQueue.Status)
	 */
	public List<PatientQueue> getPatientQueueList(Provider provider, Date fromDate, Date toDate, Location locationTo,
	        Location locationFrom, Patient patient, PatientQueue.Status status) {
		return dao.getPatientQueueList(provider, fromDate, toDate, locationTo, locationFrom, patient, status, null);
	}
	
	/**
	 * @see PatientQueueingService#completePatientQueue(PatientQueue)
	 */
	@Override
	public PatientQueue completePatientQueue(PatientQueue patientQueue) {
		patientQueue.setStatus(PatientQueue.Status.COMPLETED);
		patientQueue.setDateCompleted(new Date());
		patientQueue.setEndedAt(new Date());
		PatientQueue saved = dao.savePatientQueue(patientQueue);
		createAuditEvent(saved, PatientQueueEvent.EventType.COMPLETED, null);
		return saved;
	}
	
	/**
	 * @see PatientQueueingService#getIncompletePatientQueue(Patient, Location)
	 */
	public PatientQueue getIncompletePatientQueue(Patient patient, Location locationTo) {
		
		return dao.getIncompletePatientQueue(patient, locationTo, null);
	}
	
	/**
	 * @see PatientQueueingService#getMostRecentQueue(Patient)
	 */
	@Override
	public PatientQueue getMostRecentQueue(Patient patient) {
		return dao.getMostRecentQueue(patient);
	}
	
	/**
	 * @see PatientQueueingService#assignVisitNumberForToday(PatientQueue)
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
	 * @see PatientQueueingService#generateVisitNumber(Location, Patient)
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
	 * @see PatientQueueingService#getPatientQueueListBySearchParams(String, Date, Date, Location,
	 *      Location, PatientQueue.Status)
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
	 * @see PatientQueueingService#getPatientQueueList(Provider, Date, Date, Location, Location,
	 *      Patient, PatientQueue.Status, Location)
	 */
	public List<PatientQueue> getPatientQueueList(Provider provider, Date fromDate, Date toDate, Location locationTo,
	        Location locationFrom, Patient patient, PatientQueue.Status status, Location queueRoom) {
		return dao.getPatientQueueList(provider, fromDate, toDate, locationTo, locationFrom, patient, status, queueRoom);
	}
	
	/**
	 * @see PatientQueueingService#getIncompletePatientQueue(Patient, Location, Location )
	 */
	public PatientQueue getIncompletePatientQueue(Patient patient, Location locationTo, Location queueRoom) {
		
		return dao.getIncompletePatientQueue(patient, locationTo, queueRoom);
	}
	
	/**
	 * @see PatientQueueingService#getPatientQueueListBySearchParams(String, Date, Date, Location,
	 *      Location, PatientQueue.Status, Location)
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
	 * @see PatientQueueingService#getPatientQueueByUuid(String)
	 */
	@Override
	public PatientQueue getPatientQueueByUuid(String uuid) {
		return dao.getPatientQueueByUUID(uuid);
	}
	
	@Override
	public PatientQueue getPatientQueueByTicketNumber(String ticketNumberOrVisitNumber, Date fromDate, Date toDate) {
		return dao.getPatientQueueByTicketNumber(ticketNumberOrVisitNumber, fromDate, toDate);
	}
	
	@Override
	public PatientQueue checkInByTicketNumber(String ticketNumberOrVisitNumber, Location facilityLocation, String deviceId) {
		Date today = new Date();
		PatientQueue pq = dao.getPatientQueueByTicketNumber(ticketNumberOrVisitNumber, OpenmrsUtil.firstSecondOfDay(today),
		    OpenmrsUtil.getLastMomentOfDay(today));
		if (pq == null) {
			return null;
		}
		// If facilityLocation is provided, ensure the queue entry belongs to that facility
		if (facilityLocation != null) {
			hydrateDerivedFields(pq);
			if (pq.getFacilityLocation() != null && !pq.getFacilityLocation().equals(facilityLocation)) {
				return null;
			}
		}
		// Only move forward (do not override completed/cancelled)
		if (pq.getStatus() != PatientQueue.Status.COMPLETED && pq.getStatus() != PatientQueue.Status.CANCELLED) {
			pq.setCheckedInAt(new Date());
			pq.setStatus(PatientQueue.Status.PRESENT);
			PatientQueue saved = dao.savePatientQueue(pq);
			createAuditEvent(saved, PatientQueueEvent.EventType.CHECKED_IN, deviceId != null ? "deviceId=" + deviceId : null);
			return saved;
		}
		return pq;
	}
	
	/**
	 * @see PatientQueueingService#pickPatientQueue(PatientQueue, Provider, Location)
	 */
	@Override
	public PatientQueue pickPatientQueue(PatientQueue patientQueue, Provider provider, Location queueRoom) {
		patientQueue.setStatus(PatientQueue.Status.PICKED);
		patientQueue.setDatePicked(new Date());
		patientQueue.setCalledAt(new Date());
		patientQueue.setProvider(provider);
		patientQueue.setQueueRoom(queueRoom);
		PatientQueue saved = dao.savePatientQueue(patientQueue);
		createAuditEvent(saved, PatientQueueEvent.EventType.CALLED, null);
		return saved;
	}
	
	/**
	 * @see PatientQueueingService# getPatientQueueByParentLocation(org.openmrs.Location,
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
}
