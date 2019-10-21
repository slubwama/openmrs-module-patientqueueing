package org.openmrs.module.patientqueueing.mapper;

import java.io.Serializable;

public class PatientQueueMapper implements Serializable {
	
	private Integer patientQueueId;
	
	private Integer patientId;
	
	private String patientNames;
	
	private String age;
	
	private String gender;
	
	private String providerNames;
	
	private String locationFrom;
	
	private String locationTo;
	
	private String status;
	
	private String dateCreated;
	
	private String creatorId;
	
	private String creatorNames;
	
	private String encounterId;
	
	private String queueNumber;
	
	private Integer priority;
	
	private String priorityComment;
	
	public PatientQueueMapper() {
	}
	
	public Integer getPatientQueueId() {
		return patientQueueId;
	}
	
	public void setPatientQueueId(Integer patientQueueId) {
		this.patientQueueId = patientQueueId;
	}
	
	public Integer getPatientId() {
		return patientId;
	}
	
	public void setPatientId(Integer patientId) {
		this.patientId = patientId;
	}
	
	public String getPatientNames() {
		return patientNames;
	}
	
	public void setPatientNames(String patientNames) {
		this.patientNames = patientNames;
	}
	
	public String getProviderNames() {
		return providerNames;
	}
	
	public void setProviderNames(String providerNames) {
		this.providerNames = providerNames;
	}
	
	public String getLocationFrom() {
		return locationFrom;
	}
	
	public void setLocationFrom(String locationFrom) {
		this.locationFrom = locationFrom;
	}
	
	public String getLocationTo() {
		return locationTo;
	}
	
	public void setLocationTo(String locationTo) {
		this.locationTo = locationTo;
	}
	
	public Integer getId() {
		return patientQueueId;
	}
	
	public void setId(Integer integer) {
		patientQueueId = integer;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getAge() {
		return age;
	}
	
	public void setAge(String age) {
		this.age = age;
	}
	
	public String getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(String dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	public String getCreatorId() {
		return creatorId;
	}
	
	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}
	
	public String getCreatorNames() {
		return creatorNames;
	}
	
	public void setCreatorNames(String creatorNames) {
		this.creatorNames = creatorNames;
	}
	
	public String getGender() {
		return gender;
	}
	
	public void setGender(String gender) {
		this.gender = gender;
	}
	
	public String getQueueNumber() {
		return queueNumber;
	}
	
	public void setQueueNumber(String queueNumber) {
		this.queueNumber = queueNumber;
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
	
	public String getEncounterId() {
		return encounterId;
	}





	
	public void setEncounterId(String encounterId) {
		this.encounterId = encounterId;
	}
}
