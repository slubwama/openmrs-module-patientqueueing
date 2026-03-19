package org.openmrs.module.patientqueueing.customdto;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;

import java.io.Serializable;
import java.util.Date;

/**
 * Unified DTO for queue entries that can represent either a PatientQueue or NonPatientQueue This is
 * used for kiosk displays and public queue displays where both types need to be shown together
 */
public class QueueEntry implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * The UUID of the queue entry Maps to: PatientQueue.uuid or NonPatientQueue.uuid
	 */
	private String uuid;
	
	/**
	 * The ticket number for this queue entry Maps to: PatientQueue.visitNumber or
	 * NonPatientQueue.ticketNumber
	 */
	private String ticketNumber;
	
	/**
	 * The status of this queue entry Maps to: PatientQueue.Status or
	 * NonPatientQueue.NonPatientQueueStatus
	 */
	private String status;
	
	/**
	 * The display name for this queue entry For PatientQueue: Patient name (given_name +
	 * family_name) For NonPatientQueue: displayName field
	 */
	private String displayName;
	
	/**
	 * The type of queue entry Possible values: "PATIENT" or "NON_PATIENT"
	 */
	private String queueType;
	
	/**
	 * The destination location Maps to: PatientQueue.locationTo or NonPatientQueue.locationTo
	 */
	private Location locationTo;
	
	/**
	 * The queue room where this entry is queued Maps to: PatientQueue.queueRoom or
	 * NonPatientQueue.queueRoom
	 */
	private Location queueRoom;
	
	/**
	 * The date when this entry was created Maps to: PatientQueue.dateCreated or
	 * NonPatientQueue.dateCreated
	 */
	private Date dateCreated;
	
	/**
	 * The priority of this queue entry Maps to: PatientQueue.priority or NonPatientQueue.priority
	 */
	private Integer priority;
	
	/**
	 * Any additional comments Maps to: PatientQueue.comment or NonPatientQueue.comment
	 */
	private String comment;
	
	/**
	 * The patient (only for PATIENT type) Maps to: PatientQueue.patient
	 */
	private Patient patient;
	
	/**
	 * The provider assigned to this entry Maps to: PatientQueue.provider or
	 * NonPatientQueue.servedBy/calledBy
	 */
	private Provider provider;
	
	/**
	 * The date when the entry was picked/called Maps to: PatientQueue.datePicked or
	 * NonPatientQueue.calledAt
	 */
	private Date calledAt;
	
	/**
	 * The date when service started Maps to: null or NonPatientQueue.startedAt
	 */
	private Date startedAt;
	
	/**
	 * The date when the entry was completed Maps to: PatientQueue.dateCompleted or
	 * NonPatientQueue.endedAt
	 */
	private Date completedAt;
	
	/**
	 * Whether this is a patient queue entry
	 */
	private Boolean isPatientQueue;
	
	// Empty constructor for JSON/serialization
	public QueueEntry() {
	}
	
	/**
	 * Create a QueueEntry from a PatientQueue
	 */
	public QueueEntry(org.openmrs.module.patientqueueing.model.PatientQueue patientQueue) {
		this.uuid = patientQueue.getUuid();
		this.ticketNumber = patientQueue.getVisitNumber();
		this.status = patientQueue.getStatus() != null ? patientQueue.getStatus().name() : null;
		this.displayName = formatPatientName(patientQueue.getPatient());
		this.queueType = "PATIENT";
		this.locationTo = patientQueue.getLocationTo();
		this.queueRoom = patientQueue.getQueueRoom();
		this.dateCreated = patientQueue.getDateCreated();
		this.priority = patientQueue.getPriority();
		this.comment = patientQueue.getComment();
		this.patient = patientQueue.getPatient();
		this.provider = patientQueue.getProvider();
		this.calledAt = patientQueue.getDatePicked();
		this.completedAt = patientQueue.getDateCompleted();
		this.isPatientQueue = true;
	}
	
	/**
	 * Create a QueueEntry from a NonPatientQueue
	 */
	public QueueEntry(org.openmrs.module.patientqueueing.model.NonPatientQueue nonPatientQueue) {
		this.uuid = nonPatientQueue.getUuid();
		this.ticketNumber = nonPatientQueue.getTicketNumber();
		this.status = nonPatientQueue.getStatus() != null ? nonPatientQueue.getStatus().name() : null;
		this.displayName = nonPatientQueue.getDisplayName();
		this.queueType = "NON_PATIENT";
		this.locationTo = nonPatientQueue.getLocationTo();
		this.queueRoom = nonPatientQueue.getQueueRoom();
		this.dateCreated = nonPatientQueue.getDateCreated();
		this.priority = nonPatientQueue.getPriority();
		this.comment = nonPatientQueue.getComment();
		this.provider = nonPatientQueue.getServedBy() != null ? nonPatientQueue.getServedBy() : nonPatientQueue
		        .getCalledBy();
		this.calledAt = nonPatientQueue.getCalledAt();
		this.startedAt = nonPatientQueue.getStartedAt();
		this.completedAt = nonPatientQueue.getEndedAt();
		this.isPatientQueue = false;
	}
	
	/**
	 * Format patient name as "GivenName FamilyName"
	 */
	private String formatPatientName(Patient patient) {
		if (patient == null || patient.getPersonName() == null) {
			return null;
		}
		return patient.getPersonName().getGivenName() + " " + patient.getPersonName().getFamilyName();
	}
	
	// Getters and Setters
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getTicketNumber() {
		return ticketNumber;
	}
	
	public void setTicketNumber(String ticketNumber) {
		this.ticketNumber = ticketNumber;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getQueueType() {
		return queueType;
	}
	
	public void setQueueType(String queueType) {
		this.queueType = queueType;
	}
	
	public Location getLocationTo() {
		return locationTo;
	}
	
	public void setLocationTo(Location locationTo) {
		this.locationTo = locationTo;
	}
	
	public Location getQueueRoom() {
		return queueRoom;
	}
	
	public void setQueueRoom(Location queueRoom) {
		this.queueRoom = queueRoom;
	}
	
	public Date getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	public Integer getPriority() {
		return priority;
	}
	
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public Patient getPatient() {
		return patient;
	}
	
	public void setPatient(Patient patient) {
		this.patient = patient;
	}
	
	public Provider getProvider() {
		return provider;
	}
	
	public void setProvider(Provider provider) {
		this.provider = provider;
	}
	
	public Date getCalledAt() {
		return calledAt;
	}
	
	public void setCalledAt(Date calledAt) {
		this.calledAt = calledAt;
	}
	
	public Date getStartedAt() {
		return startedAt;
	}
	
	public void setStartedAt(Date startedAt) {
		this.startedAt = startedAt;
	}
	
	public Date getCompletedAt() {
		return completedAt;
	}
	
	public void setCompletedAt(Date completedAt) {
		this.completedAt = completedAt;
	}
	
	public Boolean getIsPatientQueue() {
		return isPatientQueue;
	}
	
	public void setIsPatientQueue(Boolean isPatientQueue) {
		this.isPatientQueue = isPatientQueue;
	}
	
	/**
	 * Check if this is a patient queue entry
	 */
	public boolean isPatient() {
		return Boolean.TRUE.equals(isPatientQueue);
	}
	
	/**
	 * Check if this is a non-patient queue entry
	 */
	public boolean isNonPatient() {
		return Boolean.FALSE.equals(isPatientQueue);
	}
}
