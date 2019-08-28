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
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.api.dao.PatientQueueingDao;
import org.openmrs.module.patientqueueing.model.PatientQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class PatientQueueingServiceImpl extends BaseOpenmrsService implements PatientQueueingService {
	
	PatientQueueingDao dao;
	
	public void setDao(PatientQueueingDao dao) {
		this.dao = dao;
	}
	
	public PatientQueue getPatientQueueById(String queueId) throws APIException {
		return dao.getPatientQueueById(queueId);
	}
	
	public List<PatientQueue> getPatientQueueByPatient(Patient patient) throws APIException {
		return dao.getPatientQueueByPatient(patient);
	}
	
	public List<PatientQueue> getPatientInQueueList(Provider provider, Date fromDate, Date toDate, Location sessionLocation)
	        throws APIException {
		return dao.getPatientInQueue(provider, fromDate, toDate, sessionLocation);
	}
	
	@Override
	public List<PatientQueue> getPatientInQueueList(Date fromDate, Date toDate, Location sessionLocation)
	        throws APIException {
		return dao.getPatientInQueue(null, fromDate, toDate, sessionLocation);
	}
	
	public PatientQueue savePatientQue(PatientQueue patientQueue) throws APIException {
		return dao.savePatientQueue(patientQueue);
	}
	
	@Override
	public PatientQueue completeQueue(PatientQueue patientQueue) throws APIException {
		return null;
	}
	
	@Override
	public List<PatientQueue> searchQueue(String searchString, String fromDate, String toDate, Provider provider,
	        Location sessionLocation) throws APIException {
		String searchOptions = processSearchString(searchString);
		
		String whereClause = "WHERE patient_queue.patient_id in (" + searchOptions + ")";
		
		if (provider != null) {
			String whereClauseLocation = " AND patient_queue.location_from='" + sessionLocation.getLocationId() + "' ";
			whereClause += whereClauseLocation;
		}
		
		if (provider != null) {
			String whereClauseProvider = " AND patient_queue.provider_id='" + provider.getProviderId() + "' ";
			whereClause += whereClauseProvider;
		}
		
		if (fromDate != null && toDate != null) {
			String whereClauseDateCreated = " AND patient_queue.date_created BETWEEN '" + fromDate + "' AND '" + toDate
			        + "'";
			whereClause += whereClauseDateCreated;
		}
		
		String query = "select patient_queue.* from patient_queue inner join patient on (patient_queue.patient_id=patient.patient_id) %s";
		query = String.format(query, whereClause);
		
		return dao.searchQueue(query);
	}
	
	private String processSearchString(String searchString) {
		PatientService patientService = Context.getPatientService();
		
		List list = Arrays.asList(searchString.split(" "));
		
		List patientIds = new ArrayList();
		String s = "";
		
		for (Object o : list) {
			List<Patient> patients = patientService.getPatients(o.toString());
			
			if (patients != null) {
				for (Patient patient : patients) {
					patientIds.add(patient.getPatientId());
				}
			}
		}
		if (!patientIds.isEmpty()) {
			s = patientIds.toString().replace("]", "").replace("[", "");
		}
		return s;
	}
	
}