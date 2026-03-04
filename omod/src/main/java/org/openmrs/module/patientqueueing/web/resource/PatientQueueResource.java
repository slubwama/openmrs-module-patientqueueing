package org.openmrs.module.patientqueueing.web.resource;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientqueueing.api.PatientQueueingService;
import org.openmrs.module.patientqueueing.model.PatientQueue;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.webservices.validation.ValidateUtil;
import org.openmrs.util.OpenmrsUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Resource(name = RestConstants.VERSION_1 + "/patientqueue", supportedClass = PatientQueue.class, supportedOpenmrsVersions = {"1.9.* - 9.*"})
public class PatientQueueResource extends DelegatingCrudResource<PatientQueue> {
	
	private PatientQueueingService service() {
		return Context.getService(PatientQueueingService.class);
	}
	
	@Override
	public PatientQueue newDelegate() {
		return new PatientQueue();
	}

	@Override
	public PatientQueue save(PatientQueue patientQueue) {
		PatientQueueingService patientQueueingService=Context.getService(PatientQueueingService.class);
		patientQueue = patientQueueingService.assignVisitNumberForToday(patientQueue);
		return patientQueueingService.savePatientQue(patientQueue);
	}
	
	@Override
	public PatientQueue getByUniqueId(String uniqueId) {
		PatientQueue patientQueue = service().getPatientQueueByUuid(uniqueId);
		if (patientQueue == null && uniqueId != null) {
			Integer id = null;
			try {
				id = Integer.valueOf(uniqueId);
			}
			catch (Exception e) {
				// ignore
			}
			if (id != null) {
				patientQueue = service().getPatientQueueById(id);
			}
		}
		return patientQueue;
	}
	
	/**
	 * ✅ Return a PatientQueue (not Object). OpenMRS REST will still wrap/convert based on
	 * representation.
	 */
	@Override
	public PatientQueue update(String uuid, SimpleObject propertiesToUpdate, RequestContext context)
	        throws ResponseException {
		Object statusObj = propertiesToUpdate.get("status");
		if (statusObj == null) {
			// fallback to default behavior (still results in a PatientQueue update)
			super.update(uuid, propertiesToUpdate, context);
			return getByUniqueId(uuid);
		}
		
		String statusStr = String.valueOf(statusObj);
		String normalized = normalizeStatus(statusStr);
		
		PatientQueue patientQueue = getPatientQueueForUpdate(uuid, propertiesToUpdate);
		ValidateUtil.validate(patientQueue);
		patientQueue.setDateChanged(new Date());
		
		// Apply mapped status + timestamps in a backward compatible way
		applyStatusAndTimestamps(patientQueue, normalized);
		
		return save(patientQueue);
	}
	
	public PatientQueue getPatientQueueForUpdate(String uuid, Map<String, Object> propertiesToUpdate) {
		PatientQueue patientQueue = getByUniqueId(uuid);
		PatientQueueResource patientQueueResource = (PatientQueueResource) Context.getService(RestService.class)
		        .getResourceBySupportedClass(PatientQueue.class);
		patientQueueResource.setConvertedProperties(patientQueue, propertiesToUpdate,
		    patientQueueResource.getUpdatableProperties(), false);
		return patientQueue;
	}
	
	@Override
	public NeedsPaging<PatientQueue> doGetAll(RequestContext context) throws ResponseException {
		return new NeedsPaging<PatientQueue>(new ArrayList<PatientQueue>(service().getPatientQueueList(null, null, null,
		    null, null, null, null)), context);
	}
	
	@Override
	public List<Representation> getAvailableRepresentations() {
		return Arrays.asList(Representation.DEFAULT, Representation.FULL);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof DefaultRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("patient", Representation.REF);
			description.addProperty("datePicked");
			description.addProperty("dateCompleted");
			description.addProperty("locationFrom", Representation.REF);
			description.addProperty("locationTo", Representation.REF);
			description.addProperty("provider", Representation.REF);
			description.addProperty("encounter", Representation.REF);
			description.addProperty("status");
			description.addProperty("priority");
			description.addProperty("priorityComment");
			description.addProperty("visitNumber");
			description.addProperty("comment");
			description.addProperty("queueRoom", Representation.REF);
			
			// New fields (safe additions)
			description.addProperty("ticketNumber");
			description.addProperty("checkedInAt");
			description.addProperty("calledAt");
			description.addProperty("startedAt");
			description.addProperty("endedAt");
			description.addProperty("priorityScore");
			description.addProperty("priorityReason");
			
			description.addSelfLink();

			return description;
		} else if (rep instanceof FullRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("creator");
			description.addProperty("dateCreated");
			description.addProperty("changedBy");
			description.addProperty("dateChanged");
			description.addProperty("voided");
			description.addProperty("dateVoided");
			description.addProperty("voidedBy");
			description.addProperty("patient");
			description.addProperty("provider");
			description.addProperty("locationFrom");
			description.addProperty("locationTo");
			description.addProperty("encounter");
			description.addProperty("status");
			description.addProperty("priority");
			description.addProperty("priorityComment");
			description.addProperty("visitNumber");
			description.addProperty("comment");
			description.addProperty("queueRoom");
			description.addProperty("datePicked");
			description.addProperty("dateCompleted");
			
			// New fields
			description.addProperty("ticketNumber");
			description.addProperty("checkedInAt");
			description.addProperty("calledAt");
			description.addProperty("startedAt");
			description.addProperty("endedAt");
			description.addProperty("priorityScore");
			description.addProperty("priorityReason");
			
			description.addSelfLink();
			description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
			return description;
		} else if (rep instanceof RefRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("visitNumber");
			description.addProperty("dateCreated");
			description.addProperty("dateChanged");
			description.addProperty("locationFrom");
			description.addProperty("locationTo");
			description.addProperty("status");
			description.addProperty("queueRoom");
			description.addProperty("datePicked");
			description.addProperty("dateCompleted");
			description.addProperty("provider");
			description.addSelfLink();
			return description;
		}
		return null;
	}
	
	@Override
	protected void delete(PatientQueue patientQueue, String s, RequestContext requestContext) throws ResponseException {
		// legacy no-op
	}
	
	@Override
	public void purge(PatientQueue patientQueue, RequestContext requestContext) throws ResponseException {

	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("patient");
		description.addProperty("provider");
		description.addProperty("locationTo");
		description.addProperty("locationFrom");
		description.addProperty("status");
		description.addProperty("encounter");
		description.addProperty("visitNumber");
		description.addProperty("priority");
		description.addProperty("queueRoom");
		description.addProperty("provider");
		description.addProperty("datePicked");
		description.addProperty("dateCompleted");
		description.addProperty("priorityComment");
		description.addProperty("comment");
		
		// New optional inputs
		description.addProperty("ticketNumber");
		description.addProperty("checkedInAt");
		description.addProperty("calledAt");
		description.addProperty("startedAt");
		description.addProperty("endedAt");
		description.addProperty("priorityScore");
		description.addProperty("priorityReason");
		
		return description;
	}
	
	@Override
	protected PageableResult doSearch(RequestContext context) {
		PatientQueueingService patientQueueingService = service();
		
		String locationQuery = context.getParameter("location");
		String parentLocationQuery = context.getParameter("parentLocation");
		boolean onlyInQueueRooms = Boolean.parseBoolean(context.getParameter("onlyInQueueRooms"));
		String statusParam = context.getParameter("status");
		String queueRoomQuery = context.getParameter("room");
		
		Location location = null;
		Location room = null;
		
		if (locationQuery != null && !locationQuery.trim().isEmpty()) {
			location = Context.getLocationService().getLocationByUuid(locationQuery);
		}
		if (queueRoomQuery != null && !queueRoomQuery.trim().isEmpty()) {
			room = Context.getLocationService().getLocationByUuid(queueRoomQuery);
		}
		
		PatientQueue.Status queueStatus = mapToQueueStatus(normalizeStatus(statusParam));
		
		Date from = OpenmrsUtil.firstSecondOfDay(new Date());
		Date to = OpenmrsUtil.getLastMomentOfDay(new Date());
		
		List<PatientQueue> results;
		
		if (parentLocationQuery != null && !parentLocationQuery.trim().isEmpty()) {
			Location parent = Context.getLocationService().getLocationByUuid(parentLocationQuery);
			results = patientQueueingService
			        .getPatientQueueByParentLocation(parent, queueStatus, from, to, onlyInQueueRooms);
		} else {
			results = patientQueueingService.getPatientQueueListBySearchParams(context.getParameter("searchString"), from,
			    to, location, null, queueStatus, room);
		}
		
		return new NeedsPaging<PatientQueue>(results, context);
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("provider");
		description.addProperty("status");
		description.addProperty("encounter");
		description.addProperty("queueRoom");
		description.addProperty("datePicked");
		description.addProperty("dateCompleted");
		description.addProperty("dateChanged");
		description.addProperty("comment");
		description.addProperty("priorityComment");
		description.addProperty("priority");
		
		// New optional fields
		description.addProperty("ticketNumber");
		description.addProperty("checkedInAt");
		description.addProperty("calledAt");
		description.addProperty("startedAt");
		description.addProperty("endedAt");
		description.addProperty("priorityScore");
		description.addProperty("priorityReason");
		
		return description;
	}
	
	// Swagger models (unchanged aside from dedupe)
	@Override
	public Model getGETModel(Representation rep) {
		ModelImpl model = (ModelImpl) super.getGETModel(rep);
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			model.property("uuid", new StringProperty()).property("dateCreated", new DateProperty())
			        .property("voided", new BooleanProperty()).property("priority", new IntegerProperty())
			        .property("priorityComment", new StringProperty()).property("visitNumber", new StringProperty())
			        .property("comment", new StringProperty()).property("status", new StringProperty())
			        .property("datePicked", new DateProperty()).property("dateCompleted", new DateProperty())
			        .property("ticketNumber", new StringProperty()).property("checkedInAt", new DateProperty())
			        .property("calledAt", new DateProperty()).property("startedAt", new DateProperty())
			        .property("endedAt", new DateProperty()).property("priorityScore", new IntegerProperty())
			        .property("priorityReason", new StringProperty());
		}
		if (rep instanceof DefaultRepresentation) {
			model.property("patient", new RefProperty("#/definitions/PatientGetRef"))
			        .property("creator", new RefProperty("#/definitions/UserGetRef"))
			        .property("changedBy", new RefProperty("#/definitions/UserGetRef"))
			        .property("voidedBy", new RefProperty("#/definitions/UserGetRef"))
			        .property("provider", new RefProperty("#/definitions/ProviderGetRef"))
			        .property("locationFrom", new RefProperty("#/definitions/LocationGetRef"))
			        .property("locationTo", new RefProperty("#/definitions/LocationGetRef"))
			        .property("encounter", new RefProperty("#/definitions/EncounterGetRef"));
		} else if (rep instanceof FullRepresentation) {
			model.property("patient", new RefProperty("#/definitions/PatientGetRef"))
			        .property("creator", new RefProperty("#/definitions/UserGetRef"))
			        .property("changedBy", new RefProperty("#/definitions/UserGetRef"))
			        .property("voidedBy", new RefProperty("#/definitions/UserGetRef"))
			        .property("provider", new RefProperty("#/definitions/ProviderGetRef"))
			        .property("locationFrom", new RefProperty("#/definitions/LocationGetRef"))
			        .property("locationTo", new RefProperty("#/definitions/LocationGetRef"))
			        .property("encounter", new RefProperty("#/definitions/EncounterGetRef"));
		}
		return model;
	}
	
	@Override
	public Model getCREATEModel(Representation rep) {
		ModelImpl model = (ModelImpl) super.getGETModel(rep);
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			model.property("uuid", new StringProperty()).property("dateCreated", new DateProperty())
			        .property("voided", new BooleanProperty()).property("status", new StringProperty())
			        .property("priority", new IntegerProperty()).property("priorityComment", new StringProperty())
			        .property("visitNumber", new StringProperty()).property("comment", new StringProperty())
			        .property("datePicked", new DateProperty()).property("dateCompleted", new DateProperty())
			        .property("ticketNumber", new StringProperty()).property("checkedInAt", new DateProperty())
			        .property("calledAt", new DateProperty()).property("startedAt", new DateProperty())
			        .property("endedAt", new DateProperty()).property("priorityScore", new IntegerProperty())
			        .property("priorityReason", new StringProperty());
		}
		if (rep instanceof DefaultRepresentation) {
			model.property("patient", new RefProperty("#/definitions/PatientCreate"))
			        .property("creator", new RefProperty("#/definitions/UserCreate"))
			        .property("changedBy", new RefProperty("#/definitions/UserCreate"))
			        .property("voidedBy", new RefProperty("#/definitions/UserCreate"))
			        .property("provider", new RefProperty("#/definitions/ProviderCreate"))
			        .property("locationFrom", new RefProperty("#/definitions/LocationCreate"))
			        .property("locationTo", new RefProperty("#/definitions/LocationCreate"))
			        .property("encounter", new RefProperty("#/definitions/EncounterCreate"));
		} else if (rep instanceof FullRepresentation) {
			model.property("patient", new RefProperty("#/definitions/PatientCreate"))
			        .property("creator", new RefProperty("#/definitions/UserCreate"))
			        .property("changedBy", new RefProperty("#/definitions/UserCreate"))
			        .property("voidedBy", new RefProperty("#/definitions/UserCreate"))
			        .property("provider", new RefProperty("#/definitions/ProviderCreate"))
			        .property("locationFrom", new RefProperty("#/definitions/LocationCreate"))
			        .property("locationTo", new RefProperty("#/definitions/LocationCreate"))
			        .property("encounter", new RefProperty("#/definitions/EncounterCreate"));
		}
		return model;
	}
	
	@Override
	public Model getUPDATEModel(Representation rep) {
		return new ModelImpl().property("status", new StringProperty()).property("priority", new IntegerProperty())
		        .property("priorityComment", new StringProperty()).property("comment", new StringProperty())
		        .property("encounter", new StringProperty()).property("datePicked", new DateProperty())
		        .property("dateCompleted", new DateProperty())
		        .property("provider", new RefProperty("#/definitions/ProviderCreate"))
		        .property("voided", new BooleanProperty()).property("ticketNumber", new StringProperty())
		        .property("checkedInAt", new DateProperty()).property("calledAt", new DateProperty())
		        .property("startedAt", new DateProperty()).property("endedAt", new DateProperty())
		        .property("priorityScore", new IntegerProperty()).property("priorityReason", new StringProperty());
	}
	
	// ----------------- status helpers -----------------
	
	private String normalizeStatus(String status) {
		if (status == null)
			return null;
		String s = status.trim().toLowerCase();
		return s.replace('-', '_').replace(' ', '_');
	}
	
	private PatientQueue.Status mapToQueueStatus(String normalized) {
		if (normalized == null || normalized.trim().isEmpty())
			return null;
		
		// Legacy
		if ("pending".equals(normalized))
			return PatientQueue.Status.PENDING;
		if ("picked".equals(normalized))
			return PatientQueue.Status.PICKED;
		if ("completed".equals(normalized))
			return PatientQueue.Status.COMPLETED;
		
		// New
		if ("waiting".equals(normalized))
			return PatientQueue.Status.WAITING;
		if ("present".equals(normalized) || "checked_in".equals(normalized) || "checkedin".equals(normalized))
			return PatientQueue.Status.PRESENT;
		if ("called".equals(normalized))
			return PatientQueue.Status.CALLED;
		if ("in_service".equals(normalized) || "inservice".equals(normalized))
			return PatientQueue.Status.IN_SERVICE;
		if ("no_show".equals(normalized) || "noshow".equals(normalized))
			return PatientQueue.Status.NO_SHOW;
		if ("skipped".equals(normalized))
			return PatientQueue.Status.SKIPPED;
		if ("cancelled".equals(normalized) || "canceled".equals(normalized))
			return PatientQueue.Status.CANCELLED;
		
		return null;
	}
	
	private void applyStatusAndTimestamps(PatientQueue pq, String normalizedStatus) {
		PatientQueue.Status mapped = mapToQueueStatus(normalizedStatus);
		if (mapped == null)
			return;
		
		pq.setStatus(mapped);
		Date now = new Date();
		
		// Legacy alignment
		if (mapped == PatientQueue.Status.PICKED) {
			if (pq.getDatePicked() == null)
				pq.setDatePicked(now);
			if (pq.getCalledAt() == null)
				pq.setCalledAt(pq.getDatePicked());
			return;
		}
		
		if (mapped == PatientQueue.Status.COMPLETED) {
			if (pq.getDateCompleted() == null)
				pq.setDateCompleted(now);
			if (pq.getEndedAt() == null)
				pq.setEndedAt(pq.getDateCompleted());
			return;
		}
		
		if (mapped == PatientQueue.Status.PENDING) {
			pq.setDatePicked(null);
			pq.setDateCompleted(null);
			pq.setCalledAt(null);
			pq.setStartedAt(null);
			pq.setEndedAt(null);
			return;
		}
		
		// New timestamps
		if (mapped == PatientQueue.Status.PRESENT && pq.getCheckedInAt() == null) {
			pq.setCheckedInAt(now);
		} else if (mapped == PatientQueue.Status.CALLED && pq.getCalledAt() == null) {
			pq.setCalledAt(now);
			if (pq.getDatePicked() == null)
				pq.setDatePicked(now);
		} else if (mapped == PatientQueue.Status.IN_SERVICE && pq.getStartedAt() == null) {
			pq.setStartedAt(now);
			if (pq.getDatePicked() == null)
				pq.setDatePicked(now);
		}
	}
}
