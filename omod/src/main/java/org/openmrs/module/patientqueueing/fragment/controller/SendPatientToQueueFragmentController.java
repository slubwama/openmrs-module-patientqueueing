package org.openmrs.module.patientqueueing.fragment.controller;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Date;

public class SendPatientToQueueFragmentController {
	
	protected final Logger log = LoggerFactory.getLogger(SendPatientToQueueFragmentController.class);
	
	public void controller(@SpringBean FragmentModel pageModel,
	        @SpringBean("locationService") LocationService locationService,
	        @RequestParam(value = "patientId", required = false) Patient patient) {
		if (patient != null) {
			pageModel.put("birthDate", patient.getBirthdate());
			pageModel.put("patient", patient);
			pageModel.put("patientId", patient.getPatientId());
		}
		pageModel.put("locationList", (locationService.getRootLocations(false).get(0)).getChildLocations());
		
		pageModel.put("providerList", Context.getProviderService().getAllProviders(false));
	}
	
	public SimpleObject create(@RequestParam(value = "patientId") Patient patient,
	        @RequestParam(value = "providerId", required = false) Provider provider, UiUtils ui,
	        @RequestParam("locationId") Location location, UiSessionContext uiSessionContext) throws IOException {
		PatientQueue patientQueue = new PatientQueue();
		
		PatientQueueingService patientQueueingService = Context.getService(PatientQueueingService.class);
		patientQueue.setLocationFrom(uiSessionContext.getSessionLocation());
		patientQueue.setPatient(patient);
		patientQueue.setLocationTo(location);
		patientQueue.setProvider(provider);
		patientQueue.setStatus(PatientQueue.Status.PENDING);
		patientQueue.setQueueNumber(patientQueueingService.generateQueueNumber(location, new Date(), patient));
		patientQueue.setCreator(uiSessionContext.getCurrentUser());
		patientQueue.setDateCreated(new Date());
		
		patientQueueingService.savePatientQue(patientQueue);
		
		SimpleObject simpleObject = new SimpleObject();
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		simpleObject.put(
		    "toastMessage",
		    objectMapper.writeValueAsString(SimpleObject.create("message", "Patient Sent to "
		            + patientQueue.getLocationTo().getName() + " with Queue Number: " + patientQueue.getQueueNumber())));
		
		return simpleObject;
		
	}
}
