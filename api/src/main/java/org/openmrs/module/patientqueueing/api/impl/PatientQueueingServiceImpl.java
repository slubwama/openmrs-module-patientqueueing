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
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.api.dao.PatientQueueingDao;
import org.openmrs.module.patientqueueing.model.PatientQueue;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PatientQueueingServiceImpl extends BaseOpenmrsService implements PatientQueueingService {
	
	PatientQueueingDao dao;
	
	public void setDao(PatientQueueingDao dao) {
		this.dao = dao;
	}
	
	public PatientQueue getPatientQueueById(String queueId) {
		return dao.getPatientQueueById(queueId);
	}
	
	public List<PatientQueue> getPatientQueueByPatient(Patient patient) {
		return getPatientQueueList(null, null, null, patient, null, null, null, null);
	}
	
	public List<PatientQueue> getPatientInQueueList(Provider provider, Date fromDate, Date toDate, Location sessionLocation) {
		return dao.getPatientQueueList(null, fromDate, toDate, null, provider, sessionLocation, null, null);
	}
	
	public List<PatientQueue> getPatientInQueueList(Provider provider, Date fromDate, Date toDate, Location sessionLocation,
	        Patient patient, String status) {
		return dao.getPatientQueueList(null, fromDate, toDate, patient, provider, sessionLocation, null, status);
	}
	
	@Override
	public List<PatientQueue> getPatientInQueueList(Date fromDate, Date toDate, Location sessionLocation) {
		return dao.getPatientQueueList(null, fromDate, toDate, null, null, sessionLocation, null, null);
	}
	
	public PatientQueue savePatientQue(PatientQueue patientQueue) {
		return dao.savePatientQueue(patientQueue);
	}
	
	@Override
	public PatientQueue completeQueue(PatientQueue patientQueue, String status) {
		patientQueue.setStatus(status);
		return dao.savePatientQueue(patientQueue);
	}
	
	@Override
	public PatientQueue getPatientQueueByQueueNumber(String queueNumber) {
		List<PatientQueue> patientQueueList = dao.getPatientQueueByQueueNumber(queueNumber);
		if (!patientQueueList.isEmpty()) {
			return patientQueueList.get(0);
		} else {
			return null;
		}
		
	}
	
	@Override
	public List<PatientQueue> getPatientQueueList(String searchString, Date fromDate, Date toDate, Patient patient,
	        Provider provider, Location locationTo, Location locationFrom, String status) {
		return dao.getPatientQueueList(searchString, fromDate, toDate, patient, provider, locationTo, locationFrom, status);
	}
	
	/**
	 * Generate Checkin ID
	 * 
	 * @param location
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	public String generateQueueNumber(Location location) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		String date = sdf.format(new Date());
		String letter = location.getName();
		if (letter.length() > 3) {
			letter = letter.substring(0, 3);
		}
		String defaultQueueNumber = "";
		int id = 0;
		do {
			++id;
			defaultQueueNumber = date + "-" + letter + "-" + "00" + id;
		} while (isQueueNumberIdExisting(defaultQueueNumber));
		
		return defaultQueueNumber;
	}
	
	public boolean isQueueNumberIdExisting(String queueNumber) {
		PatientQueue patientQueue = getPatientQueueByQueueNumber(queueNumber);
		boolean isQueueNumberExisting = false;
		
		if (patientQueue != null) {
			isQueueNumberExisting = true;
		}
		
		return isQueueNumberExisting;
	}
	
}
