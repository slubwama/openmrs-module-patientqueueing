# Self Check-in Migration Plan
## UgandaEMR → Patient Queueing Module

**Status**: Planning Phase
**Created**: 2026-04-20
**Objective**: Migrate self check-in features from UgandaEMR module to patientqueueing module in a generic, implementation-agnostic way

---

## Executive Summary

The UgandaEMR module has implemented self check-in functionality that is currently implementation-specific. This plan outlines migrating those features to the patientqueueing module while making them generic enough for any OpenMRS distribution to use.

### Current State (UgandaEMR)
- Self check-in via phone/national ID/patient ID
- Non-patient queue management (visitors, payments, etc.)
- Queue kiosk display (patient + non-patient unified)
- Public queue display screens
- Provider queue management dashboard
- Custom identifier types (National ID, Patient ID)

### Target State (Patient Queueing)
- Generic patient identification system (configurable identifier types)
- Non-patient queue system (configurable queue types)
- Unified kiosk display for all queue types
- Public queue display feed
- Generic provider queue resources
- Distribution-agnostic implementation

---

## Feature Analysis

### 1. Patient Self Check-in

**Current Implementation (UgandaEMR)**:
- Hardcoded identifier type UUIDs:
  - `PHONE_ATTRIBUTE_TYPE_UUID = "14d4f066-15f5-102d-96e4-000c29c2a5d7"`
  - `NATIONAL_ID_IDENTIFIER_TYPE_UUID = "f0c16a6d-dc5f-4118-a803-616d0075d282"`
  - `PATIENT_ID_IDENTIFIER_TYPE_UUID = "e1731641-30ab-102d-86b0-7a5022ba4115"`

**Issues**:
- UgandaEMR-specific identifiers
- Hardcoded concept/identifier UUIDs
- Not configurable for other distributions

**Generic Solution**:
```java
// Configuration-based patient identifiers
public class PatientIdentifierConfiguration {
    // Map of identifier type names (configurable via GP or module config)
    // Support for patient attributes (phone number)
    // Fallback chain configuration
}

// Generic patient resolution
public Patient resolvePatient(
    String identifierValue,
    List<PatientIdentifierType> identifierTypes,
    List<PersonAttributeType> attributeTypes,
    String searchIdentifierType  // "primaryIdentifier", "phoneNumber", etc.
)
```

### 2. Non-Patient Queue

**Current Implementation**:
- `NonPatientQueue` entity with hardcoded types:
  ```java
  public enum NonPatientQueueType {
      REGISTRATION, VISITOR, ADMIN, PAYMENT, RECORDS, OTHER;
  }
  ```

**Generic Solution**:
```java
// Make queue types configurable
public class QueueType {
    private String name;           // e.g., "REGISTRATION"
    private String displayName;     // e.g., "Registration"
    private String description;
    private Integer sortOrder;
    private Boolean retired;
}

// Or use OpenMRS Concept/LocationTag approach
// Migrate to Concept-based queue types for maximum flexibility
```

**Recommended Approach**: Use OpenMRS **Concept** datatype for queue types
- Allows distribution-specific configuration
- Leverages existing OpenMRS concept dictionary
- Supports translations and concept mapping

### 3. Check-in Workflow

**Current Implementation**:
```java
CheckInPatient checkInPatient(
    Patient patient,
    Location currentLocation,
    Location locationTo,
    Location queueRoom,
    Provider provider,  // Auto-assigned if null
    String visitComment,
    String patientStatus,
    String visitTypeUuid,  // UgandaEMR-specific
    Integer priority
)
```

**Generic Solution**:
```java
// Simplified, generic check-in
CheckInResult checkInPatient(
    Patient patient,
    Location fromLocation,
    Location toLocation,
    Location queueRoom,
    Provider provider,        // Optional: auto-assign
    String visitType,         // Optional: use visit types from core
    Integer priority,
    String comment
)

// Returns
class CheckInResult {
    private PatientQueue patientQueue;
    private Visit visit;                    // Created if needed
    private String ticketNumber;            // Visit number or generated
    private Boolean visitCreated;           // Whether new visit was created
}
```

### 4. Provider Auto-assignment

**Current Implementation**:
```java
public Provider getLeastBusyProviderForLocation(Location location)
```

**Generic Solution**:
- Add to PatientQueueingService
- Implement "load balancing" strategy
- Make strategy configurable (round-robin, least-busy, random, etc.)

```java
public interface ProviderAssignmentStrategy {
    Provider assignProvider(
        Location location,
        List<Provider> availableProviders
    );
}

// Built-in strategies
public class LeastBusyProviderStrategy implements ProviderAssignmentStrategy { }
public class RoundRobinProviderStrategy implements ProviderAssignmentStrategy { }
public class RandomProviderStrategy implements ProviderAssignmentStrategy { }
```

### 5. Queue Kiosk Display

**Current Implementation**:
- Unified DTO supporting both PatientQueue and NonPatientQueue
- Hardcoded queue type enum

**Generic Solution**:
```java
// Polymorphic queue entry
public class QueueEntry {
    private String uuid;
    private String ticketNumber;
    private QueueStatus status;
    private String displayName;
    private QueueType type;           // PATIENT or NON_PATIENT
    private Location locationTo;
    private Location queueRoom;
    private Date dateCreated;
    private Integer priority;
    private String comment;

    // Polymorphic getters
    public boolean isPatient();
    public PatientQueue getPatientQueue();
    public NonPatientQueue getNonPatientQueue();
}
```

### 6. Public Queue Display

**Current Implementation**:
- QueueDisplayResource with facility/location/room views
- Integrated patient and non-patient queues
- Priority-based sorting

**Generic Solution**:
- Keep existing logic but make it distribution-agnostic
- Remove hardcoded concepts/identifiers
- Support custom display configurations via module config

---

## Migration Architecture

### New Module Structure

```
patientqueueing/
├── api/
│   ├── model/
│   │   ├── PatientQueue.java                 (existing)
│   │   ├── NonPatientQueue.java              (new, generic)
│   │   ├── QueueType.java                    (new, Concept-based)
│   │   └── CheckInResult.java                (new)
│   ├── api/
│   │   ├── PatientQueueingService.java       (enhanced)
│   │   ├── NonPatientQueueingService.java    (new)
│   │   └── ProviderAssignmentService.java    (new)
│   ├── dao/
│   │   ├── PatientQueueingDao.java           (existing)
│   │   └── NonPatientQueueingDao.java        (new)
│   └── impl/
│       ├── PatientQueueingServiceImpl.java   (enhanced)
│       ├── NonPatientQueueingServiceImpl.java(new)
│       └── ProviderAssignmentServiceImpl.java(new)
├── omod/
│   └── web/
│       ├── resource/
│       │   ├── PatientQueueResource.java            (existing)
│       │   ├── NonPatientQueueResource.java         (new)
│       │   ├── SelfCheckInResource.java            (new, generic)
│       │   ├── QueueKioskResource.java             (new)
│       │   ├── QueueDisplayResource.java           (new)
│       │   └── ProviderQueueResource.java          (new)
│       └── customdto/
│           ├── QueueEntry.java                     (new)
│           ├── QueueDisplayDto.java                (new)
│           ├── CheckInRequest.java                 (new)
│           └── CheckInResponse.java                (new)
└── config/
    ├── queue_types.xml                             (new, configurable)
    └── checkin_config.xml                          (new)
```

---

## Implementation Plan

### Phase 1: Foundation (Week 1-2)

#### Task 1.1: Add NonPatientQueue Entity
- [ ] Create `NonPatientQueue` model class
- [ ] Add Liquibase changeset for `non_patient_queue` table
- [ ] Make queue types Concept-based (not hardcoded enum)
- [ ] Add indexes for performance
- [ ] Write unit tests

#### Task 1.2: Create NonPatientQueueingService
- [ ] Define service interface
- [ ] Implement DAO layer
- [ ] Implement service layer
- [ ] Add CRUD operations
- [ ] Add status transition methods (call, arrive, start serving, complete)
- [ ] Write integration tests

#### Task 1.3: Configuration System
- [ ] Create module configuration for queue types
- [ ] Create module configuration for patient identifiers
- [ ] Create module configuration for provider assignment strategy
- [ ] Add global properties for defaults
- [ ] Document configuration options

### Phase 2: Self Check-in (Week 3-4)

#### Task 2.1: Generic Patient Identification
- [ ] Create `PatientIdentifierConfiguration` service
- [ ] Support configurable identifier types
- [ ] Support person attributes (e.g., phone number)
- [ ] Implement search order/fallback chain
- [ ] Handle multiple matches gracefully
- [ ] Write comprehensive tests

#### Task 2.2: Check-in Service
- [ ] Create `CheckInService` interface
- [ ] Implement `checkInPatient()` method
- [ ] Auto-create visit if needed
- [ ] Create patient queue entry
- [ ] Generate visit number
- [ ] Handle provider assignment
- [ ] Write integration tests

#### Task 2.3: Self Check-in REST Resource
- [ ] Create `SelfCheckInResource` at `/ws/rest/v1/patientqueueing/checkin`
- [ ] Support patient lookup by multiple identifiers
- [ ] Validate required fields
- [ ] Return meaningful error messages
- [ ] Return both visit and queue information
- [ ] Add Swagger documentation
- [ ] Write REST tests

### Phase 3: Queue Display & Kiosk (Week 5-6)

#### Task 3.1: Unified Queue Entry DTO
- [ ] Create `QueueEntry` polymorphic DTO
- [ ] Support both patient and non-patient queues
- [ ] Add type discriminator
- [ ] Implement consistent field mapping
- [ ] Write unit tests

#### Task 3.2: Queue Kiosk Resource
- [ ] Create `QueueKioskResource` at `/ws/rest/v1/patientqueueing/kiosk`
- [ ] Search by ticket number
- [ ] Return unified queue entry
- [ ] Support both patient and non-patient tickets
- [ ] Add meaningful error for not found
- [ ] Write REST tests

#### Task 3.3: Queue Display Resource
- [ ] Create `QueueDisplayResource` at `/ws/rest/v1/patientqueueing/display`
- [ ] Support facility/location/room views
- [ ] Merge patient and non-patient queues
- [ ] Implement "now serving" vs "up next" logic
- [ ] Priority-based sorting
- [ ] Add last updated timestamp
- [ ] Write REST tests

#### Task 3.4: Non-Patient Queue Resource
- [ ] Create `NonPatientQueueResource` at `/ws/rest/v1/patientqueueing/nonpatient`
- [ ] Full CRUD operations
- [ ] Search by location, status, queue type
- [ ] Bulk operations (call, arrive, complete)
- [ ] Write REST tests

### Phase 4: Provider Features (Week 7)

#### Task 4.1: Provider Assignment Strategies
- [ ] Create `ProviderAssignmentStrategy` interface
- [ ] Implement `LeastBusyProviderStrategy`
- [ ] Implement `RoundRobinProviderStrategy`
- [ ] Implement `RandomProviderStrategy`
- [ ] Make strategy configurable via GP
- [ ] Write unit tests

#### Task 4.2: Provider Queue Dashboard
- [ ] Create `ProviderQueueResource` at `/ws/rest/v1/patientqueueing/providerqueue`
- [ ] Get queues by location
- [ ] Get queues by provider
- [ ] Filter by status
- [ ] Support bulk status updates
- [ ] Write REST tests

### Phase 5: Testing & Documentation (Week 8)

#### Task 5.1: Testing
- [ ] Complete unit test coverage (>80%)
- [ ] Integration test suite
- [ ] REST API contract tests
- [ ] Performance testing
- [ ] Multi-threading tests

#### Task 5.2: Documentation
- [ ] Update module README
- [ ] Add self check-in guide
- [ ] Document configuration options
- [ ] Add API documentation
- [ ] Create migration guide from UgandaEMR
- [ ] Add examples for common use cases

#### Task 5.3: Release Preparation
- [ ] Update CHANGELOG.md
- [ ] Bump module version
- [ ] Tag release
- [ ] Publish to Maven

---

## Generic Design Principles

### 1. Configuration over Code

**Avoid**:
```java
// Hardcoded UUIDs
private static final String NATIONAL_ID_UUID = "f0c16a6d-...";
```

**Prefer**:
```java
// Configurable via Global Property or module config
@GlobalProperty("patientqueueing.identifierTypes.nationalId")
private String nationalIdIdentifierTypeUuid;

// Or use OpenMRS metadata
Concept queueTypeConcept = Context.getConceptService().getConceptByName("Queue Type");
```

### 2. Interface-Based Design

**Avoid**:
```java
// Hardcoded implementation
Provider provider = getLeastBusyProvider(location);
```

**Prefer**:
```java
// Strategy pattern
ProviderAssignmentStrategy strategy = getStrategy();
Provider provider = strategy.assignProvider(location, providers);
```

### 3. Distribution Agnosticism

**Avoid**:
- UgandaEMR-specific concepts
- UgandaEMR-specific identifier types
- UgandaEMR-specific visit types
- Hardcoded location hierarchies

**Prefer**:
- Configurable concepts (via GP or admin UI)
- Configurable identifier types
- Core OpenMRS visit types
- Flexible location queries (any hierarchy)

### 4. Extensibility

**Design for**:
- Custom queue types via configuration
- Custom identifier sources
- Custom provider assignment strategies
- Custom validation rules
- Custom workflows via hooks/events

---

## API Design

### REST Endpoints

#### Self Check-in
```
POST /ws/rest/v1/patientqueueing/checkin
{
  "patientIdentifier": "12345",           // Generic: any configured identifier
  "identifierType": "nationalId",         // Optional: specify type
  "fromLocation": "uuid",
  "toLocation": "uuid",
  "queueRoom": "uuid",
  "provider": "uuid",                     // Optional: auto-assign if null
  "visitType": "uuid",                    // Optional: use default
  "priority": 1,
  "comment": "Routine checkup"
}

Response:
{
  "uuid": "...",
  "visit": { "uuid": "...", ... },
  "patientQueue": { "uuid": "...", ... },
  "ticketNumber": "20/04/2026-OUT-001",
  "visitCreated": true
}
```

#### Queue Kiosk
```
GET /ws/rest/v1/patientqueueing/kiosk?ticketNumber=20/04/2026-OUT-001

Response:
{
  "uuid": "...",
  "ticketNumber": "20/04/2026-OUT-001",
  "status": "PENDING",
  "displayName": "John Doe",
  "queueType": "PATIENT",
  "locationTo": { ... },
  "queueRoom": { ... },
  "dateCreated": "2026-04-20T10:30:00Z",
  "priority": null,
  "patient": { ... }
}
```

#### Queue Display
```
GET /ws/rest/v1/patientqueueing/display?type=facility&uuid=...&date=today

Response:
{
  "context": {
    "type": "FACILITY",
    "locationUuid": "...",
    "locationName": "Main Hospital"
  },
  "nowServing": [
    { "ticketNumber": "...", "status": "PICKED", ... }
  ],
  "upNext": [
    { "ticketNumber": "...", "status": "PENDING", ... }
  ],
  "stats": {
    "nowServing": 5,
    "waiting": 23,
    "total": 28
  },
  "lastUpdated": "2026-04-20T10:35:00Z"
}
```

#### Non-Patient Queue
```
POST /ws/rest/v1/patientqueueing/nonpatient
{
  "displayName": "Jane Visitor",
  "phoneNumber": "+256...",
  "queueType": "VISITOR",           // Concept-based
  "currentLocation": "uuid",
  "locationTo": "uuid",
  "queueRoom": "uuid",
  "priority": 1,
  "comment": "Visiting patient"
}

GET /ws/rest/v1/patientqueueing/nonpatient?status=WAITING&location=...
```

---

## Migration from UgandaEMR

### For UgandaEMR Users

**Before**:
```java
// UgandaEMR-specific endpoint
POST /ws/rest/v1/selfcheckinpatient
{
  "phoneNumber": "...",
  "nationalId": "...",
  "patientId": "...",
  "patientStatus": "...",     // UgandaEMR-specific
  "visitType": "...",         // UgandaEMR-specific
  ...
}
```

**After**:
```java
// Generic endpoint
POST /ws/rest/v1/patientqueueing/checkin
{
  "patientIdentifier": "...",
  "identifierType": "phoneNumber",
  "fromLocation": "...",
  "toLocation": "...",
  "queueRoom": "...",
  ...
}
```

### Compatibility Layer

Optionally provide a compatibility shim:
```java
// UgandaEMR compatibility resource (deprecated)
@Resource(
    name = RestConstants.VERSION_1 + "/selfcheckinpatient",
    deprecated = "Use /patientqueueing/checkin instead"
)
public class SelfCheckInPatientResourceCompat {
    // Delegates to new generic resource
}
```

---

## Testing Strategy

### Unit Tests
- [ ] All service methods
- [ ] DAO queries
- [ ] Configuration parsing
- [ ] Provider assignment strategies
- [ ] DTO transformations

### Integration Tests
- [ ] End-to-end check-in flow
- [ ] Queue state transitions
- [ ] Multi-user scenarios
- [ ] Database constraints

### REST Tests
- [ ] All endpoints
- [ ] Error handling
- [ ] Validation
- [ ] Representation variants (default, full, ref)

### Performance Tests
- [ ] Concurrent check-ins (100+ users)
- [ ] Large queue displays (1000+ entries)
- [ ] Database query performance

---

## Open Questions

1. **Queue Types**: Should we use Concepts or a new configuration system?
   - **Recommendation**: Concepts for maximum flexibility

2. **Identifier Configuration**: Global Properties vs admin UI vs module config file?
   - **Recommendation**: Start with GP, add UI later

3. **Provider Assignment**: How complex should the strategies be?
   - **Recommendation**: Start with 2-3 simple strategies, make extensible

4. **Visit Creation**: Should we auto-create visits or make it optional?
   - **Recommendation**: Optional via configuration (default: auto-create)

5. **Backward Compatibility**: Should we maintain UgandaEMR compatibility?
   - **Recommendation**: No, provide migration guide instead

---

## Success Criteria

- [ ] All self check-in features migrated to patientqueueing
- [ ] No UgandaEMR-specific hardcoding
- [ ] Configuration via Global Properties
- [ ] REST API fully documented
- [ ] Unit test coverage >80%
- [ ] Integration test suite passing
- [ ] Performance benchmarks met
- [ ] Documentation complete
- [ ] Migration guide published
- [ ] Example configurations provided

---

## Timeline

- **Week 1-2**: Foundation (NonPatientQueue, configuration)
- **Week 3-4**: Self Check-in (identification, check-in service, REST)
- **Week 5-6**: Queue Display & Kiosk (unified DTO, resources)
- **Week 7**: Provider Features (assignment strategies, dashboard)
- **Week 8**: Testing, Documentation, Release

**Total**: 8 weeks

---

## References

- UgandaEMR self checkin branch: `/Users/lubwamasamuel/Projects/mets/ugandaemr/modules/openmrs-module-ugandaemr` (branch: `selfcheckin`)
- Current patientqueueing module: `/Users/lubwamasamuel/Projects/mets/openmrs/modules/openmrs-module-patientqueueing`
- OpenMRS REST Module: https://wiki.openmrs.org/display/docs/REST+Module
- OpenMRS Module Best Practices: https://wiki.openmrs.org/display/docs/Best+Practices+for+Module+Developers
