package org.openmrs.module.patientqueueing.api.dao;

import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PatientQueueingDaoOrderingTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	private PatientQueueingDao dao;
	
	@Test
	public void getPatientQueueListFifo_shouldReturnOldestFirst() {
		// Arrange
		Location locTo = Context.getLocationService().getLocation(1);
		Patient patient = Context.getPatientService().getPatient(2);
		
		PatientQueue older = new PatientQueue();
		older.setPatient(patient);
		older.setLocationTo(locTo);
		older.setStatus(PatientQueue.Status.PENDING);
		older.setDateCreated(daysAgo(2));
		
		PatientQueue newer = new PatientQueue();
		newer.setPatient(patient);
		newer.setLocationTo(locTo);
		newer.setStatus(PatientQueue.Status.PENDING);
		newer.setDateCreated(daysAgo(1));
		
		dao.savePatientQueue(older);
		dao.savePatientQueue(newer);
		
		// Act
		List<PatientQueue> rows = dao.getPatientQueueListFifo(null, daysAgo(10), new Date(), locTo, null, patient,
		    PatientQueue.Status.PENDING, null);
		
		// Assert (FIFO => older first)
		assertThat(rows, is(not(empty())));
		assertThat(rows.get(0).getDateCreated(), is(older.getDateCreated()));
		assertThat(rows.get(1).getDateCreated(), is(newer.getDateCreated()));
	}
	
	@Test
	public void getPatientsInQueueRoomFifo_shouldReturnOldestFirst() {
		// Arrange
		Location room = Context.getLocationService().getLocation(1);
		Patient patient = Context.getPatientService().getPatient(2);
		
		PatientQueue older = new PatientQueue();
		older.setPatient(patient);
		older.setQueueRoom(room);
		older.setStatus(PatientQueue.Status.PENDING);
		older.setDateCreated(daysAgo(3));
		
		PatientQueue newer = new PatientQueue();
		newer.setPatient(patient);
		newer.setQueueRoom(room);
		newer.setStatus(PatientQueue.Status.PENDING);
		newer.setDateCreated(daysAgo(1));
		
		dao.savePatientQueue(older);
		dao.savePatientQueue(newer);
		
		// Act
		List<PatientQueue> rows = dao.getPatientsInQueueRoomFifo(Arrays.asList(room), PatientQueue.Status.PENDING,
		    daysAgo(10), new Date());
		
		// Assert (FIFO => older first)
		assertThat(rows, is(not(empty())));
		assertThat(rows.get(0).getDateCreated(), is(older.getDateCreated()));
		assertThat(rows.get(1).getDateCreated(), is(newer.getDateCreated()));
	}
	
	private Date daysAgo(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -days);
		return cal.getTime();
	}
}
