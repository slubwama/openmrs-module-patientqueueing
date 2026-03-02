package org.openmrs.module.patientqueueing.model;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Location;
import org.openmrs.User;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "patientqueueing.PatientQueueEvent")
@Table(name = "patient_queue_event")
public class PatientQueueEvent extends BaseOpenmrsData implements Serializable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "patient_queue_event_id")
	private Integer patientQueueEventId;
	
	@ManyToOne
	@JoinColumn(name = "patient_queue_id", nullable = false)
	private PatientQueue patientQueue;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", length = 50, nullable = false)
	private EventType eventType;
	
	@ManyToOne
	@JoinColumn(name = "from_queue_location")
	private Location fromQueueLocation;
	
	@ManyToOne
	@JoinColumn(name = "to_queue_location")
	private Location toQueueLocation;
	
	@ManyToOne
	@JoinColumn(name = "from_service_location")
	private Location fromServiceLocation;
	
	@ManyToOne
	@JoinColumn(name = "to_service_location")
	private Location toServiceLocation;
	
	@ManyToOne
	@JoinColumn(name = "actor_user")
	private User actorUser;
	
	@Column(name = "actor_device_id", length = 100)
	private String actorDeviceId;
	
	@Column(name = "event_time", nullable = false)
	private Date eventTime;
	
	@Column(name = "details", length = 2000)
	private String details;
	
	public enum EventType {
		CREATED, CHECKED_IN, CALLED, STARTED, COMPLETED, TRANSFERRED, NO_SHOW, SKIPPED, CANCELLED, UPDATED
	}
	
	@Override
	public Integer getId() {
		return patientQueueEventId;
	}
	
	@Override
	public void setId(Integer id) {
		this.patientQueueEventId = id;
	}
	
	public PatientQueue getPatientQueue() {
		return patientQueue;
	}
	
	public void setPatientQueue(PatientQueue patientQueue) {
		this.patientQueue = patientQueue;
	}
	
	public EventType getEventType() {
		return eventType;
	}
	
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}
	
	public Location getFromQueueLocation() {
		return fromQueueLocation;
	}
	
	public void setFromQueueLocation(Location fromQueueLocation) {
		this.fromQueueLocation = fromQueueLocation;
	}
	
	public Location getToQueueLocation() {
		return toQueueLocation;
	}
	
	public void setToQueueLocation(Location toQueueLocation) {
		this.toQueueLocation = toQueueLocation;
	}
	
	public Location getFromServiceLocation() {
		return fromServiceLocation;
	}
	
	public void setFromServiceLocation(Location fromServiceLocation) {
		this.fromServiceLocation = fromServiceLocation;
	}
	
	public Location getToServiceLocation() {
		return toServiceLocation;
	}
	
	public void setToServiceLocation(Location toServiceLocation) {
		this.toServiceLocation = toServiceLocation;
	}
	
	public User getActorUser() {
		return actorUser;
	}
	
	public void setActorUser(User actorUser) {
		this.actorUser = actorUser;
	}
	
	public String getActorDeviceId() {
		return actorDeviceId;
	}
	
	public void setActorDeviceId(String actorDeviceId) {
		this.actorDeviceId = actorDeviceId;
	}
	
	public Date getEventTime() {
		return eventTime;
	}
	
	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
	}
	
	public String getDetails() {
		return details;
	}
	
	public void setDetails(String details) {
		this.details = details;
	}
}
