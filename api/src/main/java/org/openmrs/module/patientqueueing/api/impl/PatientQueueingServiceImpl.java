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
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.patientqueueing.QueueingUtils;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.api.dao.PatientQueueingDao;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import java.text.ParseException;

public class PatientQueueingServiceImpl extends BaseOpenmrsService implements PatientQueueingService {
	
	private static final Logger log = LoggerFactory.getLogger(PatientQueueingServiceImpl.class);
	
	PatientQueueingDao dao;
	
	public void setDao(PatientQueueingDao dao) {
		this.dao = dao;
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#savePatientQue(org.openmrs.module.patientqueueing.model.PatientQueue)
	 */
	public PatientQueue savePatientQue(PatientQueue patientQueue) {
		PatientQueue currentQueue = dao.getIncompletePatientQueue(patientQueue.getPatient(), patientQueue.getLocationTo());
		
		if (currentQueue != null) {
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
		return dao.getPatientQueueList(provider, fromDate, toDate, locationTo, locationFrom, patient, status);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#completePatientQueue(org.openmrs.module.patientqueueing.model.PatientQueue)
	 */
	@Override
	public PatientQueue completePatientQueue(PatientQueue patientQueue) {
		patientQueue.setStatus(PatientQueue.Status.COMPLETED);
		return dao.savePatientQueue(patientQueue);
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getIncompletePatientQueue(org.openmrs.Patient,
	 *      org.openmrs.Location)
	 */
	public PatientQueue getIncompletePatientQueue(Patient patient, Location locationTo) {
		
		return dao.getIncompletePatientQueue(patient, locationTo);
	}
	
	/**
	 * This Method checks if a patient visitNumber exists
	 * 
	 * @param visitNumber A visitNumber in form of a string which will be check to determine if it
	 *            exists
	 * @return
	 */
	private boolean isvisitNumberIdExisting(String visitNumber) {
		
		return getPatientQueueByVisitNumber(visitNumber) != null;
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueByVisitNumber(java.lang.String)
	 */
	@Override
	public PatientQueue getPatientQueueByVisitNumber(String visitNumber) {
		List<PatientQueue> patientQueueList = dao.getPatientQueueByVisitNumber(visitNumber);
		if (!patientQueueList.isEmpty()) {
			return patientQueueList.get(0);
		} else {
			return null;
		}
		
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#generateVisitNumber(org.openmrs.Location,
	 *      org.openmrs.Patient)
	 */
	public String generateVisitNumber(Location location, Patient patient) {
		
		Calendar fromDate = new GregorianCalendar();
		Calendar toDate = new GregorianCalendar();
		fromDate.set(Calendar.HOUR_OF_DAY, 0);
		fromDate.set(Calendar.MINUTE, 0);
		fromDate.set(Calendar.SECOND, 0);
		
		toDate.set(Calendar.HOUR_OF_DAY, 23);
		toDate.set(Calendar.MINUTE, 59);
		toDate.set(Calendar.SECOND, 59);
		
		Date newFromDate = fromDate.getTime();
		Date newToDate = toDate.getTime();
		
		List<PatientQueue> patientQueueList = new ArrayList<PatientQueue>();
		
		try {
			patientQueueList = getPatientQueueList(null, QueueingUtils.changeTimeOfDate(newFromDate, "00:00:00"),
			    QueueingUtils.changeTimeOfDate(newToDate, "23:59:59"), null, location, patient, null);
		}
		catch (ParseException e) {
			log.error("", e);
		}
		
		if (!patientQueueList.isEmpty() && patientQueueList.get(0).getVisitNumber() != null) {
			return patientQueueList.get(0).getVisitNumber();
		} else {
			
			String dateString = QueueingUtils.formatDateAsString(newToDate, null);
			
			String letter = location.getName();
			
			if (letter.length() > 3) {
				letter = letter.substring(0, 3);
			}
			
			String defaultvisitNumber;
			
			int id = 0;
			do {
				++id;
				String appendZeroes;
				
				if (id <= 9) {
					appendZeroes = "00";
				} else if (id < 100) {
					appendZeroes = "0";
				} else
					appendZeroes = "";
				
				defaultvisitNumber = dateString + "-" + letter + "-" + appendZeroes + id;
			} while (isvisitNumberIdExisting(defaultvisitNumber));
			
			return defaultvisitNumber;
		}
	}
	
	@Override
	public List<PatientQueue> getPatientQueueList(String searchString, Date fromDate, Date toDate, Patient patient,
	        Provider provider, Location locationTo, Location locationFrom, String status) {
		return dao.getPatientQueueList(processPatientSearchString(searchString), fromDate, toDate, patient, provider,
		    locationTo, locationFrom, status);
	}
	
	private List<Patient> processPatientSearchString(String searchString) {
		PatientService patientService = Context.getPatientService();
		
		List list = Arrays.asList(searchString.split(" "));
		
		List<Patient> patientList = new ArrayList<Patient>();
		
		for (Object o : list) {
			List<Patient> patients = patientService.getPatients(o.toString());
			patientList.addAll(patients);
		}
		return patientList;
	}
	
}
