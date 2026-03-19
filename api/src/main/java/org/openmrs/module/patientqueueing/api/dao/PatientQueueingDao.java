/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientqueueing.api.dao;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.patientqueueing.model.NonPatientQueue;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Repository("patientqueueing.PatientQueueingDao")
public class PatientQueueingDao {
	
	private static final Logger log = LoggerFactory.getLogger(PatientQueueingDao.class);
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	public DbSession getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueById(java.lang.Integer)
	 */
	public PatientQueue getPatientQueueById(Integer queueId) {
		return (PatientQueue) getSession().createCriteria(PatientQueue.class)
		        .add(Restrictions.eq("patientQueueId", queueId)).uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueByUuid(java.lang.String)
	 */
	public PatientQueue getPatientQueueByUUID(String uuid) {
		return (PatientQueue) getSession().createCriteria(PatientQueue.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueList(org.openmrs.Provider,
	 *      java.util.Date, java.util.Date, org.openmrs.Location, org.openmrs.Location,
	 *      org.openmrs.Patient, org.openmrs.module.patientqueueing.model.PatientQueue.Status)
	 */
	public List<PatientQueue> getPatientQueueList(Provider provider, Date fromDate, Date toDate, Location locationTo,
	        Location locationFrom, Patient patient, PatientQueue.Status status, Location queueRoom) {
		Criteria criteria = getSession().createCriteria(PatientQueue.class);
		
		if (fromDate != null && toDate != null) {
			criteria.add(Restrictions.between("dateCreated", fromDate, toDate));
		}
		
		if (provider != null) {
			criteria.add(Restrictions.eq("provider", provider));
		}
		
		if (locationTo != null) {
			criteria.add(Restrictions.eq("locationTo", locationTo));
		}
		
		if (locationFrom != null) {
			criteria.add(Restrictions.eq("locationFrom", locationFrom));
		}
		
		if (patient != null) {
			criteria.add(Restrictions.eq("patient", patient));
		}
		
		if (status != null) {
			criteria.add(Restrictions.eq("status", status));
		}
		
		if (queueRoom != null) {
			criteria.add(Restrictions.eq("queueRoom", queueRoom));
		}
		
		criteria.addOrder(Order.desc("dateCreated"));
		
		return criteria.list();
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#savePatientQue(org.openmrs.module.patientqueueing.model.PatientQueue)
	 */
	public PatientQueue savePatientQueue(PatientQueue patientQueue) {
		sessionFactory.getCurrentSession().saveOrUpdate(patientQueue);
		return patientQueue;
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getIncompletePatientQueue(org.openmrs.Patient,
	 *      org.openmrs.Location)
	 */
	public PatientQueue getIncompletePatientQueue(Patient patient, Location locationTo, Location queueRoom) {
		Criteria criteria = getSession().createCriteria(PatientQueue.class);
		
		if (locationTo != null) {
			criteria.add(Restrictions.eq("locationTo", locationTo));
		}
		
		if (patient != null) {
			criteria.add(Restrictions.eq("patient", patient));
		}
		
		if (queueRoom != null) {
			criteria.add(Restrictions.eq("queueRoom", queueRoom));
		}
		
		criteria.add(Restrictions.ne("status", PatientQueue.Status.COMPLETED));
		
		return (PatientQueue) criteria.uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getMostRecentQueue(org.openmrs.Patient)
	 */
	public PatientQueue getMostRecentQueue(Patient patient) {
		Criteria criteria = getSession().createCriteria(PatientQueue.class);
		
		criteria.add(Restrictions.eq("patient", patient));
		criteria.addOrder(Order.desc("dateCreated"));
		criteria.setMaxResults(1);
		
		return (PatientQueue) criteria.uniqueResult();
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#getPatientQueueListBySearchParams(java.lang.String,
	 *      java.util.Date, java.util.Date, org.openmrs.Location, org.openmrs.Location,
	 *      org.openmrs.module.patientqueueing.model.PatientQueue.Status)
	 */
	
	public List<PatientQueue> getPatientQueueList(List<Patient> patientList, Date fromDate, Date toDate,
	        Location locationTo, Location locationFrom, PatientQueue.Status status, Location queueRoom) {
		Criteria criteria = getSession().createCriteria(PatientQueue.class);
		
		if (!patientList.isEmpty()) {
			criteria.add(Restrictions.in("patient", patientList));
		}
		
		if (fromDate != null && toDate != null) {
			criteria.add(Restrictions.between("dateCreated", fromDate, toDate));
		}
		
		if (locationTo != null) {
			criteria.add(Restrictions.eq("locationTo", locationTo));
		}
		
		if (locationFrom != null) {
			criteria.add(Restrictions.eq("locationFrom", locationFrom));
		}
		
		if (status != null) {
			criteria.add(Restrictions.eq("status", status));
		}
		
		if (queueRoom != null) {
			criteria.add(Restrictions.eq("queueRoom", queueRoom));
		}
		
		criteria.addOrder(Order.desc("dateCreated"));
		
		return criteria.list();
	}
	
	/**
	 * @see org.openmrs.module.patientqueueing.api.PatientQueueingService#
	 *      getPatientQueueByParentLocation(org.openmrs.Location,
	 *      org.openmrs.module.patientqueueing.model.PatientQueue.Status,java.util.Date dateFrom,
	 *      java.util.Date)
	 */
	public List<PatientQueue> getPatientsInQueueRoom(List<Location> queueRooms, PatientQueue.Status status, Date fromDate, Date toDate) {
		Criteria criteria = getSession().createCriteria(PatientQueue.class);
		
		if (fromDate != null && toDate != null) {
			criteria.add(Restrictions.between("dateCreated", fromDate, toDate));
		}
		
		if (status != null) {
			criteria.add(Restrictions.eq("status", status));
		}
		
		if (queueRooms != null) {
			criteria.add(Restrictions.in("queueRoom", queueRooms));
		}
		
		criteria.addOrder(Order.desc("dateCreated"));
		return criteria.list();
	}
	
	public List<PatientQueue> getPatientQueueByVisitNumber(String visitNumber, Date fromDate, Date toDate) {
		Criteria criteria = getSession().createCriteria(PatientQueue.class);
		
		if (fromDate != null && toDate != null) {
			criteria.add(Restrictions.between("dateCreated", fromDate, toDate));
		}
		
		criteria.add(Restrictions.eq("visitNumber", visitNumber));
		
		criteria.addOrder(Order.desc("dateCreated"));
		
		return criteria.list();
	}
	
	public List<PatientQueue> getPatientQueueListFifo(Provider provider, Date fromDate, Date toDate, Location locationTo,
	        Location locationFrom, Patient patient, PatientQueue.Status status, Location queueRoom) {
		Criteria criteria = getSession().createCriteria(PatientQueue.class);
		
		if (fromDate != null && toDate != null) {
			criteria.add(Restrictions.between("dateCreated", fromDate, toDate));
		}

		if (provider != null) {
			criteria.add(Restrictions.eq("provider", provider));
		}

		if (locationTo != null) {
			criteria.add(Restrictions.eq("locationTo", locationTo));
		}

		if (locationFrom != null) {
			criteria.add(Restrictions.eq("locationFrom", locationFrom));
		}

		if (patient != null) {
			criteria.add(Restrictions.eq("patient", patient));

		}

		if (status != null) {
			criteria.add(Restrictions.eq("status", status));
		}

		if (queueRoom != null) {
			criteria.add(Restrictions.eq("queueRoom", queueRoom));
		}

		criteria.addOrder(Order.asc("dateCreated"));
		return criteria.list();
	}

	public List<PatientQueue> getPatientsInQueueRoomFifo(List<Location> queueRooms, PatientQueue.Status status,
	        Date fromDate, Date toDate) {
		Criteria criteria = getSession().createCriteria(PatientQueue.class);
		
		if (fromDate != null && toDate != null) {
			criteria.add(Restrictions.between("dateCreated", fromDate, toDate));
		}

		if (status != null) {
			criteria.add(Restrictions.eq("status", status));
		}

		if (queueRooms != null) {
			criteria.add(Restrictions.in("queueRoom", queueRooms));
		}
		
		criteria.addOrder(Order.asc("dateCreated"));
		return criteria.list();
	}

	// ========== Non-Patient Queue DAO Methods ==========

	/**
	 * Get a NonPatientQueue by id
	 *
	 * @param queueId the id of the queue entry
	 * @return the NonPatientQueue with the given id
	 */
	public NonPatientQueue getNonPatientQueueById(Integer queueId) {
		return (NonPatientQueue) getSession().createCriteria(NonPatientQueue.class)
		        .add(Restrictions.eq("nonPatientQueueId", queueId)).uniqueResult();
	}

	/**
	 * Get a NonPatientQueue by uuid
	 *
	 * @param uuid the uuid of the queue entry
	 * @return the NonPatientQueue with the given uuid
	 */
	public NonPatientQueue getNonPatientQueueByUuid(String uuid) {
		return (NonPatientQueue) getSession().createCriteria(NonPatientQueue.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}

	/**
	 * Get NonPatientQueue entries by ticket number
	 *
	 * @param ticketNumber the ticket number to search for
	 * @param fromDate the start date for filtering
	 * @param toDate the end date for filtering
	 * @return list of NonPatientQueue entries matching the ticket number
	 */
	public List<NonPatientQueue> getNonPatientQueueByTicketNumber(String ticketNumber, Date fromDate, Date toDate) {
		Criteria criteria = getSession().createCriteria(NonPatientQueue.class);

		if (ticketNumber != null) {
			criteria.add(Restrictions.eq("ticketNumber", ticketNumber));
		}

		if (fromDate != null && toDate != null) {
			criteria.add(Restrictions.between("dateCreated", fromDate, toDate));
		}

		criteria.addOrder(Order.desc("dateCreated"));

		return criteria.list();
	}

	/**
	 * Get NonPatientQueue entries by queue room
	 *
	 * @param queueRoom the queue room location
	 * @param fromDate the start date for filtering
	 * @param toDate the end date for filtering
	 * @return list of NonPatientQueue entries in the queue room
	 */
	public List<NonPatientQueue> getNonPatientQueuesByQueueRoom(Location queueRoom, Date fromDate, Date toDate) {
		Criteria criteria = getSession().createCriteria(NonPatientQueue.class);

		if (queueRoom != null) {
			criteria.add(Restrictions.eq("queueRoom", queueRoom));
		}

		if (fromDate != null && toDate != null) {
			criteria.add(Restrictions.between("dateCreated", fromDate, toDate));
		}

		criteria.addOrder(Order.asc("dateCreated"));

		return criteria.list();
	}

	/**
	 * Get NonPatientQueue entries by queue room and status
	 *
	 * @param queueRoom the queue room location
	 * @param status the status to filter by
	 * @return list of NonPatientQueue entries matching the criteria
	 */
	public List<NonPatientQueue> getNonPatientQueuesByQueueRoomAndStatus(Location queueRoom,
	        NonPatientQueue.NonPatientQueueStatus status) {
		Criteria criteria = getSession().createCriteria(NonPatientQueue.class);

		if (queueRoom != null) {
			criteria.add(Restrictions.eq("queueRoom", queueRoom));
		}

		if (status != null) {
			criteria.add(Restrictions.eq("status", status));
		}

		criteria.addOrder(Order.asc("dateCreated"));

		return criteria.list();
	}

	/**
	 * Get all active NonPatientQueue entries
	 *
	 * @return list of all active NonPatientQueue entries
	 */
	public List<NonPatientQueue> getAllActiveNonPatientQueues() {
		return getSession().createCriteria(NonPatientQueue.class)
		        .add(Restrictions.ne("status", NonPatientQueue.NonPatientQueueStatus.COMPLETED))
		        .add(Restrictions.ne("status", NonPatientQueue.NonPatientQueueStatus.CANCELLED))
		        .add(Restrictions.eq("voided", false)).addOrder(Order.asc("dateCreated")).list();
	}

	/**
	 * Get NonPatientQueue entries with flexible filtering
	 *
	 * @param status the status to filter by (can be null)
	 * @param queueType the queue type concept to filter by (can be null)
	 * @param locationTo the destination location to filter by (can be null)
	 * @param queueRoom the queue room to filter by (can be null)
	 * @param fromDate the start date for filtering (can be null)
	 * @param toDate the end date for filtering (can be null)
	 * @return list of NonPatientQueue entries matching the criteria
	 */
	public List<NonPatientQueue> getNonPatientQueues(NonPatientQueue.NonPatientQueueStatus status, Concept queueType,
	        Location locationTo, Location queueRoom, Date fromDate, Date toDate) {
		Criteria criteria = getSession().createCriteria(NonPatientQueue.class);

		if (status != null) {
			criteria.add(Restrictions.eq("status", status));
		}

		if (queueType != null) {
			criteria.add(Restrictions.eq("queueType", queueType));
		}

		if (locationTo != null) {
			criteria.add(Restrictions.eq("locationTo", locationTo));
		}

		if (queueRoom != null) {
			criteria.add(Restrictions.eq("queueRoom", queueRoom));
		}

		if (fromDate != null && toDate != null) {
			criteria.add(Restrictions.between("dateCreated", fromDate, toDate));
		}

		criteria.addOrder(Order.asc("dateCreated"));

		return criteria.list();
	}

	/**
	 * Save or update a NonPatientQueue entry
	 *
	 * @param nonPatientQueue the queue entry to save
	 * @return the saved NonPatientQueue entry
	 */
	public NonPatientQueue saveNonPatientQueue(NonPatientQueue nonPatientQueue) {
		getSession().saveOrUpdate(nonPatientQueue);
		return nonPatientQueue;
	}

}
