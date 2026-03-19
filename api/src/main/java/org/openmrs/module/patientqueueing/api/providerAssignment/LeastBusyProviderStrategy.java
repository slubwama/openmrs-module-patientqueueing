/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientqueueing.api.providerAssignment;

import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.model.PatientQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Assigns the provider with the fewest active patient queues
 */
public class LeastBusyProviderStrategy implements ProviderAssignmentStrategy {
	
	private final PatientQueueingService queueingService;
	
	public LeastBusyProviderStrategy() {
		this.queueingService = Context.getService(PatientQueueingService.class);
	}
	
	@Override
	public Provider assignProvider(Location location, List<Provider> availableProviders) {
		if (availableProviders == null || availableProviders.isEmpty()) {
			return null;
		}
		
		if (availableProviders.size() == 1) {
			return availableProviders.get(0);
		}
		
		// Sort providers by their current queue count (ascending)
		final Location finalLocation = location;
		List<Provider> sortedProviders = new ArrayList<Provider>(availableProviders);
		Collections.sort(sortedProviders, new Comparator<Provider>() {
			
			@Override
			public int compare(Provider p1, Provider p2) {
				int count1 = getActiveQueueCount(finalLocation, p1);
				int count2 = getActiveQueueCount(finalLocation, p2);
				return Integer.compare(count1, count2);
			}
		});
		
		return sortedProviders.get(0);
	}
	
	@Override
	public String getName() {
		return "leastBusy";
	}
	
	/**
	 * Get the count of active queues for a provider at a location
	 * 
	 * @param location the location
	 * @param provider the provider
	 * @return the number of active queues
	 */
	private int getActiveQueueCount(Location location, Provider provider) {
		Date today = new Date();
		List<PatientQueue> queues = queueingService.getPatientQueueList(provider,
		    org.openmrs.util.OpenmrsUtil.firstSecondOfDay(today), org.openmrs.util.OpenmrsUtil.getLastMomentOfDay(today),
		    location, null, null, null, null);
		
		int count = 0;
		for (PatientQueue queue : queues) {
			if (queue.getStatus() != PatientQueue.Status.COMPLETED) {
				count++;
			}
		}
		
		return count;
	}
}
