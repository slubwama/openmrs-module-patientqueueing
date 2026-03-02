/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc.
 */
package org.openmrs.module.patientqueueing.api;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Context-sensitive tests verifying logic in PatientQueueingService.
 */
public class PatientQueueingServiceTest extends BaseModuleContextSensitiveTest {
	
	private static final String QUEUE_STANDARD_DATASET_XML = "org/openmrs/module/patientqueueing/standardTestDataset.xml";
	
	private static final Integer QUEUE_PRIORITY_ZERO = 0;
	
	private static final Integer QUEUE_PRIORITY_ONE = 1;
	
	private static final int STANDARD_VISIT_NUMBER_LENGTH = 18;
	
	@Before
	public void initialize() throws Exception {
		executeDataSet(QUEUE_STANDARD_DATASET_XML);
	}
	
	@Test
	public void getIncompletePatientQueue_shouldReturnInCompletePatientQueue() throws Exception {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		Location location = Context.getLocationService().getLocation(1);
		
		PatientQueue patientQueue = service.getIncompletePatientQueue(patient, location);
		
		Assert.assertNotNull(patientQueue);
		Assert.assertEquals(PatientQueue.Status.PENDING, patientQueue.getStatus());
	}
	
	@Test
	public void completePatientQueue_shouldSetAndReturnPatientQueueWithCompletedStatus() throws Exception {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		Location location = Context.getLocationService().getLocation(1);
		
		List<PatientQueue> patientQueueList = service.getPatientQueueList(null, null, null, location, null, patient,
		    PatientQueue.Status.PENDING);
		
		PatientQueue patientQueue = patientQueueList.get(0);
		Assert.assertEquals(PatientQueue.Status.PENDING, patientQueue.getStatus());
		
		service.completePatientQueue(patientQueue);
		
		PatientQueue completed = service.getPatientQueueById(patientQueue.getPatientQueueId());
		Assert.assertNotNull(completed);
		
		Assert.assertEquals(PatientQueue.Status.COMPLETED, completed.getStatus());
		Assert.assertNotNull("dateCompleted should be set", completed.getDateCompleted());
		
		// New field
		Assert.assertNotNull("endedAt should be set", completed.getEndedAt());
	}
	
	@Test
	public void savePatientQueue_shouldIncreaseTheNumberOfPatientQueueInList_andHydrateDerivedFields() throws Exception {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		Location location = Context.getLocationService().getLocation(1);
		
		List<PatientQueue> originalList = service.getPatientQueueList(null, null, null, null, null, null, null);
		
		PatientQueue patientQueue = new PatientQueue();
		patientQueue.setPatient(patient);
		patientQueue.setStatus(PatientQueue.Status.PENDING);
		patientQueue.setVisitNumber("20/10/2019-Unk-001");
		patientQueue.setEncounter(Context.getEncounterService().getEncounter(10000));
		patientQueue.setLocationFrom(location);
		patientQueue.setLocationTo(location);
		patientQueue.setPriority(0);
		patientQueue.setPriorityComment("Emergency");
		service.savePatientQue(patientQueue);
		
		// Intentionally do NOT set ticketNumber / queueDate / facilityLocation
		PatientQueue saved = service.savePatientQue(patientQueue);
		
		List<PatientQueue> newList = service.getPatientQueueList(null, null, null, null, null, null, null);
		Assert.assertEquals(originalList.size() + 1, newList.size());
		
		// Reload and assert reliably
		PatientQueue reloaded = service.getPatientQueueById(saved.getPatientQueueId());
		Assert.assertNotNull(reloaded);
		
		Assert.assertEquals("20/10/2019-Unk-001", reloaded.getVisitNumber());
		
		// New derived fields hydrated on save
		Assert.assertNotNull("queueDate should be hydrated", reloaded.getQueueDate());
		Assert.assertNotNull("facilityLocation should be hydrated", reloaded.getFacilityLocation());
		Assert.assertNotNull("ticketNumber should be hydrated", reloaded.getTicketNumber());
		Assert.assertEquals("ticketNumber should default to visitNumber when missing", reloaded.getVisitNumber(),
		    reloaded.getTicketNumber());
	}
	
	@Test
	public void savePatientQueue_shouldCompleteExistingPatientQueueWithSamePatientAndLocation() throws Exception {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);

		Location location = Context.getLocationService().getLocation(1);
		
		List<PatientQueue> originalPatientQueueList = service.getPatientQueueList(null, null, null, null,
		    null, null, null);

		PatientQueue patientQueue = new PatientQueue();
		patientQueue.setPatient(patient);
		patientQueue.setStatus(PatientQueue.Status.PENDING);
		patientQueue.setVisitNumber("20/10/2019-Unk-001");
		patientQueue.setEncounter(Context.getEncounterService().getEncounter(10000));
		patientQueue.setLocationFrom(location);
		patientQueue.setLocationTo(location);
		patientQueue.setPriority(0);
		patientQueue.setPriorityComment("Emergency");
		patientQueue = service.savePatientQue(patientQueue);
		
		PatientQueue patientQueue2 = new PatientQueue();
		patientQueue2.setPatient(patient);
		patientQueue2.setStatus(PatientQueue.Status.PENDING);
		patientQueue2.setVisitNumber("20/10/2019-Unk-002");
		patientQueue2.setEncounter(Context.getEncounterService().getEncounter(10000));
		patientQueue2.setLocationFrom(location);
		patientQueue2.setLocationTo(location);
		patientQueue2.setPriority(0);
		patientQueue2.setPriorityComment("Emergency");
		service.savePatientQue(patientQueue2);
		
		// Reload pq1 to ensure we see DB state after service completed it
		PatientQueue pq1Reloaded = service.getPatientQueueById(patientQueue.getPatientQueueId());
		Assert.assertEquals(PatientQueue.Status.COMPLETED, pq1Reloaded.getStatus());
		Assert.assertNotNull("dateCompleted should be set on auto-completed queue", pq1Reloaded.getDateCompleted());
		Assert.assertNotNull("endedAt should be set on auto-completed queue", pq1Reloaded.getEndedAt());
	}
	
	@Test
	public void generateVisitNumber_shouldReturnVisitNumberBasedOnPatientAndLocation() {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		Location location = Context.getLocationService().getLocation(1);
		
		String visitNumber = service.generateVisitNumber(location, patient);
		
		SimpleDateFormat formatter = new SimpleDateFormat(Context.getAdministrationService().getGlobalProperty(
		    "patientqueueing.defaultDateFormat"));
		
		Assert.assertEquals(formatter.format(new Date()) + "-Unk" + "-001", visitNumber);
	}
	
	@Test
	public void assignVisitNumber_shouldAssignPatientQueueNewVisitNumberWhenNoPatientQueueExistsOnSameDate() {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		Location location = Context.getLocationService().getLocation(1);
		
		String visitNumber = service.generateVisitNumber(location, patient);
		
		PatientQueue patientQueue = new PatientQueue();
		patientQueue.setPatient(patient);
		patientQueue.setStatus(PatientQueue.Status.PENDING);
		patientQueue.setEncounter(Context.getEncounterService().getEncounter(10000));
		patientQueue.setLocationFrom(location);
		patientQueue.setLocationTo(location);
		patientQueue.setPriority(0);
		patientQueue.setPriorityComment("Emergency");
		service.assignVisitNumberForToday(patientQueue);
		service.savePatientQue(patientQueue);
		
		service.assignVisitNumberForToday(patientQueue);
		service.savePatientQue(patientQueue);
		
		Assert.assertEquals(visitNumber, patientQueue.getVisitNumber());
		Assert.assertEquals("ticketNumber should default to visitNumber", patientQueue.getVisitNumber(), patientQueue.getTicketNumber());
	}
	
	@Test
	public void assignVisitNumber_shouldAssignPatientQueueExistingVisitNumberWhenPatientQueueExistsOnSameDate()
	        throws ParseException {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		Location location = Context.getLocationService().getLocation(1);
		
		PatientQueue patientQueue = new PatientQueue();
		patientQueue.setPatient(patient);
		patientQueue.setStatus(PatientQueue.Status.PENDING);
		patientQueue.setEncounter(Context.getEncounterService().getEncounter(10000));
		patientQueue.setLocationFrom(location);
		patientQueue.setLocationTo(location);
		patientQueue.setPriority(0);
		patientQueue.setPriorityComment("Emergency");
		service.assignVisitNumberForToday(patientQueue);
		service.savePatientQue(patientQueue);
		
		PatientQueue patientQueue2 = new PatientQueue();
		patientQueue2.setPatient(patient);
		patientQueue2.setStatus(PatientQueue.Status.PENDING);
		patientQueue2.setEncounter(Context.getEncounterService().getEncounter(10000));
		patientQueue2.setLocationFrom(location);
		patientQueue2.setLocationTo(location);
		patientQueue2.setPriority(0);
		patientQueue2.setPriorityComment("Emergency");
		service.assignVisitNumberForToday(patientQueue2);
		service.savePatientQue(patientQueue2);
		
		Assert.assertEquals(patientQueue.getVisitNumber(), patientQueue2.getVisitNumber());
	}
	
	@Test
	public void getMostRecentQueue_shouldReturnMostRecentPatientQueue() {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		PatientQueue patientQueue = service.getPatientQueueById(2);
		
		Assert.assertEquals(patientQueue, service.getMostRecentQueue(patient));
	}
	
	@Test
	public void savePatientQueue_shouldNotCompletePatientQueueOnEdit() throws Exception {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);

		Location location = Context.getLocationService().getLocation(1);
		
		PatientQueue patientQueue = new PatientQueue();
		patientQueue.setPatient(patient);
		patientQueue.setStatus(PatientQueue.Status.PENDING);
		patientQueue.setEncounter(Context.getEncounterService().getEncounter(10000));
		patientQueue.setLocationFrom(location);
		patientQueue.setLocationTo(location);
		patientQueue.setPriority(QUEUE_PRIORITY_ZERO);
		patientQueue.setPriorityComment("Emergency");
		
		service.assignVisitNumberForToday(patientQueue);
		service.savePatientQue(patientQueue);
		
		PatientQueue patientQueueToEdit = service.getPatientQueueById(patientQueue.getPatientQueueId());
		patientQueueToEdit.setPriority(QUEUE_PRIORITY_ONE);
		patientQueueToEdit.setPriorityComment("Non-Emergency");
		
		PatientQueue editedPatientQueue = service.savePatientQue(patientQueueToEdit);
		
		Assert.assertEquals(QUEUE_PRIORITY_ONE, editedPatientQueue.getPriority());
		Assert.assertEquals("Non-Emergency", editedPatientQueue.getPriorityComment());
		Assert.assertEquals(PatientQueue.Status.PENDING, editedPatientQueue.getStatus());
	}
	
	@Test
	public void generateVisitNumber_shouldNotThrowOutOfIndexExceptionWhenPreviousQueueVisitNumberLengthLessThanStandardLength() {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		Location location = Context.getLocationService().getLocation(1);
		
		PatientQueue patientQueue = new PatientQueue();
		patientQueue.setPatient(patient);
		patientQueue.setStatus(PatientQueue.Status.PENDING);
		patientQueue.setEncounter(Context.getEncounterService().getEncounter(10000));
		patientQueue.setLocationFrom(location);
		patientQueue.setLocationTo(location);
		patientQueue.setVisitNumber("20/10/2019-002");
		service.savePatientQue(patientQueue);
		
		Assert.assertNotEquals(STANDARD_VISIT_NUMBER_LENGTH, patientQueue.getVisitNumber());
		
		service.generateVisitNumber(location, patient);
	}
	
	@Test
	public void getPatientQueueListBySearchParams_shouldReturnPatientQueuesThatMatchesParameters() throws Exception {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Date dateCreated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2019-10-07 19:08:26");
		Patient patient = Context.getPatientService().getPatient(10000);
		
		List<PatientQueue> patientQueueList = service.getPatientQueueListBySearchParams("Mukasa",
		    OpenmrsUtil.firstSecondOfDay(dateCreated), OpenmrsUtil.getLastMomentOfDay(dateCreated), null, null,
		    PatientQueue.Status.PENDING);
		
		Assert.assertEquals(1, patientQueueList.size());
		Assert.assertEquals(patient, patientQueueList.get(0).getPatient());
		Assert.assertEquals("Mukasa", patientQueueList.get(0).getPatient().getFamilyName());
	}
	
	@Test
	public void getPatientQueueListBySearchParams_shouldReturnNotReturnPatientQueuesThatDontMatchParameters()
	        throws Exception {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(8);
		Location location = Context.getLocationService().getLocation(1);
		
		Assert.assertEquals("Anet", patient.getGivenName());
		
		List<PatientQueue> patientQueueList = service.getPatientQueueListBySearchParams("Anet", null, null, null, location,
		    null);
		
		Assert.assertEquals(0, patientQueueList.size());
	}
	
	@Test
	public void pickPatientQueue_shouldSetAndReturnPatientQueueWithPickedStatus_andCalledAt() throws Exception {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		Location location = Context.getLocationService().getLocation(1);
		
		List<PatientQueue> patientQueueList = service.getPatientQueueList(null, null, null, location, null, patient,
		    PatientQueue.Status.PENDING);
		
		PatientQueue patientQueue = patientQueueList.get(0);

		Assert.assertEquals(PatientQueue.Status.PENDING, patientQueue.getStatus());
		
		service.pickPatientQueue(patientQueue, null, null);
		
		PatientQueue picked = service.getPatientQueueById(patientQueue.getPatientQueueId());
		Assert.assertNotNull(picked);
		
		Assert.assertEquals(PatientQueue.Status.PICKED, picked.getStatus());
		Assert.assertNotNull("datePicked should be set", picked.getDatePicked());
		
		// New field set in pickPatientQueue()
		Assert.assertNotNull("calledAt should be set", picked.getCalledAt());
	}
	
	@Test
	public void completePatientQueue_shouldSetAndReturnPatientQueueWithDateCompleted() throws Exception {
		PatientQueueingService service = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		Location location = Context.getLocationService().getLocation(1);
		
		List<PatientQueue> patientQueueList = service.getPatientQueueList(null, null, null, location, null, patient,
		    PatientQueue.Status.PENDING);
		
		PatientQueue patientQueue = patientQueueList.get(0);

		Assert.assertEquals(PatientQueue.Status.PENDING, patientQueue.getStatus());
		
		service.completePatientQueue(patientQueue);
		
		PatientQueue completed = service.getPatientQueueById(patientQueue.getPatientQueueId());
		Assert.assertNotNull(completed);
		
		Assert.assertEquals(PatientQueue.Status.COMPLETED, completed.getStatus());
		Assert.assertNotNull("dateCompleted should be set", completed.getDateCompleted());
		
		// New field
		Assert.assertNotNull("endedAt should be set", completed.getEndedAt());
	}

	@Test
	public void getPatientsInQueueRoom_ShouldReturnPatientsInQueueRoomsOfParentLocation() throws ParseException {
		Location parentLocation = Context.getLocationService().getLocation(1);
		Date dateCreated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2023-07-07 19:08:26");
		
		List<PatientQueue> patientQueueList = Context.getService(PatientQueueingService.class).getPatientQueueByParentLocation(
		    parentLocation, PatientQueue.Status.PENDING, OpenmrsUtil.firstSecondOfDay(dateCreated),
		    OpenmrsUtil.getLastMomentOfDay(dateCreated), true);
		
		Assert.assertNotNull(patientQueueList);
		Assert.assertEquals(2, patientQueueList.size());
		Assert.assertEquals(parentLocation, patientQueueList.get(0).getQueueRoom().getParentLocation());
		Assert.assertEquals("Room 1", patientQueueList.get(0).getQueueRoom().getName());
	}
	
	@Test
	public void getPatientsInQueue_ShouldReturnPatientsInQueueChildLocationsOfParentLocation() throws ParseException {
		Location parentLocation = Context.getLocationService().getLocation(1);
		Date dateCreated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2023-07-07 19:08:26");
		
		List<PatientQueue> patientQueueList = Context.getService(PatientQueueingService.class).getPatientQueueByParentLocation(
		    parentLocation, PatientQueue.Status.PENDING, OpenmrsUtil.firstSecondOfDay(dateCreated),
		    OpenmrsUtil.getLastMomentOfDay(dateCreated), false);
		
		Assert.assertNotNull(patientQueueList);
		Assert.assertEquals(3, patientQueueList.size());
		Assert.assertEquals("Sub Sub Room 1 R2", patientQueueList.get(2).getQueueRoom().getName());
	}
}
