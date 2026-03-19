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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assigns providers in a round-robin fashion
 */
public class RoundRobinProviderStrategy implements ProviderAssignmentStrategy {

	private final Map<String, RoundRobinState> stateMap = new HashMap<>();

	@Override
	public Provider assignProvider(Location location, List<Provider> availableProviders) {
		if (availableProviders == null || availableProviders.isEmpty()) {
			return null;
		}

		if (availableProviders.size() == 1) {
			return availableProviders.get(0);
		}

		String locationKey = location.getUuid();

		RoundRobinState state = stateMap.get(locationKey);
		if (state == null || state.getProviderList() != availableProviders) {
			state = new RoundRobinState(availableProviders);
			stateMap.put(locationKey, state);
		}

		Provider provider = state.getNext();
		state.incrementIndex();

		return provider;
	}

	@Override
	public String getName() {
		return "roundRobin";
	}

	private static class RoundRobinState {

		private final List<Provider> providerList;

		private int currentIndex = 0;

		public RoundRobinState(List<Provider> providerList) {
			this.providerList = providerList;
		}

		public Provider getNext() {
			return providerList.get(currentIndex);
		}

		public void incrementIndex() {
			currentIndex = (currentIndex + 1) % providerList.size();
		}

		public List<Provider> getProviderList() {
			return providerList;
		}
	}
}
