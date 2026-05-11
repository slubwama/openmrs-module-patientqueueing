package org.openmrs.module.patientqueueing;

/**
 * Global property names used throughout the Patient Queueing module
 */
public class PatientQueueingConstants {
	
	public static final String GP_SELF_CHECK_IN_PERSON_ATTRIBUTE_TYPE_UUIDS = "patientqueueing.selfCheckInPersonAttributeTypeUuids";
	
	public static final String GP_SELF_CHECK_IN_IDENTIFIER_TYPE_UUIDS = "patientqueueing.selfCheckInIdentifierTypeUuids";
	
	public static final String GP_SELF_CHECK_IN_VISIT_TYPE_UUID = "patientqueueing.selfCheckInVisitTypeUuid";
	
	public static final String GP_KIOSK_LOCATION_TAG_UUID = "patientqueueing.kioskLocationTagUuid";
	
	public static final String GP_RECEPTION_LOCATION_TAG_UUID = "patientqueueing.receptionLocationTagUuid";
	
	public static final String DEFAULT_PHONE_ATTRIBUTE_TYPE_UUID = "14d4f066-15f5-102d-96e4-000c29c2a5d7"; // Telephone Number
	
	public static final String DEFAULT_PATIENT_ID_IDENTIFIER_TYPE_UUID = "e1731641-30ab-102d-86b0-7a5022ba4115"; // Patient ID
	
	public static final String DEFAULT_NATIONAL_ID_IDENTIFIER_TYPE_UUID = "f0c16a6d-dc5f-4118-a803-616d0075d282"; // National ID
	
	private PatientQueueingConstants() {
		// Utility class
	}
}
