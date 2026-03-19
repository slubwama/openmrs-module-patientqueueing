/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientqueueing.model;

import org.junit.Assert;
import org.junit.Test;

public class NonPatientQueueTest {
	
	@Test
	public void shouldCreateNonPatientQueueWithDefaultValues() {
		NonPatientQueue queue = new NonPatientQueue();
		
		Assert.assertNull(queue.getTicketNumber());
		Assert.assertNull(queue.getDisplayName());
		Assert.assertNull(queue.getPhoneNumber());
		Assert.assertNull(queue.getQueueType());
		Assert.assertNull(queue.getStatus());
		Assert.assertNull(queue.getCurrentLocation());
		Assert.assertNull(queue.getLocationTo());
		Assert.assertNull(queue.getQueueRoom());
		Assert.assertNull(queue.getCalledBy());
		Assert.assertNull(queue.getServedBy());
		Assert.assertNull(queue.getPriority());
		Assert.assertNull(queue.getComment());
	}
	
	@Test
	public void shouldSetAndGetAllFields() {
		NonPatientQueue queue = new NonPatientQueue();
		queue.setTicketNumber("TICKET-001");
		queue.setDisplayName("John Visitor");
		queue.setPhoneNumber("+256123456789");
		queue.setStatus(NonPatientQueue.NonPatientQueueStatus.WAITING);
		queue.setPriority(1);
		queue.setComment("VIP visitor");
		
		Assert.assertEquals("TICKET-001", queue.getTicketNumber());
		Assert.assertEquals("John Visitor", queue.getDisplayName());
		Assert.assertEquals("+256123456789", queue.getPhoneNumber());
		Assert.assertEquals(NonPatientQueue.NonPatientQueueStatus.WAITING, queue.getStatus());
		Assert.assertEquals(Integer.valueOf(1), queue.getPriority());
		Assert.assertEquals("VIP visitor", queue.getComment());
	}
	
	@Test
	public void shouldHaveAllStatusValues() {
		NonPatientQueue.NonPatientQueueStatus[] statuses = NonPatientQueue.NonPatientQueueStatus.values();
		
		Assert.assertEquals(7, statuses.length);
		Assert.assertTrue(java.util.Arrays.asList(statuses).contains(NonPatientQueue.NonPatientQueueStatus.WAITING));
		Assert.assertTrue(java.util.Arrays.asList(statuses).contains(NonPatientQueue.NonPatientQueueStatus.CALLED));
		Assert.assertTrue(java.util.Arrays.asList(statuses).contains(NonPatientQueue.NonPatientQueueStatus.ARRIVED));
		Assert.assertTrue(java.util.Arrays.asList(statuses).contains(NonPatientQueue.NonPatientQueueStatus.SERVING));
		Assert.assertTrue(java.util.Arrays.asList(statuses).contains(NonPatientQueue.NonPatientQueueStatus.COMPLETED));
		Assert.assertTrue(java.util.Arrays.asList(statuses).contains(NonPatientQueue.NonPatientQueueStatus.SKIPPED));
		Assert.assertTrue(java.util.Arrays.asList(statuses).contains(NonPatientQueue.NonPatientQueueStatus.CANCELLED));
	}
	
	@Test
	public void shouldUseIdAsPrimaryKey() {
		NonPatientQueue queue = new NonPatientQueue();
		queue.setId(123);
		
		Assert.assertEquals(Integer.valueOf(123), queue.getId());
		Assert.assertEquals(Integer.valueOf(123), queue.getNonPatientQueueId());
	}
	
	@Test
	public void shouldSetIdAndNonPatientQueueIdConsistently() {
		NonPatientQueue queue = new NonPatientQueue();
		queue.setId(456);
		
		Assert.assertEquals(Integer.valueOf(456), queue.getNonPatientQueueId());
		
		queue.setNonPatientQueueId(789);
		Assert.assertEquals(Integer.valueOf(789), queue.getId());
	}
}
