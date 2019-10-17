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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Repository("patientqueueing.PatientQueueingDao")
public class PatientQueueingDao {
    private static final String LOCATION_TO = "locationTo";

    private static final String LOCATION_FROM = "locationFrom";

    private static final String PROVIDER = "provider";

    private static final String STATUS = "status";

    private static final String PATIENT = "patient";

    private static final String DATE_CREATED = "dateCreated";


    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    DbSessionFactory sessionFactory;

    public DbSession getSession() {
        return sessionFactory.getCurrentSession();
    }

    public PatientQueue getPatientQueueById(String queueId) {
        return (PatientQueue) getSession().createCriteria(PatientQueue.class).add(Restrictions.eq("patient_queue_id", queueId)).uniqueResult();
    }

    public List<PatientQueue> getPatientQueueByQueueNumber(String queueNumber) {
        return (List<PatientQueue>) getSession().createCriteria(PatientQueue.class).add(Restrictions.eq("queueNumber", queueNumber)).list();
    }


    public List<PatientQueue> getPatientQueueList(String searchString, Date fromDate, Date toDate,Patient patient, Provider provider, Location locationTo, Location locationFrom, String status) {
        Criteria criteria = getSession().createCriteria(PatientQueue.class);

        if (!searchString.equals("")) {
            List<Patient> patientIdList = processPatientSearchString(searchString);

            if (!patientIdList.isEmpty()) {
                criteria.add(Restrictions.in(PATIENT, patientIdList));
            } else {
                return new ArrayList<PatientQueue>();
            }
        }

        if (fromDate != null && toDate != null) {
            criteria.add(Restrictions.between(DATE_CREATED, fromDate, toDate));
        }

        if (provider != null) {
            criteria.add(Restrictions.eq(PROVIDER, provider));
        }

        if (provider != null) {
            criteria.add(Restrictions.eq(PATIENT, patient));
        }

        if (locationTo != null) {
            criteria.add(Restrictions.eq(LOCATION_TO, locationTo));
        }

        if (locationFrom != null) {
            criteria.add(Restrictions.eq(LOCATION_FROM, locationFrom));
        }

        if (status != null) {
            criteria.add(Restrictions.eq(STATUS, status));
        }

        criteria.addOrder(Order.desc(DATE_CREATED));

        return criteria.list();
    }

    public PatientQueue savePatientQueue(PatientQueue patientQueue) {
        try {
            sessionFactory.getCurrentSession().saveOrUpdate(patientQueue);
            return patientQueue;
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    private List<Patient> processPatientSearchString(String searchString) {
        PatientService patientService = Context.getPatientService();

        List list = Arrays.asList(searchString.split(" "));

        List<Patient> patientList = new ArrayList<Patient>();

        for (Object o : list) {
            List<Patient> patients = patientService.getPatients(o.toString());
            patientList.addAll(patients);
        }
        return patientList;
    }


}
