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
import org.openmrs.module.patientqueueing.PatientQueueingConfig;
import org.openmrs.module.patientqueueing.api.dao.PatientQueueingDao;
import org.openmrs.module.patientqueueing.api.impl.PatientQueueingServiceImpl;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a unit test, which verifies logic in PatientQueueingService. It extends
 * BaseModuleContextSensitiveTest, thus it is run without the in-memory DB and Spring context.
 */
public class PatientQueueingServiceTest extends BaseModuleContextSensitiveTest {
	
	private static final String QUEUE_STANDARD_DATASET_XML = "org/openmrs/module/patientqueueing/standardTestDataset.xml";
	
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
	public void getPatientQueueList_shouldReturnAllPatientQueueListWithPatientThatMatchesTheSearchString() throws Exception {
		
		PatientQueueingService patientQueueingService = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		
		Location location = Context.getLocationService().getLocation(1);
		
		List<PatientQueue> patientQueueList = patientQueueingService.getPatientQueueList("Mukasa", null, null, null, null,
		    location, null, null);
		
		Assert.assertEquals(2, patientQueueList.size());
		
		Assert.assertEquals(patient, patientQueueList.get(0).getPatient());
	}
	
	@Test
	public void getPatientQueueList_shouldReturnPatientQueueListWithPatientThatMatchesTheSearchStringAndTheGivenStatus()
	        throws Exception {
		
		PatientQueueingService patientQueueingService = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(10000);
		
		List<PatientQueue> patientQueueList = patientQueueingService.getPatientQueueList("Mukasa", null, null, null, null,
		    null, null, PatientQueueingConfig.status.PENDING.name());
		
		Assert.assertEquals(1, patientQueueList.size());
		
		Assert.assertEquals(patient, patientQueueList.get(0).getPatient());
		
		Assert.assertEquals(PatientQueueingConfig.status.PENDING.name(), patientQueueList.get(0).getStatus());
	}
	
	@Test
	public void getPatientQueueList_shouldReturnNotReturnAnyPatientQueueForSearchStringProvidedWithExistingPatient()
	        throws Exception {
		
		PatientQueueingService patientQueueingService = Context.getService(PatientQueueingService.class);
		
		Patient patient = Context.getPatientService().getPatient(8);
		
		Location location = Context.getLocationService().getLocation(1);
		
		Assert.assertEquals("Anet", patient.getGivenName());
		
		List<PatientQueue> patientQueueList = patientQueueingService.getPatientQueueList("Anet", null, null, null, null,
		    location, null, null);
		
		Assert.assertEquals(0, patientQueueList.size());
		
	}
	
	@Test
	public void getPatientQueueList_shouldReturnNotReturnAnyPatientQueueForSearchStringProvidedWithNonExistingPatient()
	        throws Exception {
		
		PatientQueueingService patientQueueingService = Context.getService(PatientQueueingService.class);
		
		Location location = Context.getLocationService().getLocation(1);
		
		List<Patient> patientList = new ArrayList<Patient>();
		
		for (Patient patient1 : Context.getPatientService().getAllPatients()) {
			if (patient1.getPerson().getPersonName().getFullName().contains("Pan")) {
				patientList.add(patient1);
			}
		}
		
		Assert.assertEquals(0, patientList.size());
		
		List<PatientQueue> patientQueueList = patientQueueingService.getPatientQueueList("Pan", null, null, null, null,
		    location, null, null);
		
		Assert.assertEquals(0, patientQueueList.size());
		
	}
}
