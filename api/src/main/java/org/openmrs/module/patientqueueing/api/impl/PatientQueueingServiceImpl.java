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
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.patientqueueing.QueueingUtils;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.api.dao.PatientQueueingDao;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PatientQueueingServiceImpl extends BaseOpenmrsService implements PatientQueueingService {
	
	private static final Logger log = LoggerFactory.getLogger(PatientQueueingServiceImpl.class);
	
	PatientQueueingDao dao;
	
	public void setDao(PatientQueueingDao dao) {
		this.dao = dao;
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueByQueueNumber(java.lang.String)
	 */
	@Override
	public PatientQueue getPatientQueueByQueueNumber(String queueNumber) {
		List<PatientQueue> patientQueueList = dao.getPatientQueueByQueueNumber(queueNumber);
		if (!patientQueueList.isEmpty()) {
			return patientQueueList.get(0);
		} else {
			return null;
		}
		
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#generateQueueNumber(org.openmrs.Location,
	 *      java.util.Date, org.openmrs.Patient)
	 */
	public String generateQueueNumber(Location location, Date date, Patient patient) {
		
		List<PatientQueue> patientQueueList = new ArrayList<PatientQueue>();
		
		try {
			patientQueueList = getPatientQueueList(null, QueueingUtils.convertDateToDefaultDateFormat(date, "00:00:00"),
			    QueueingUtils.convertDateToDefaultDateFormat(date, "23:59:59"), null, location, patient, null);
		}
		catch (ParseException e) {
			log.error("", e);
		}
		
		if (!patientQueueList.isEmpty() && patientQueueList.get(0).getQueueNumber() != null) {
			return patientQueueList.get(0).getQueueNumber();
		} else {
			
			String dateString = QueueingUtils.convertDateToDefaultDateFormatAsString(date, null);
			
			String letter = location.getName();
			
			if (letter.length() > 3) {
				letter = letter.substring(0, 3);
			}
			
			String defaultQueueNumber;
			
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
				
				defaultQueueNumber = dateString + "-" + letter + "-" + appendZeroes + id;
			} while (isQueueNumberIdExisting(defaultQueueNumber));
			
			return defaultQueueNumber;
		}
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
	 * This Method is a helper method to the generateQueueNumber which checks if the queueNumber
	 * generated exists
	 * 
	 * @param queueNumber A queueNumber in form of a string which will be check to determine if it
	 *            exists
	 * @return
	 */
	private boolean isQueueNumberIdExisting(String queueNumber) {
		PatientQueue patientQueue = getPatientQueueByQueueNumber(queueNumber);
		boolean queueNumberExists = false;
		
		if (patientQueue != null) {
			queueNumberExists = true;
		}
		return queueNumberExists;
	}
	
}
