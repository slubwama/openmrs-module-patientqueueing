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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.patientqueueing.Configuration;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.api.ProviderAssignmentService;
import org.openmrs.module.patientqueueing.api.providerAssignment.LeastBusyProviderStrategy;
import org.openmrs.module.patientqueueing.api.providerAssignment.ProviderAssignmentStrategy;
import org.openmrs.module.patientqueueing.api.providerAssignment.RandomProviderStrategy;
import org.openmrs.module.patientqueueing.api.providerAssignment.RoundRobinProviderStrategy;
import org.openmrs.module.patientqueueing.model.PatientQueue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderAssignmentServiceImpl extends BaseOpenmrsService implements ProviderAssignmentService {
	
	private static final Log log = LogFactory.getLog(ProviderAssignmentServiceImpl.class);
	
	private ProviderAssignmentStrategy strategy;
	
	@Override
	public Provider assignProvider(Location location) {
		List<Provider> providers = getProvidersForLocation(location);
		
		if (providers.isEmpty()) {
			throw new APIException("No providers available for location: " + location.getName());
		}
		
		return assignProvider(location, providers);
	}
	
	@Override
	public Provider assignProvider(Location location, List<Provider> availableProviders) {
		if (availableProviders == null || availableProviders.isEmpty()) {
			throw new APIException("No providers available for assignment");
		}
		
		ProviderAssignmentStrategy currentStrategy = getProviderAssignmentStrategy();
		
		if (currentStrategy == null) {
			log.warn("No provider assignment strategy configured, using least busy strategy");
			currentStrategy = new LeastBusyProviderStrategy();
		}
		
		Provider provider = currentStrategy.assignProvider(location, availableProviders);
		
		if (provider == null) {
			throw new APIException("Provider assignment strategy returned null");
		}
		
		return provider;
	}
	
	@Override
	public ProviderAssignmentStrategy getProviderAssignmentStrategy() {
		if (strategy != null) {
			return strategy;
		}
		
		AdministrationService adminService = Context.getAdministrationService();
		String strategyName = adminService.getGlobalProperty(Configuration.GP_PROVIDER_ASSIGNMENT_STRATEGY,
		    Configuration.DEFAULT_PROVIDER_ASSIGNMENT_STRATEGY);
		
		strategy = createStrategy(strategyName);
		
		return strategy;
	}
	
	@Override
	public void setProviderAssignmentStrategy(ProviderAssignmentStrategy strategy) {
		this.strategy = strategy;
	}
	
	@Override
	public List<Provider> getProvidersForLocation(Location location) {
		List<Provider> providers = new ArrayList<>();

		for (Provider provider : Context.getProviderService().getAllProviders(false)) {
			if (provider.getPerson() != null && !provider.getRetired()) {
				// Check if provider is assigned to this location
				// This is a simplified check - in real implementation, you'd have
				// a proper provider-location mapping
				providers.add(provider);
			}
		}

		return providers;
	}
	
	@Override
	public Map<Provider, Integer> getProviderQueueCounts(Location location) {
		PatientQueueingService queueingService = Context.getService(PatientQueueingService.class);
		Map<Provider, Integer> counts = new HashMap<>();

		Date today = new Date();
		List<Provider> providers = getProvidersForLocation(location);

		for (Provider provider : providers) {
			List<PatientQueue> queues = queueingService.getPatientQueueList(provider,
			    org.openmrs.util.OpenmrsUtil.firstSecondOfDay(today),
			    org.openmrs.util.OpenmrsUtil.getLastMomentOfDay(today), location, null, null, null, null);

			int activeCount = 0;
			for (PatientQueue queue : queues) {
				if (queue.getStatus() != PatientQueue.Status.COMPLETED) {
					activeCount++;
				}
			}

			counts.put(provider, activeCount);
		}

		return counts;
	}
	
	private ProviderAssignmentStrategy createStrategy(String strategyName) {
		if (strategyName == null || strategyName.trim().isEmpty()) {
			log.warn("Empty strategy name, using least busy strategy");
			return new LeastBusyProviderStrategy();
		}
		
		switch (strategyName.trim()) {
			case Configuration.ProviderAssignmentStrategy.LEAST_BUSY:
				return new LeastBusyProviderStrategy();
			case Configuration.ProviderAssignmentStrategy.ROUND_ROBIN:
				return new RoundRobinProviderStrategy();
			case Configuration.ProviderAssignmentStrategy.RANDOM:
				return new RandomProviderStrategy();
			case Configuration.ProviderAssignmentStrategy.MANUAL:
				log.warn("Manual provider assignment strategy selected - will return null");
				return null;
			default:
				log.warn("Unknown strategy: " + strategyName + ", using least busy strategy");
				return new LeastBusyProviderStrategy();
		}
	}
}
