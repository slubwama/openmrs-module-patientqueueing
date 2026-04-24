package org.openmrs.module.patientqueueing.customdto;

import java.util.Date;

/**
 * DTO for provider queue entry response.
 */
public class ProviderQueueEntry {
	
	private String uuid;
	
	private String queueNumber;
	
	private String ticketNumber;
	
	private String displayName;
	
	private String identifiedBy;
	
	private String currentLocation;
	
	private String serviceLocation;
	
	private String status;
	
	private String waitTime;
	
	private Boolean patient;
	
	private String sourceType;
	
	private String locationToUuid;
	
	private String queueRoomUuid;
	
	private Date dateCreated;
	
	private String comment;
	
	private Integer priority;
	
	private String action;
	
	public ProviderQueueEntry() {
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getQueueNumber() {
		return queueNumber;
	}
	
	public void setQueueNumber(String queueNumber) {
		this.queueNumber = queueNumber;
	}
	
	public String getTicketNumber() {
		return ticketNumber;
	}
	
	public void setTicketNumber(String ticketNumber) {
		this.ticketNumber = ticketNumber;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public String getIdentifiedBy() {
		return identifiedBy;
	}
	
	public void setIdentifiedBy(String identifiedBy) {
		this.identifiedBy = identifiedBy;
	}
	
	public String getCurrentLocation() {
		return currentLocation;
	}
	
	public void setCurrentLocation(String currentLocation) {
		this.currentLocation = currentLocation;
	}
	
	public String getServiceLocation() {
		return serviceLocation;
	}
	
	public void setServiceLocation(String serviceLocation) {
		this.serviceLocation = serviceLocation;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getWaitTime() {
		return waitTime;
	}
	
	public void setWaitTime(String waitTime) {
		this.waitTime = waitTime;
	}
	
	public Boolean getPatient() {
		return patient;
	}
	
	public void setPatient(Boolean patient) {
		this.patient = patient;
	}
	
	public String getSourceType() {
		return sourceType;
	}
	
	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}
	
	public String getLocationToUuid() {
		return locationToUuid;
	}
	
	public void setLocationToUuid(String locationToUuid) {
		this.locationToUuid = locationToUuid;
	}
	
	public String getQueueRoomUuid() {
		return queueRoomUuid;
	}
	
	public void setQueueRoomUuid(String queueRoomUuid) {
		this.queueRoomUuid = queueRoomUuid;
	}
	
	public Date getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public Integer getPriority() {
		return priority;
	}
	
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	
	public String getAction() {
		return action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}
}
