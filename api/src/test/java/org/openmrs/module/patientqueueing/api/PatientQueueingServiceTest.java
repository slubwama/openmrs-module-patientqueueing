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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientqueueing.QueueingUtils;
import org.openmrs.module.patientqueueing.api.dao.PatientQueueingDao;
import org.openmrs.module.patientqueueing.api.impl.PatientQueueingServiceImpl;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * This is a unit test, which verifies logic in PatientQueueingService. It extends
 * BaseModuleContextSensitiveTest, thus it is run without the in-memory DB and Spring context.
 */
public class PatientQueueingServiceTest extends BaseModuleContextSensitiveTest {
	
	private static final String QUEUE_STANDARD_DATASET_XML = "org/openmrs/module/patientqueueing/standardTestDataset.xml";
	
	private static Logger logger = LoggerFactory.getLogger(PatientQueueingServiceTest.class);
	
	@Before
	public void initialize() throws Exception {
		executeDataSet(QUEUE_STANDARD_DATASET_XML);
	}
	
	@InjectMocks
	PatientQueueingServiceImpl patientQueueingService;
	
	@Mock
	PatientQueueingDao dao;
	
	@Before
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void getIncompletePatientQueue_shouldReturnInCompletePatientQueue() throws Exception {
		
		PatientQueueingService patientQueueingService = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		
		Location location = Context.getLocationService().getLocation(1);
		
		PatientQueue patientQueue = patientQueueingService.getIncompletePatientQueue(patient, location);
		
		Assert.assertNotNull(patientQueue);
		
		Assert.assertEquals(PatientQueue.Status.PENDING, patientQueue.getStatus());
	}
	
	@Test
	public void completePatientQueue_shouldSetAndReturnPatientQueueWithCompletedStatus() throws Exception {
		
		PatientQueueingService patientQueueingService = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		Location location = Context.getLocationService().getLocation(1);
		
		List<PatientQueue> patientQueueList = Context.getService(PatientQueueingService.class).getPatientQueueList(null,
		    null, null, location, null, patient, PatientQueue.Status.PENDING);
		
		PatientQueue patientQueue = patientQueueList.get(0);
		
		Assert.assertEquals(PatientQueue.Status.PENDING, patientQueue.getStatus());
		
		patientQueueingService.completePatientQueue(patientQueue);
		
		PatientQueue completedPatientQueue = patientQueueingService.getPatientQueueById(patientQueue.getPatientQueueId());
		
		Assert.assertNotNull(completedPatientQueue);
		
		Assert.assertEquals(PatientQueue.Status.COMPLETED, completedPatientQueue.getStatus());
	}
	
	@Test
	public void savePatientQueue_shouldIncreaseTheNumberOfPatientQueueInList() throws Exception {
		
		PatientQueueingService patientQueueingService = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		
		Location location = Context.getLocationService().getLocation(1);
		
		List<PatientQueue> originalPatientQueueList = patientQueueingService.getPatientQueueList(null, null, null, null,
		    null, null, null);
		
		PatientQueue patientQueue = new PatientQueue();
		patientQueue.setPatient(patient);
		patientQueue.setStatus(PatientQueue.Status.PENDING);
		patientQueue.setVisitNumber("QN-001");
		patientQueue.setEncounter(Context.getEncounterService().getEncounter(10000));
		patientQueue.setLocationFrom(location);
		patientQueue.setLocationTo(location);
		patientQueue.setPriority(0);
		patientQueue.setPriorityComment("Emergency");
		patientQueueingService.savePatientQue(patientQueue);
		
		List<PatientQueue> newPatientQueueList = patientQueueingService.getPatientQueueList(null, null, null, null, null,
		    null, null);
		
		Assert.assertEquals(originalPatientQueueList.size() + 1, newPatientQueueList.size());
		
		Assert.assertEquals("QN-001", newPatientQueueList.get(0).getVisitNumber());
		
	}
	
	@Test
	public void savePatientQueue_shouldCompleteExistingPatientQueueWithSamePatientAndLocation() throws Exception {
		
		PatientQueueingService patientQueueingService = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		
		Location location = Context.getLocationService().getLocation(1);
		
		List<PatientQueue> originalPatientQueueList = patientQueueingService.getPatientQueueList(null, null, null, null,
		    null, null, null);
		
		PatientQueue patientQueue = new PatientQueue();
		patientQueue.setPatient(patient);
		patientQueue.setStatus(PatientQueue.Status.PENDING);
		patientQueue.setVisitNumber("QN-001");
		patientQueue.setEncounter(Context.getEncounterService().getEncounter(10000));
		patientQueue.setLocationFrom(location);
		patientQueue.setLocationTo(location);
		patientQueue.setPriority(0);
		patientQueue.setPriorityComment("Emergency");
		PatientQueue patientQueue1 = patientQueueingService.savePatientQue(patientQueue);
		
		PatientQueue patientQueue2 = new PatientQueue();
		patientQueue2.setPatient(patient);
		patientQueue2.setStatus(PatientQueue.Status.PENDING);
		patientQueue2.setVisitNumber("QN-002");
		patientQueue2.setEncounter(Context.getEncounterService().getEncounter(10000));
		patientQueue2.setLocationFrom(location);
		patientQueue2.setLocationTo(location);
		patientQueue2.setPriority(0);
		patientQueue2.setPriorityComment("Emergency");
		patientQueueingService.savePatientQue(patientQueue2);
		
		Assert.assertEquals(PatientQueue.Status.COMPLETED, patientQueue1.getStatus());
		
	}
	
	@Test
	public void generatevisitNumber_shouldReturnNewvisitNumberForGivenPatientOnGivenDate() {
		PatientQueueingService patientQueueingService = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		
		Location location = Context.getLocationService().getLocation(1);
		
		String visitNumber = patientQueueingService.generateVisitNumber(location, patient);
		
		Assert.assertEquals(QueueingUtils.formatDateAsString(new Date(), null) + "-Unk" + "-001", visitNumber);
		
	}
	
	@Test
	public void generatevisitNumber_shouldReturnExistingvisitNumberForGivenPatientOnGivenDate() throws ParseException {
		PatientQueueingService patientQueueingService = Context.getService(PatientQueueingService.class);
		String dateString = "2019-10-07 18:53:56";
		Date date = null;
		
		date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateString);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		
		Location location = Context.getLocationService().getLocation(1);
		
		String visitNumber1 = patientQueueingService.generateVisitNumber(location, patient);
		
		String visitNumber2 = patientQueueingService.generateVisitNumber(location, patient);
		
		Assert.assertEquals(visitNumber1, visitNumber2);
		
	}
}
