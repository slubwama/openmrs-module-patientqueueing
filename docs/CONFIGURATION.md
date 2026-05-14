# Patient Queueing Module Configuration

This document describes the configuration options for the Patient Queueing Module, including self check-in features.

## Global Properties

### Provider Assignment

#### `patientqueueing.providerAssignmentStrategy`

**Description**: Strategy to use when automatically assigning providers to queues

**Valid Values**:
- `leastBusy` - Assign the provider with the fewest active queues (default)
- `roundRobin` - Rotate through available providers in order
- `random` - Randomly assign a provider
- `manual` - No automatic assignment; provider must be specified manually

**Example**:
```properties
patientqueueing.providerAssignmentStrategy=leastBusy
```

---

### Visit Management

#### `patientqueueing.autoCreateVisit`

**Description**: Automatically create a visit when a patient checks in

**Valid Values**:
- `true` - Automatically create visits (default)
- `false` - Do not create visits automatically

**Example**:
```properties
patientqueueing.autoCreateVisit=true
```

#### `patientqueueing.defaultVisitTypeUuid`

**Description**: UUID of the default visit type to use when auto-creating visits

**Valid Values**: Any valid visit type UUID

**Example**:
```properties
patientqueueing.defaultVisitTypeUuid=7b0f5697-27e3-4c28-ab42-4d4b4e9b6f6c
```

---

### Queue Settings

#### `patientqueueing.defaultPriority`

**Description**: Default priority level for new queue entries

**Valid Values**: Integer from 1 (highest) to 10 (lowest)

**Default**: `5`

**Example**:
```properties
patientqueueing.defaultPriority=5
```

---

### Patient Identification

#### `patientqueueing.patientIdentifierTypes`

**Description**: Comma-separated list of patient identifier type UUIDs to use for patient lookup during self check-in

**Valid Values**: Comma-separated list of valid patient identifier type UUIDs

**Example**:
```properties
patientqueueing.patientIdentifierTypes=f0c16a6d-dc5f-4118-a803-616d0075d282,e1731641-30ab-102d-86b0-7a5022ba4115
```

**Note**: Identifiers are searched in the order specified. The first matching patient is returned.

#### `patientqueueing.phoneNumberAttributeTypeUuid`

**Description**: UUID of the person attribute type used for phone number lookup

**Valid Values**: Any valid person attribute type UUID

**Example**:
```properties
patientqueueing.phoneNumberAttributeTypeUuid=14d4f066-15f5-102d-96e4-000c29c2a5d7
```

---

### Non-Patient Queue Types

#### `patientqueueing.defaultQueueTypeConceptUuid`

**Description**: UUID of the default concept to use for non-patient queue types

**Valid Values**: Any valid concept UUID

**Example**:
```properties
patientqueueing.defaultQueueTypeConceptUuid=12345678-1234-1234-1234-123456789012
```

#### `patientqueueing.nonPatientQueueTypeConceptUuids`

**Description**: Comma-separated list of concept UUIDs that represent valid non-patient queue types

**Valid Values**: Comma-separated list of valid concept UUIDs

**Example**:
```properties
patientqueueing.nonPatientQueueTypeConceptUuids=12345678-1234-1234-1234-123456789012,87654321-4321-4321-4321-210987654321
```

**Note**: These concepts should be created in the concept dictionary and can represent any queue type (Registration, Payment, Visitor, etc.)

---

## Setting Global Properties

### Via Admin UI

1. Navigate to **Administration** → **System Administration**
2. Click on **Global Properties**
3. Search for properties starting with `patientqueueing.`
4. Edit and save

### Via SQL

```sql
INSERT INTO global_property (property, property_value, datatype, uuid, description)
VALUES ('patientqueueing.providerAssignmentStrategy', 'leastBusy', 'java.lang.String', UUID(), 'Provider assignment strategy for patient queues');
```

### Via Module Configuration File

Add to your OpenMRS runtime properties file:

```properties
# Provider Assignment
patientqueueing.providerAssignmentStrategy=leastBusy

# Visit Management
patientqueueing.autoCreateVisit=true
patientqueueing.defaultVisitTypeUuid=7b0f5697-27e3-4c28-ab42-4d4b4e9b6f6c

# Queue Settings
patientqueueing.defaultPriority=5

# Patient Identification
patientqueueing.patientIdentifierTypes=f0c16a6d-dc5f-4118-a803-616d0075d282
patientqueueing.phoneNumberAttributeTypeUuid=14d4f066-15f5-102d-96e4-000c29c2a5d7
```

---

## Concept-Based Queue Types

Non-patient queue types are represented as OpenMRS Concepts for maximum flexibility. This allows distributions to:

1. **Define custom queue types** without code changes
2. **Support translations** through concept dictionary
3. **Map queue types** to other coding systems
4. **Re-use existing concepts** for interoperability

### Recommended Queue Type Concepts

Create these concepts in your concept dictionary:

| Concept Name | Description | Class | Datatype |
|--------------|-------------|-------|----------|
| Queue Type | Non-patient queue types | Misc | N/A |
| Registration | Patient registration queue | Misc | N/A |
| Payment | Payment/billing queue | Misc | N/A |
| Visitor | Visitor queue | Misc | N/A |
| Records | Medical records queue | Misc | N/A |
| Administration | Administrative services queue | Misc | N/A |
| Other | Other/miscellaneous queue | Misc | N/A |

### Example: Creating Queue Type Concepts

```sql
-- Create parent concept
INSERT INTO concept (retired, datatype_id, class_id, creator, date_created, uuid, name, short_name, description, version, display_name)
VALUES (0, 4, 33, 1, NOW(), UUID(), 'Queue Type', 'Queue Type', 'Types of non-patient queues', '1.0', 'Queue Type');

-- Create specific queue types (assuming parent concept_id = 1000)
INSERT INTO concept (retired, datatype_id, class_id, creator, date_created, uuid, name, short_name, description, version, display_name)
VALUES
  (0, 4, 33, 1, NOW(), UUID(), 'Registration', 'Registration', 'Patient registration queue', '1.0', 'Registration'),
  (0, 4, 33, 1, NOW(), UUID(), 'Payment', 'Payment', 'Payment and billing queue', '1.0', 'Payment'),
  (0, 4, 33, 1, NOW(), UUID(), 'Visitor', 'Visitor', 'Visitor queue', '1.0', 'Visitor');
```

---

## Provider Assignment Strategies

### Least Busy (Default)

Assigns the provider with the fewest active patient queues.

**Best for**:
- Balancing workload evenly among providers
- Minimizing patient wait times
- High-volume clinics

**Configuration**:
```properties
patientqueueing.providerAssignmentStrategy=leastBusy
```

### Round Robin

Rotates through available providers in order.

**Best for**:
- Fair distribution regardless of queue length
- Predictable assignment patterns
- When all providers have similar capacity

**Configuration**:
```properties
patientqueueing.providerAssignmentStrategy=roundRobin
```

### Random

Randomly selects from available providers.

**Best for**:
- Simple load distribution
- When provider capacity is similar
- Testing and development

**Configuration**:
```properties
patientqueueing.providerAssignmentStrategy=random
```

### Manual

No automatic assignment. Provider must be specified explicitly when creating queues.

**Best for**:
- Complete control over provider assignment
- Specialized provider assignments
- When providers have specific expertise

**Configuration**:
```properties
patientqueueing.providerAssignmentStrategy=manual
```

---

## Patient Identification Configuration

The self check-in feature supports flexible patient identification through multiple identifier types.

### Supported Identifier Sources

1. **Patient Identifiers**: Primary identifiers, medical record numbers, national IDs, etc.
2. **Person Attributes**: Phone numbers, email addresses, etc.

### Configuration Example

```properties
# Search order: National ID → Patient ID → Phone Number
patientqueueing.patientIdentifierTypes=f0c16a6d-dc5f-4118-a803-616d0075d282,e1731641-30ab-102d-86b0-7a5022ba4115
patientqueueing.phoneNumberAttributeTypeUuid=14d4f066-15f5-102d-96e4-000c29c2a5d7
```

### Lookup Process

During self check-in, the system searches for patients in this order:

1. **Primary Identifiers**: Searches configured patient identifier types in order
2. **Person Attributes**: Falls back to configured person attributes (e.g., phone number)
3. **Multiple Matches**: Returns error if multiple patients match
4. **No Match**: Returns error if no patient found

---

## Migration from UgandaEMR

If you're migrating from the UgandaEMR module's self check-in feature, update your configuration:

### Before (UgandaEMR)

Hardcoded in code:
- Phone Number attribute UUID: `14d4f066-15f5-102d-96e4-000c29c2a5d7`
- National ID identifier UUID: `f0c16a6d-dc5f-4118-a803-616d0075d282`
- Patient ID identifier UUID: `e1731641-30ab-102d-86b0-7a5022ba4115`
- Hardcoded queue types: REGISTRATION, VISITOR, ADMIN, PAYMENT, RECORDS, OTHER

### After (Patient Queueing)

Configure via global properties:

```properties
# Patient Identifiers (generic, configurable)
patientqueueing.patientIdentifierTypes=f0c16a6d-dc5f-4118-a803-616d0075d282,e1731641-30ab-102d-86b0-7a5022ba4115
patientqueueing.phoneNumberAttributeTypeUuid=14d4f066-15f5-102d-96e4-000c29c2a5d7

# Queue Types (Concept-based, flexible)
patientqueueing.defaultQueueTypeConceptUuid=your-queue-type-concept-uuid
patientqueueing.nonPatientQueueTypeConceptUuids=registration-concept,visitor-concept,admin-concept,payment-concept,records-concept,other-concept
```

### Migration Steps

1. **Create Queue Type Concepts**: Add concepts for each queue type you need
2. **Update Global Properties**: Configure the new property names
3. **Update API Endpoints**: Change from `/ws/rest/v1/selfcheckinpatient` to `/ws/rest/v1/patientqueueing/checkin`
4. **Test Thoroughly**: Verify patient lookup and queue creation works as expected

---

## Troubleshooting

### Provider Assignment Not Working

**Problem**: Providers are not being assigned automatically

**Solutions**:
1. Check global property: `patientqueueing.providerAssignmentStrategy`
2. Verify providers are associated with the location
3. Ensure providers are not retired
4. Check logs for errors

### Patient Lookup Failing

**Problem**: Patients cannot be found during self check-in

**Solutions**:
1. Verify identifier type UUIDs are correct
2. Check that patients have the specified identifiers
3. Ensure identifiers are not voided
4. Verify person attribute type UUID is correct

### Queue Types Not Available

**Problem**: Non-patient queue types are missing

**Solutions**:
1. Create queue type concepts in the concept dictionary
2. Configure `patientqueueing.nonPatientQueueTypeConceptUuids`
3. Verify concepts are not retired

---

## API Reference

For detailed API documentation, see:
- [Self Check-in API](docs/API.md#self-check-in)
- [Non-Patient Queue API](docs/API.md#non-patient-queue)
- [Provider Assignment API](docs/API.md#provider-assignment)

---

## Additional Resources

- [OpenMRS Global Properties](https://wiki.openmrs.org/display/docs/Global+Properties)
- [OpenMRS Concept Dictionary](https://wiki.openmrs.org/display/docs/Concept+Source)
- [Patient Queueing Module README](../README.md)
- [Self Check-in Migration Plan](SELF_CHECKIN_MIGRATION_PLAN.md)
