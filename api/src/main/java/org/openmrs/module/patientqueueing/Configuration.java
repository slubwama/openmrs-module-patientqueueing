/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientqueueing;

/**
 * Configuration constants and global property names for the patient queueing module
 */
public final class Configuration {
	
	private Configuration() {
	}
	
	// Global Property Names
	public static final String GP_PROVIDER_ASSIGNMENT_STRATEGY = "patientqueueing.providerAssignmentStrategy";
	
	public static final String GP_AUTO_CREATE_VISIT = "patientqueueing.autoCreateVisit";
	
	public static final String GP_DEFAULT_VISIT_TYPE = "patientqueueing.defaultVisitTypeUuid";
	
	public static final String GP_DEFAULT_PRIORITY = "patientqueueing.defaultPriority";
	
	// Patient Identifier Configuration
	public static final String GP_PATIENT_IDENTIFIER_TYPES = "patientqueueing.patientIdentifierTypes";
	
	public static final String GP_PHONE_NUMBER_ATTRIBUTE_TYPE = "patientqueueing.phoneNumberAttributeTypeUuid";
	
	// Queue Type Configuration
	public static final String GP_DEFAULT_QUEUE_TYPE_CONCEPT = "patientqueueing.defaultQueueTypeConceptUuid";
	
	public static final String GP_NON_PATIENT_QUEUE_TYPE_CONCEPTS = "patientqueueing.nonPatientQueueTypeConceptUuids";
	
	// Default Values
	public static final String DEFAULT_PROVIDER_ASSIGNMENT_STRATEGY = "leastBusy";
	
	public static final boolean DEFAULT_AUTO_CREATE_VISIT = true;
	
	public static final int DEFAULT_PRIORITY = 5;
	
	// Provider Assignment Strategy Options
	public interface ProviderAssignmentStrategy {
		
		String LEAST_BUSY = "leastBusy";
		
		String ROUND_ROBIN = "roundRobin";
		
		String RANDOM = "random";
		
		String MANUAL = "manual";
	}
	
	/**
	 * Validates if a provider assignment strategy is valid
	 * 
	 * @param strategy the strategy to validate
	 * @return true if the strategy is valid, false otherwise
	 */
	public static boolean isValidProviderAssignmentStrategy(String strategy) {
		return ProviderAssignmentStrategy.LEAST_BUSY.equals(strategy)
		        || ProviderAssignmentStrategy.ROUND_ROBIN.equals(strategy)
		        || ProviderAssignmentStrategy.RANDOM.equals(strategy) || ProviderAssignmentStrategy.MANUAL.equals(strategy);
	}
}
