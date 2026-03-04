package org.openmrs.module.patientqueueing.model;

import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Location;
import org.openmrs.Encounter;
import org.openmrs.BaseOpenmrsData;

import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "patientqueueing.PatientQueue")
@Table(name = "patient_queue")
public class PatientQueue extends BaseOpenmrsData implements Serializable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "patient_queue_id")
	private Integer patientQueueId;
	
	@ManyToOne
	@JoinColumn(name = "patient_id")
	private Patient patient;
	
	@ManyToOne
	@JoinColumn(name = "provider_id")
	private Provider provider;
	
	@ManyToOne
	@JoinColumn(name = "location_from")
	private Location locationFrom;
	
	@ManyToOne
	@JoinColumn(name = "location_to")
	private Location locationTo;
	
	@ManyToOne
	@JoinColumn(name = "encounter_id")
	private Encounter encounter;
	
	@Column(name = "status", length = 255)
	@Enumerated(EnumType.STRING)
	private Status status;
	
	@Column(name = "visit_number", length = 255)
	private String visitNumber;
	
	@Column(name = "priority")
	private Integer priority;
	
	@Column(name = "priority_comment", length = 255)
	private String priorityComment;
	
	@Column(name = "comment", length = 255)
	private String comment;
	
	@ManyToOne
	@JoinColumn(name = "queue_room")
	private Location queueRoom;
	
	/**
	 * Root facility location for fast filtering and reporting (denormalized from the location
	 * hierarchy).
	 */
	@ManyToOne
	@JoinColumn(name = "facility_location")
	private Location facilityLocation;
	
	/**
	 * Ticket number that is safe for patient-facing displays (e.g. OPD-034). For backward
	 * compatibility this may match visitNumber.
	 */
	@Column(name = "ticket_number", length = 50)
	private String ticketNumber;
	
	/**
	 * Date-part of the queue (facility timezone). Used for "today" views.
	 */
	@Column(name = "queue_date")
	private java.sql.Date queueDate;
	
	/**
	 * A computed priority score used for ordering (higher means sooner).
	 */
	@Column(name = "priority_score")
	private Integer priorityScore;
	
	@Column(name = "priority_reason", length = 255)
	private String priorityReason;
	
	@Column(name = "checked_in_at")
	private Date checkedInAt;
	
	@Column(name = "called_at")
	private Date calledAt;
	
	@Column(name = "started_at")
	private Date startedAt;
	
	@Column(name = "ended_at")
	private Date endedAt;
	
	/**
	 * Date when a provider selects a picks or selects from the queue
	 */
	@Column(name = "date_picked")
	private Date datePicked;
	
	/**
	 * Date when the patient queue is completed
	 */
	@Column(name = "date_completed")
	private Date dateCompleted;
	
	public PatientQueue() {
	}
	
	public enum Status {
		// Legacy states (kept for backward compatibility with existing deployments/UIs)
		PENDING, PICKED,
		
		// Preferred states for richer queue workflows (displays, kiosks, no-shows)
		WAITING, PRESENT, CALLED, IN_SERVICE, COMPLETED, NO_SHOW, SKIPPED, CANCELLED;
	}
	
	/**
	 * Convenience alias for department/service queue location. In this module, locationTo
	 * historically represents the queue destination.
	 */
	public Location getQueueLocation() {
		return getLocationTo();
	}
	
	/**
	 * Convenience alias for service point location (room/counter). In this module, queueRoom
	 * historically represents the service location.
	 */
	public Location getServiceLocation() {
		return getQueueRoom();
	}
	
	public Integer getId() {
		return patientQueueId;
	}
	
	public void setId(Integer integer) {
		patientQueueId = integer;
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
	
	public Location getLocationFrom() {
		return locationFrom;
	}
	
	public void setLocationFrom(Location locationFrom) {
		this.locationFrom = locationFrom;
	}
	
	public Location getLocationTo() {
		return locationTo;
	}
	
	public void setLocationTo(Location locationTo) {
		this.locationTo = locationTo;
	}
	
	public Encounter getEncounter() {
		return encounter;
	}
	
	public void setEncounter(Encounter encounter) {
		this.encounter = encounter;
	}
	
	public Integer getPatientQueueId() {
		return patientQueueId;
	}
	
	public void setPatientQueueId(Integer patientQueueId) {
		this.patientQueueId = patientQueueId;
	}
	
	public Integer getPriority() {
		return priority;
	}
	
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	
	public String getPriorityComment() {
		return priorityComment;
	}
	
	public void setPriorityComment(String priorityComment) {
		this.priorityComment = priorityComment;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public String getVisitNumber() {
		return visitNumber;
	}
	
	public void setVisitNumber(String visitNumber) {
		this.visitNumber = visitNumber;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public Location getQueueRoom() {
		return queueRoom;
	}
	
	public void setQueueRoom(Location queueRoom) {
		this.queueRoom = queueRoom;
	}
	
	public Location getFacilityLocation() {
		return facilityLocation;
	}
	
	public void setFacilityLocation(Location facilityLocation) {
		this.facilityLocation = facilityLocation;
	}
	
	public String getTicketNumber() {
		return ticketNumber;
	}
	
	public void setTicketNumber(String ticketNumber) {
		this.ticketNumber = ticketNumber;
	}
	
	public java.sql.Date getQueueDate() {
		return queueDate;
	}
	
	public void setQueueDate(java.sql.Date queueDate) {
		this.queueDate = queueDate;
	}
	
	public Integer getPriorityScore() {
		return priorityScore;
	}
	
	public void setPriorityScore(Integer priorityScore) {
		this.priorityScore = priorityScore;
	}
	
	public String getPriorityReason() {
		return priorityReason;
	}
	
	public void setPriorityReason(String priorityReason) {
		this.priorityReason = priorityReason;
	}
	
	public Date getCheckedInAt() {
		return checkedInAt;
	}
	
	public void setCheckedInAt(Date checkedInAt) {
		this.checkedInAt = checkedInAt;
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
	
	public Date getEndedAt() {
		return endedAt;
	}
	
	public void setEndedAt(Date endedAt) {
		this.endedAt = endedAt;
	}
	
	public Date getDatePicked() {
		return datePicked;
	}
	
	public void setDatePicked(Date datePicked) {
		this.datePicked = datePicked;
	}
	
	public Date getDateCompleted() {
		return dateCompleted;
	}
	
	public void setDateCompleted(Date dateCompleted) {
		this.dateCompleted = dateCompleted;
	}
}
