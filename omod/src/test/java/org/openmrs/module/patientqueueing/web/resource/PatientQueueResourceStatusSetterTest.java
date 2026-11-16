/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientqueueing.web.resource;

import org.junit.Test;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the status guard on {@link PatientQueueResource} that keeps hold, no-show and transfer
 * moves (which carry transition rules and need manageQueue) on providerqueuebylocation.
 */
public class PatientQueueResourceStatusSetterTest {
	
	private PatientQueue entryWithStatus(PatientQueue.Status status) {
		PatientQueue patientQueue = new PatientQueue();
		patientQueue.setStatus(status);
		return patientQueue;
	}
	
	private void assertRefused(PatientQueue.Status from, PatientQueue.Status to) {
		try {
			PatientQueueResource.setStatus(entryWithStatus(from), to);
			fail("Expected moving from " + from + " to " + to + " to be refused");
		}
		catch (ResourceDoesNotSupportOperationException e) {
			// expected
		}
	}
	
	private void assertAllowed(PatientQueue.Status from, PatientQueue.Status to) {
		PatientQueue patientQueue = entryWithStatus(from);
		PatientQueueResource.setStatus(patientQueue, to);
		assertEquals(to, patientQueue.getStatus());
	}
	
	@Test
	public void shouldAllowCreatingWithQueueableStatuses() {
		assertAllowed(null, PatientQueue.Status.PENDING);
		assertAllowed(null, PatientQueue.Status.PICKED);
		assertAllowed(null, PatientQueue.Status.COMPLETED);
	}
	
	@Test
	public void shouldRefuseCreatingWithDashboardOnlyStatus() {
		assertRefused(null, PatientQueue.Status.ON_HOLD);
		assertRefused(null, PatientQueue.Status.NO_SHOW);
		assertRefused(null, PatientQueue.Status.TRANSFERRED);
	}
	
	@Test
	public void shouldAllowOrdinaryStatusEdits() {
		assertAllowed(PatientQueue.Status.PENDING, PatientQueue.Status.PICKED);
		assertAllowed(PatientQueue.Status.PICKED, PatientQueue.Status.COMPLETED);
		assertAllowed(PatientQueue.Status.COMPLETED, PatientQueue.Status.PENDING);
	}
	
	@Test
	public void shouldRefuseMovingIntoDashboardOnlyStatus() {
		assertRefused(PatientQueue.Status.PENDING, PatientQueue.Status.ON_HOLD);
		assertRefused(PatientQueue.Status.PENDING, PatientQueue.Status.NO_SHOW);
		assertRefused(PatientQueue.Status.PENDING, PatientQueue.Status.TRANSFERRED);
		assertRefused(PatientQueue.Status.PICKED, PatientQueue.Status.TRANSFERRED);
	}
	
	@Test
	public void shouldRefuseMovingOutOfDashboardOnlyStatus() {
		assertRefused(PatientQueue.Status.ON_HOLD, PatientQueue.Status.PENDING);
		assertRefused(PatientQueue.Status.ON_HOLD, PatientQueue.Status.PICKED);
		assertRefused(PatientQueue.Status.NO_SHOW, PatientQueue.Status.PENDING);
		assertRefused(PatientQueue.Status.TRANSFERRED, PatientQueue.Status.PENDING);
		assertRefused(PatientQueue.Status.TRANSFERRED, PatientQueue.Status.COMPLETED);
	}
	
	@Test
	public void shouldAllowNoOpStatusOnDashboardOnlyEntry() {
		assertAllowed(PatientQueue.Status.ON_HOLD, PatientQueue.Status.ON_HOLD);
	}
}
