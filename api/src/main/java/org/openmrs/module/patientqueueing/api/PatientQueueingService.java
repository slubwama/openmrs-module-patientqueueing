/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientqueueing.api;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * The main service of this module, which is exposed for other modules. See
 * moduleApplicationContext.xml on how it is wired up.
 */
public interface PatientQueueingService extends OpenmrsService {
	
	/**
	 * This Method Gets a patientQueue By the queueNumber
	 * 
	 * @param queueNumber The queueNumber String that is used to search for a patientQueue
	 * @return a patientQueue that matches the queueNumber passed
	 * @throws APIException
	 */
	@Transactional
	public PatientQueue getPatientQueueByQueueNumber(String queueNumber);
	
	/**
	 * This Generates a queueNumber based on location and date. The Number generated is unique for a
	 * patient on a given day A patient will only have one patientQueueNumber for a given day. The
	 * same number will be reassigned to another queue, incase its the same patient on the same day.
	 * 
	 * @param location Location which will be used to first three characters of the queueNumber
	 * @param date the date which will be appended to the queueNumber in format dd/MM/yyyy
	 * @param patient the Patient who the queueNumber is for in a given queue
	 * @return This will return a String with format LOC-dd/MM/yyy-000-1
	 * @throws ParseException
	 * @throws IOException
	 */
	public String generateQueueNumber(Location location, Date date, Patient patient);
	
	/**
	 * Get a single patient queue record by queueId. The queueId can not be null
	 * 
	 * @param queueId Id of the PatientQueue to be retrieved
	 * @return The Patient Queue that matches the queueId
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	public PatientQueue getPatientQueueById(Integer queueId);
	
	/**
	 * Update or Save patientQueue. Requires a patientQueue
	 * 
	 * @param patientQueue The PatientQueue to be saved
	 * @return PatientQueue that has been saved
	 * @throws APIException
	 */
	@Transactional
	public PatientQueue savePatientQue(PatientQueue patientQueue);
	
	/**
	 * Gets a list of patient queues basing on given parameters.
	 * 
	 * @param provider The provider where the patient was being sent. It Can be null
	 * @param fromDate lowest date a query will be built upon. It can be null
	 * @param toDate highest date a query will be built upon. It Can be null
	 * @param locationTo Location Where patient was sent to
	 * @param locationFrom Location Where patient was sent from
	 * @param patient The patient who is in the queue
	 * @param status Status such as COMPLETED,PENDING
	 * @return List<PatientQueue> A list of patientQueue that meet the parameters
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	public List<PatientQueue> getPatientQueueList(Provider provider, Date fromDate, Date toDate, Location locationTo,
	        Location locationFrom, Patient patient, PatientQueue.Status status);
	
	/**
	 * Mark passed patientQueue completed.
	 * 
	 * @param patientQueue The PatientQueue to be completed
	 * @return PatientQueue. The Queue that is completed
	 * @throws APIException
	 */
	@Transactional
	public PatientQueue completePatientQueue(PatientQueue patientQueue);
	
	/**
	 * This method gets the patientQueue for a patient at a given location which is not complete.
	 * 
	 * @param locationTo The Location where the patient was is queued to
	 * @param patient The patient who is in the queue
	 * @return a patient queue that meets the criteria of parameters
	 */
	@Transactional(readOnly = true)
	public PatientQueue getIncompletePatientQueue(Patient patient, Location locationTo);
}
