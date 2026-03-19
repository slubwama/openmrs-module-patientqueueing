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
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientqueueing.model.NonPatientQueue;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.Date;
import java.util.List;

public class NonPatientQueueingServiceTest extends BaseModuleContextSensitiveTest {
	
	private PatientQueueingService service;
	
	private Location testLocation;
	
	private Concept testQueueType;
	
	private Provider testProvider;
	
	@Override
	public Boolean useInMemoryDatabase() {
		return true;
	}
	
	@Before
	public void setUp() throws Exception {
		service = Context.getService(PatientQueueingService.class);
		executeDataSet("org/openmrs/module/patientqueueing/standardTestDataset.xml");
		
		testLocation = Context.getLocationService().getLocation(1);
		
		// Create a test provider if none exists
		List<Provider> providers = Context.getProviderService().getAllProviders();
		if (!providers.isEmpty()) {
			testProvider = providers.get(0);
		}
		
		// Create a test concept for queue type if none exists
		List<Concept> concepts = Context.getConceptService().getAllConcepts();
		if (!concepts.isEmpty()) {
			testQueueType = concepts.get(0);
		}
	}
	
	@Test
	public void shouldCreateNonPatientQueueEntry() {
		NonPatientQueue queue = service.createNonPatientQueueEntry("John Visitor", "+256123456789", testQueueType,
		    testLocation, testLocation, testLocation, 1, "Test comment");
		
		Assert.assertNotNull(queue);
		Assert.assertNotNull(queue.getNonPatientQueueId());
		Assert.assertEquals("John Visitor", queue.getDisplayName());
		Assert.assertEquals("+256123456789", queue.getPhoneNumber());
		Assert.assertEquals(testQueueType, queue.getQueueType());
		Assert.assertEquals(NonPatientQueue.NonPatientQueueStatus.WAITING, queue.getStatus());
		Assert.assertEquals(Integer.valueOf(1), queue.getPriority());
		Assert.assertEquals("Test comment", queue.getComment());
		Assert.assertNotNull(queue.getTicketNumber());
	}
	
	@Test
	public void shouldGenerateUniqueTicketNumber() {
		// Create first queue
		NonPatientQueue queue1 = service.createNonPatientQueueEntry("Test 1", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		
		// Verify first queue was created and has a ticket number
		Assert.assertNotNull(queue1);
		Assert.assertNotNull(queue1.getTicketNumber());
		
		// Verify we can find the first queue by ticket number
		Date today = new Date();
		List<NonPatientQueue> results = service.getNonPatientQueueByTicketNumber(queue1.getTicketNumber(),
		    org.openmrs.util.OpenmrsUtil.firstSecondOfDay(today), org.openmrs.util.OpenmrsUtil.getLastMomentOfDay(today));
		Assert.assertFalse("Should find queue by ticket number", results.isEmpty());
		Assert.assertEquals(queue1.getTicketNumber(), results.get(0).getTicketNumber());
	}
	
	@Test
	public void shouldGetQueueEntryByUuid() {
		NonPatientQueue created = service.createNonPatientQueueEntry("Jane Visitor", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		
		NonPatientQueue retrieved = service.getNonPatientQueueByUuid(created.getUuid());
		
		Assert.assertNotNull(retrieved);
		Assert.assertEquals(created.getUuid(), retrieved.getUuid());
		Assert.assertEquals("Jane Visitor", retrieved.getDisplayName());
	}
	
	@Test
	public void shouldGetQueueEntryById() {
		NonPatientQueue created = service.createNonPatientQueueEntry("Test Visitor", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		
		NonPatientQueue retrieved = service.getNonPatientQueueById(created.getNonPatientQueueId());
		
		Assert.assertNotNull(retrieved);
		Assert.assertEquals(created.getNonPatientQueueId(), retrieved.getNonPatientQueueId());
	}
	
	@Test
	public void shouldGetQueueEntriesByTicketNumber() {
		NonPatientQueue queue = service.createNonPatientQueueEntry("Ticket Test", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		
		Date today = new Date();
		List<NonPatientQueue> results = service.getNonPatientQueueByTicketNumber(queue.getTicketNumber(),
		    org.openmrs.util.OpenmrsUtil.firstSecondOfDay(today), org.openmrs.util.OpenmrsUtil.getLastMomentOfDay(today));
		
		Assert.assertFalse(results.isEmpty());
		Assert.assertEquals(queue.getTicketNumber(), results.get(0).getTicketNumber());
	}
	
	@Test
	public void shouldCallQueueEntry() {
		NonPatientQueue queue = service.createNonPatientQueueEntry("Call Test", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		
		NonPatientQueue called = service.callNonPatientQueue(queue, testProvider);
		
		Assert.assertEquals(NonPatientQueue.NonPatientQueueStatus.CALLED, called.getStatus());
		Assert.assertEquals(testProvider, called.getCalledBy());
		Assert.assertNotNull(called.getCalledAt());
	}
	
	@Test
	public void shouldMarkQueueEntryAsArrived() {
		NonPatientQueue queue = service.createNonPatientQueueEntry("Arrive Test", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		queue = service.callNonPatientQueue(queue, testProvider);
		
		NonPatientQueue arrived = service.markNonPatientQueueArrived(queue);
		
		Assert.assertEquals(NonPatientQueue.NonPatientQueueStatus.ARRIVED, arrived.getStatus());
		Assert.assertNotNull(arrived.getArrivedAt());
	}
	
	@Test
	public void shouldStartServingQueueEntry() {
		NonPatientQueue queue = service.createNonPatientQueueEntry("Serve Test", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		queue = service.callNonPatientQueue(queue, testProvider);
		queue = service.markNonPatientQueueArrived(queue);
		
		NonPatientQueue serving = service.startServingNonPatientQueue(queue, testProvider);
		
		Assert.assertEquals(NonPatientQueue.NonPatientQueueStatus.SERVING, serving.getStatus());
		Assert.assertEquals(testProvider, serving.getServedBy());
		Assert.assertNotNull(serving.getStartedAt());
	}
	
	@Test
	public void shouldCompleteQueueEntry() {
		NonPatientQueue queue = service.createNonPatientQueueEntry("Complete Test", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		queue = service.callNonPatientQueue(queue, testProvider);
		queue = service.markNonPatientQueueArrived(queue);
		queue = service.startServingNonPatientQueue(queue, testProvider);
		
		NonPatientQueue completed = service.completeNonPatientQueue(queue, testProvider);
		
		Assert.assertEquals(NonPatientQueue.NonPatientQueueStatus.COMPLETED, completed.getStatus());
		Assert.assertNotNull(completed.getEndedAt());
	}
	
	@Test(expected = org.openmrs.api.APIException.class)
	public void shouldFailToCallNonWaitingQueueEntry() {
		NonPatientQueue queue = service.createNonPatientQueueEntry("Fail Test", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		queue.setStatus(NonPatientQueue.NonPatientQueueStatus.CALLED);
		
		service.callNonPatientQueue(queue, testProvider);
	}
	
	@Test
	public void shouldSkipQueueEntry() {
		NonPatientQueue queue = service.createNonPatientQueueEntry("Skip Test", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		queue = service.callNonPatientQueue(queue, testProvider);
		
		NonPatientQueue skipped = service.skipNonPatientQueue(queue);
		
		Assert.assertEquals(NonPatientQueue.NonPatientQueueStatus.WAITING, skipped.getStatus());
		Assert.assertNull(skipped.getCalledBy());
		Assert.assertNull(skipped.getCalledAt());
	}
	
	@Test
	public void shouldCancelQueueEntry() {
		NonPatientQueue queue = service.createNonPatientQueueEntry("Cancel Test", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		
		NonPatientQueue cancelled = service.cancelNonPatientQueue(queue);
		
		Assert.assertEquals(NonPatientQueue.NonPatientQueueStatus.CANCELLED, cancelled.getStatus());
		Assert.assertNotNull(cancelled.getEndedAt());
	}
	
	@Test
	public void shouldGetAllActiveQueueEntries() {
		service.createNonPatientQueueEntry("Active 1", null, testQueueType, testLocation, testLocation, testLocation, null,
		    null);
		service.createNonPatientQueueEntry("Active 2", null, testQueueType, testLocation, testLocation, testLocation, null,
		    null);
		NonPatientQueue completed = service.createNonPatientQueueEntry("Completed", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		completed = service.callNonPatientQueue(completed, testProvider);
		completed = service.markNonPatientQueueArrived(completed);
		completed = service.startServingNonPatientQueue(completed, testProvider);
		service.completeNonPatientQueue(completed, testProvider);
		
		List<NonPatientQueue> activeQueues = service.getAllActiveNonPatientQueues();
		
		Assert.assertTrue(activeQueues.size() >= 2);
		for (NonPatientQueue queue : activeQueues) {
			Assert.assertNotEquals(NonPatientQueue.NonPatientQueueStatus.COMPLETED, queue.getStatus());
		}
	}
	
	@Test
	public void shouldGetQueueEntriesByStatus() {
		service.createNonPatientQueueEntry("Waiting 1", null, testQueueType, testLocation, testLocation, testLocation, null,
		    null);
		service.createNonPatientQueueEntry("Waiting 2", null, testQueueType, testLocation, testLocation, testLocation, null,
		    null);
		NonPatientQueue called = service.createNonPatientQueueEntry("Called", null, testQueueType, testLocation,
		    testLocation, testLocation, null, null);
		service.callNonPatientQueue(called, testProvider);
		
		List<NonPatientQueue> waitingQueues = service.getNonPatientQueues(NonPatientQueue.NonPatientQueueStatus.WAITING,
		    null, null, null, null, null);
		
		Assert.assertTrue(waitingQueues.size() >= 2);
		for (NonPatientQueue queue : waitingQueues) {
			Assert.assertEquals(NonPatientQueue.NonPatientQueueStatus.WAITING, queue.getStatus());
		}
	}
}
