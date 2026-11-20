# Patient Queueing Module - REST API Documentation

**Version**: 3.0.*
**Base Path**: `/ws/rest/v1`

---

## Table of Contents

1. [Check-in APIs](#check-in-apis)
2. [Queue Display APIs](#queue-display-apis)
3. [Non-Patient Queue Management](#non-patient-queue-management)
4. [Data Models](#data-models)
5. [Error Responses](#error-responses)

---

## Check-in APIs

### Generic Check-in

Generic patient check-in with configurable visit type. Supports provider auto-assignment, queue position calculation, and estimated wait time.

**Endpoint**: `POST /ws/rest/v1/patientqueueing/checkin` — **Requires**: `Task: patientqueueing.sendPatientToQueue` and `Task: patientqueueing.viewQueue` (enforced at the service layer), plus `Get Patients`, `Get Visits` and `Get Visit Types`; creating a visit, when the patient has no open one, additionally needs `Get Visit Attribute Types` and `Add Visits` (the validator reads attribute types); auto-assignment, or a `provider` in the request, additionally needs `Get Providers`; when auto-assignment runs and `patientqueueing.providerLocationAttributeTypeUuid` is not set (or does not resolve), the fallback matches providers by user default location and additionally needs `Get Users` — if that privilege is missing (or no provider matches the location), the check-in still succeeds but the patient is queued unassigned

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| patient | string | Yes | Patient UUID |
| locationTo | string | Yes | Destination Location UUID (the pre-2.8 `location` key is accepted as an alias) |
| locationFrom | string | No | Origin location UUID (defaults to locationTo) |
| visitType | string | No | Visit type UUID (overrides default) |
| queueRoom | string | No | Queue room UUID |
| provider | string | No | Provider UUID (skips auto-assignment) |
| priority | int | No | Priority level |
| comment | string | No | Comment for the queue entry |
| autoAssignProvider | boolean | No | Override auto-assignment setting |

**Response** (built by CheckInResource.create; errors surface as HTTP error codes):
```json
{
  "uuid": "queue-uuid",
  "ticketNumber": "21/04/2026-LOC-001",
  "status": "PENDING",
  "dateCreated": 1776767400000,
  "queuePosition": 5,
  "estimatedWaitMinutes": 25,
  "success": true,
  "patient": {
    "uuid": "patient-uuid",
    "display": "John Doe"
  },
  "visit": {
    "uuid": "visit-uuid",
    "startDatetime": 1776729600000,
    "visitType": "OPD"
  },
  "queueEntry": {
    "uuid": "queue-uuid",
    "visitNumber": "21/04/2026-LOC-001",
    "status": "PENDING",
    "dateCreated": 1776767400000,
    "provider": "provider-uuid-or-null"
  }
}
```

**Example**:
```bash
curl -X POST -H 'Content-Type: application/json' \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/checkin' \
  -d '{
        "patient": "a9e39c4d-1234-5678-9abc-def456789012",
        "locationTo": "b1e39c4d-1234-5678-9abc-def456789013",
        "visitType": "d1e39c4d-1234-5678-9abc-def456789015",
        "queueRoom": "c1e39c4d-1234-5678-9abc-def456789014"
      }'
```

### Self Check-in (Booth/Kiosk)

Self check-in endpoint for booth/kiosk scenarios. Simplified interface for self-service check-in.

**Endpoint**: `POST /ws/rest/v1/patientqueueing/selfcheckin` — **Requires**: `Task: patientqueueing.sendPatientToQueue` when the kiosk identifies a patient, `Task: patientqueueing.checkIn` when it creates a non-patient ticket; visit creation additionally needs `Get Visits`, `Get Visit Types`, `Get Visit Attribute Types` and `Add Visits` when `patientqueueing.selfCheckInVisitTypeUuid` is set, and a `queueTypeUuid` on a non-patient ticket needs `Get Concepts` (see the README's kiosk device-account row)

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| currentLocation | string | Yes | Location UUID the kiosk is at |
| locationTo | string | Yes | Destination location UUID |
| queueRoom | string | Yes | Queue room UUID |
| identifierTypeUuid | string | No | UUID of a configured identifier type (see `patientqueueing.selfCheckInIdentifierTypeUuids` / `...PersonAttributeTypeUuids`); when supplied with identifierValue, a miss is refused rather than falling back |
| identifierValue | string | No | The identifier value to look up |
| phoneNumber | string | No | Matched to a patient via `patientqueueing.phoneNumberAttributeTypeUuid` only when no identifier was supplied |
| displayName | string | Required for non-patient check-in | The visitor's name when no patient is identified |
| queueTypeUuid | string | No | Concept UUID classifying the non-patient ticket |
| priority | int | No | Priority level |

**Example**:
```bash
curl -X POST -H 'Content-Type: application/json' \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/selfcheckin' \
  -d '{
        "displayName": "Jane Smith",
        "currentLocation": "b1e39c4d-1234-5678-9abc-def456789013",
        "locationTo": "b1e39c4d-1234-5678-9abc-def456789013",
        "queueRoom": "c1e39c4d-1234-5678-9abc-def456789014"
      }'
```

**Response**: depends on the outcome — `checkInType` tells them apart. There is no `success`
field; validation and lookup problems surface as HTTP 500 with the reason in `error.message`.

Patient match (identifier or phone resolved to a patient; `patient.display` is masked):
```json
{
  "checkInType": "patient",
  "uuid": "queue-uuid",
  "patientQueue": {
    "uuid": "queue-uuid",
    "visitNumber": "21/04/2026-LOC-001",
    "status": "PENDING",
    "dateCreated": 1776767400000
  },
  "patient": {
    "uuid": "patient-uuid",
    "display": "J. D***"
  },
  "visit": {
    "uuid": "visit-uuid",
    "startDatetime": 1776729600000,
    "stopDatetime": null,
    "visitType": "OPD"
  },
  "ticketNumber": "21/04/2026-LOC-001",
  "visitNumber": "21/04/2026-LOC-001"
}
```
`visit` is `null` when `patientqueueing.selfCheckInVisitTypeUuid` is not set.

No patient identified (non-patient ticket):
```json
{
  "checkInType": "nonPatient",
  "uuid": "non-patient-queue-uuid",
  "displayName": "Jane Smith",
  "phoneNumber": "+1234567890",
  "ticketNumber": "21/04/2026-LOC-TYPE-001",
  "status": "WAITING",
  "nonPatientQueue": {
    "uuid": "non-patient-queue-uuid",
    "displayName": "Jane Smith",
    "phoneNumber": "+1234567890",
    "ticketNumber": "21/04/2026-LOC-TYPE-001",
    "status": "WAITING",
    "dateCreated": 1776767400000
  }
}
```

---

### Non-Patient Check-in

Create a non-patient queue entry (for visitors, companions, etc.).

**Endpoint**: `POST /ws/rest/v1/patientqueueing/nonpatientqueue` — **Requires**: `Task: patientqueueing.checkIn`

**Example**:
```bash
curl -X POST -H 'Content-Type: application/json' \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/nonpatientqueue' \
  -d '{
        "displayName": "Jane Smith",
        "phoneNumber": "+1234567890",
        "queueType": "concept-uuid",
        "currentLocation": "b1e39c4d-1234-5678-9abc-def456789013",
        "locationTo": "b1e39c4d-1234-5678-9abc-def456789013",
        "queueRoom": "c1e39c4d-1234-5678-9abc-def456789014"
      }'
```

**Response**: the created `NonPatientQueue` in its default representation:
```json
{
  "uuid": "non-patient-queue-uuid",
  "displayName": "Jane Smith",
  "phoneNumber": "+1234567890",
  "queueType": null,
  "currentLocation": {"uuid": "location-uuid", "display": "OPD"},
  "locationTo": {"uuid": "location-uuid", "display": "OPD"},
  "queueRoom": {"uuid": "room-uuid", "display": "Room 1"},
  "status": "WAITING",
  "priority": null,
  "comment": null,
  "ticketNumber": "21/04/2026-LOC-TYPE-001",
  "calledBy": null,
  "calledAt": null,
  "arrivedAt": null,
  "servedBy": null,
  "startedAt": null,
  "endedAt": null,
  "links": [ {"rel": "self", "uri": ".../nonpatientqueue/non-patient-queue-uuid"} ]
}
```

---

## Queue Display APIs

### Get Queue by Ticket Number

Look up a queue entry by ticket number. Searches both patient and non-patient queues.

**Endpoint**: `GET /ws/rest/v1/patientqueueing/kiosk` — **Requires**: `Task: patientqueueing.viewQueue`

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ticketNumber | string | Yes | Ticket number to search |
| dateFrom | long | No | Start date (milliseconds, default: start of today) |
| dateTo | long | No | End date (milliseconds, default: end of today) |

**Response**: pageable `results` array; each entry carries exactly what the kiosk lookup builds
(`displayName` is masked):
```json
{
  "results": [
    {
      "ticketNumber": "21/04/2026-LOC-001",
      "status": "PENDING",
      "displayName": "J. D***",
      "dateCreated": "2026-04-21T10:30:00.000+0000"
    }
  ]
}
```
An unknown ticket number answers HTTP 500 with "No queue entry found for ticket number: ...".

**Example**:
```bash
curl -X GET \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/kiosk?ticketNumber=21/04/2026-LOC-001'
```

### Get Patient Journey

Get a ticket's full journey through the facility (completed, current and upcoming services).

**Endpoint**: `GET /ws/rest/v1/patientqueueing/kiosk` — **Requires**: `Task: patientqueueing.viewQueue`

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| journey | string | Yes | Must be `true` |
| ticketNumber | string | Yes | Ticket number to search |

**Example**:
```bash
curl -X GET \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/kiosk?journey=true&ticketNumber=21/04/2026-LOC-001'
```

**Response**: pageable `results` array with the journey as its single entry (`displayName` is
masked; `patientUuid` is always `null`):
```json
{
  "results": [
    {
      "displayName": "J. D***",
  "patientUuid": null,
  "ticketNumber": "21/04/2026-LOC-001",
  "overallStatus": "IN_PROGRESS",
  "completedServices": [
    {
      "uuid": "queue-uuid-0",
      "service": "Triage",
      "room": "Room 1",
      "roomUuid": "room-uuid",
      "status": "COMPLETED",
      "checkedInAt": "2026-04-21T09:00:00Z",
      "type": "PATIENT",
      "completedAt": "2026-04-21T09:25:00Z",
      "durationMinutes": 25
    }
  ],
  "currentService": {
    "uuid": "queue-uuid-1",
    "service": "Outpatient Clinic",
    "room": "Room 2",
    "roomUuid": "room-uuid-2",
    "status": "PENDING",
    "checkedInAt": "2026-04-21T10:30:00Z",
    "type": "PATIENT",
    "position": 2,
    "queueLength": 5,
    "estimatedWaitMinutes": 10
  },
  "nextServices": [
    {
      "uuid": "queue-uuid-2",
      "service": "Laboratory",
      "room": "Lab Room",
      "roomUuid": "room-uuid-3",
      "status": "WAITING",
      "checkedInAt": "2026-04-21T10:35:00Z",
      "type": "NON_PATIENT"
    }
  ]
}
  ]
}
```
`overallStatus` is `COMPLETED` when no service is current, `IN_PROGRESS` otherwise.
`completedServices` entries add `completedAt` and `durationMinutes` when the completion date is
known; `currentService` adds `position`, `queueLength` and `estimatedWaitMinutes` when it is
waiting AND its queue room has waiting stats — `estimatedWaitMinutes` is `null` when no average
service time is known for the room. NO_SHOW, CANCELLED (non-patient
entries only) and TRANSFERRED entries count as terminal and are listed under
`completedServices`.

### Look Up a Single Queue Entry

Get one queue entry (patient or non-patient) by ticket or visit number.

**Endpoint**: `GET /ws/rest/v1/patientqueueing/kiosk` — **Requires**: `Task: patientqueueing.viewQueue`

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| visitNumber | string | Yes | Visit number to search (UgandaEMR-style lookup) |

This section covers `?visitNumber=` requests only. A `?ticketNumber=` request with no
`visitNumber` follows the clean single-lookup format documented under **Get Queue by Ticket
Number** above, and that path — unlike this one — accepts explicit `dateFrom`/`dateTo`. The
window here comes from the date embedded in the number (formats `DD/MM/YYYY-...` or
`YYYY-MM-DD-...`), falling back to today for a bare number.

**Response**: this path is wrapped TWICE — the REST paging layer puts the UgandaEMR-style
object under a paging `results` key, and that object itself holds a `results` array of the
kiosk status objects (queue position and estimated wait on waiting entries; `displayName`
masked):
```json
{
  "results": [
    {
      "results": [
        {
      "uuid": "queue-uuid-1",
      "ticketNumber": "21/04/2026-LOC-001",
      "visitNumber": "21/04/2026-LOC-001",
      "status": "PENDING",
      "queueLocation": "Outpatient Clinic",
      "serviceLocation": "Room 1",
      "checkedInAt": "2026-04-21T10:30:00Z",
      "dateCreated": "2026-04-21T10:30:00Z",
      "datePicked": null,
      "dateCompleted": null,
      "locationTo": { "uuid": "location-uuid", "display": "Outpatient Clinic", "name": "Outpatient Clinic" },
      "queueRoom": { "uuid": "room-uuid", "display": "Room 1", "name": "Room 1" },
      "displayName": "J. D***",
      "queueType": "PATIENT",
      "position": 2,
      "queueLength": 5,
      "estimatedWaitMinutes": 10
    },
    {
      "uuid": "queue-uuid-2",
      "ticketNumber": "21/04/2026-LOC-TYPE-001",
      "status": "WAITING",
      "queueLocation": "Outpatient Clinic",
      "serviceLocation": "Room 1",
      "checkedInAt": "2026-04-21T10:31:00Z",
      "dateCreated": "2026-04-21T10:31:00Z",
      "dateCompleted": null,
      "locationTo": { "uuid": "location-uuid", "display": "Outpatient Clinic", "name": "Outpatient Clinic" },
      "queueRoom": { "uuid": "room-uuid", "display": "Room 1", "name": "Room 1" },
      "displayName": "J. S***",
          "queueType": "NON_PATIENT",
          "position": 1,
          "queueLength": 2,
          "estimatedWaitMinutes": 0
        }
      ]
    }
  ]
}
```


**Example**:
```bash
curl -X GET \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/kiosk?visitNumber=21/04/2026-LOC-001'
```


### Get Queue Display Data

Get display data for public queue monitors, including "now serving" and "up next" information.

**Endpoint**: `GET /ws/rest/v1/patientqueueing/display` — **Requires**: `Task: patientqueueing.viewQueue`

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| type | string | Yes | `facility`, `location` or `room` |
| uuid | string | Yes | UUID of the location for the chosen type |
| date | string | No | `today` (default) or `YYYY-MM-DD` |

**Response**: built as `context` / `nowServing` / `upNext` / `stats` / `lastUpdated`
(`displayName` is masked on every row):
```json
{
  "results": [
    {
      "context": {
        "type": "LOCATION",
        "locationUuid": "location-uuid",
        "locationName": "Outpatient Clinic"
      },
  "nowServing": [
    {
      "uuid": "queue-uuid",
      "ticketNumber": "21/04/2026-LOC-001",
      "status": "PICKED",
      "displayName": "J. D***",
      "queueLocation": "Outpatient Clinic",
      "serviceLocation": "Room 1",
      "dateCreated": 1776767400000,
      "priority": 1,
      "priorityScore": null,
      "priorityReason": null
    }
  ],
  "upNext": [
    {
      "uuid": "queue-uuid-2",
      "ticketNumber": "21/04/2026-LOC-002",
      "status": "PENDING",
      "displayName": "J. S***",
      "queueLocation": "Outpatient Clinic",
      "serviceLocation": "Room 1",
      "dateCreated": 1776767460000,
      "priority": null,
      "priorityScore": null,
      "priorityReason": null
    }
  ],
  "stats": {
    "nowServingCount": 1,
    "upNextCount": 4,
    "totalCount": 5
  },
      "lastUpdated": "2026-04-21T10:50:00.000+0000"
    }
  ]
}
```

**Example**:
```bash
curl -X GET \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/display?type=location&uuid=b1e39c4d-1234-5678-9abc-def456789013'
```

## Non-Patient Queue Management

### List All Non-Patient Queues

**Requires**: `Task: patientqueueing.viewQueue`; a `queueType` filter additionally needs `Get Concepts`

**Endpoint**: `GET /ws/rest/v1/patientqueueing/nonpatientqueue`

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| status | string | No | Filter by status — any of WAITING, CALLED, ARRIVED, SERVING, COMPLETED, SKIPPED, CANCELLED, HOLD, NO_SHOW, TRANSFERRED |
| queueType | string | No | Filter by queue type Concept UUID |
| locationTo | string | No | Filter by destination location UUID |
| queueRoom | string | No | Filter by queue room UUID |
| fromDate | long | No | Start date (milliseconds) |
| toDate | long | No | End date (milliseconds) |

COMPLETED entries are omitted unless `includeHistory=true`, and with no
`fromDate`/`toDate` the window defaults to today — a reporting client must pass explicit dates
or it never sees completed tickets.

**Response**: paged list of `NonPatientQueue` under the standard `results` envelope

**Example**:
```bash
curl -X GET \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/nonpatientqueue?status=WAITING&queueRoom=c1e39c4d-1234-5678-9abc-def456789014'
```

### Get Non-Patient Queue by UUID

**Requires**: `Task: patientqueueing.viewQueue`

**Endpoint**: `GET /ws/rest/v1/patientqueueing/nonpatientqueue/:uuid`

**Response**: `NonPatientQueue` (default representation; `v=full` adds audit fields)

**Example**:
```bash
curl -X GET \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/nonpatientqueue/a1e39c4d-1234-5678-9abc-def456789015'
```

### Create Non-Patient Queue

**Endpoint**: `POST /ws/rest/v1/patientqueueing/nonpatientqueue`

**Request Body**:
```json
{
  "displayName": "Jane Smith",
  "phoneNumber": "+1234567890",
  "queueType": "concept-uuid",
  "currentLocation": "location-uuid",
  "locationTo": "location-uuid",
  "queueRoom": "room-uuid",
  "priority": 1,
  "comment": "Urgent case"
}
```

Same endpoint and request as [Non-Patient Check-in](#non-patient-check-in), which holds the
full request/response documentation. **Requires**: `Task: patientqueueing.checkIn`; a
`queueType` additionally needs `Get Concepts`.

### Update Non-Patient Queue

**Requires**: `Task: patientqueueing.manageQueue` (plus `Task: patientqueueing.viewQueue` to resolve the entry)

**Endpoint**: `POST /ws/rest/v1/patientqueueing/nonpatientqueue/:uuid`

**Request Body** (all fields optional):
```json
{
  "displayName": "Jane Smith Updated",
  "phoneNumber": "+0987654321",
  "priority": 2,
  "comment": "Updated priority"
}
```

**Response**: Updated `NonPatientQueue`

**Example**:
```bash
curl -X POST -H 'Content-Type: application/json' \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/nonpatientqueue/a1e39c4d-1234-5678-9abc-def456789015' \
  -d '{"priority": 2, "comment": "Updated priority"}'
```

### Search Non-Patient Queues

**Requires**: `Task: patientqueueing.viewQueue`; a `queueType` filter additionally needs `Get Concepts`

**Endpoint**: `GET /ws/rest/v1/patientqueueing/nonpatientqueue?q=search`

**Search Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| ticketNumber | string | Search by ticket number |
| queueRoom | string | Search by queue room UUID |
| status | string | Filter by status |
| fromDate | long | Start date (milliseconds) |
| toDate | long | End date (milliseconds) |
| queueType | string | No | Queue type concept UUID |
| locationTo | string | No | Destination location UUID |

As with the list endpoint, COMPLETED entries are omitted unless `includeHistory=true`, and the
window defaults to today when no dates are given.

**Response**: paged list of matching `NonPatientQueue` under the standard `results` envelope

**Example**:
```bash
curl -X GET \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/nonpatientqueue?q=search&ticketNumber=21/04/2026-LOC-TYPE-001'
```

---

## Provider Dashboard

The unified dashboard resource for a queue room: one list covering patient and non-patient
entries, and the status transitions (call, hold, recall, skip, no-show, transfer, complete)
enforced by the service's transition rules.

### List a Room's Queue

**Endpoint**: `GET /ws/rest/v1/patientqueueing/providerqueuebylocation?uuid=<roomUuid>`

**Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| uuid | string | Yes | UUID of the queue room location |

**Response**: paged list of `ProviderQueueEntry` under the standard `results` envelope (patient and non-patient entries merged,
sorted by status, then priority high-to-low, then FIFO). Each entry carries:
`uuid`, `queueNumber`, `displayName`, `identifiedBy`, `currentLocation`, `serviceLocation`,
`status`, `priority`, `dateCreated`, `patient`, `sourceType` (`PATIENT_QUEUE` or `NON_PATIENT_QUEUE`),
`locationToUuid`, `queueRoomUuid`, `comment`, `action`.

**Returned status values**: `WAITING`, `CALLED`, `ARRIVED`, `IN_SERVICE`, `HOLD`, `NO_SHOW`,
`COMPLETED`, `TRANSFERRED`, `CANCELLED` (`ARRIVED`/`CANCELLED` are non-patient only; patient
`PENDING`/`PICKED`/`ON_HOLD` are normalized to `WAITING`/`CALLED`/`HOLD`).

**Requires**: `Task: patientqueueing.viewQueue`

### Update an Entry (call, hold, recall, skip, no-show, complete)

OpenMRS REST takes updates as **POST** to the resource's uuid (`PUT …/<uuid>` answers 404).

**Endpoint**: `POST /ws/rest/v1/patientqueueing/providerqueuebylocation/:uuid`

**Request Body** (all fields optional):
```json
{
  "status": "CALLED",
  "priority": 2,
  "comment": "Called to consultation room",
  "sourceType": "PATIENT_QUEUE"
}
```

- `status` accepts `WAITING` (skip back to waiting), `CALLED`, `ARRIVED` (non-patient only,
  between CALLED and IN_SERVICE), `IN_SERVICE`, `HOLD`, `NO_SHOW`, `CANCELLED` (non-patient
  only) and `COMPLETED`. Transitions that are illegal from the entry's current status are
  rejected with 500 — e.g. calling a transferred entry, or serving a ticket that is only
  CALLED (non-patient tickets must be marked ARRIVED first).
- `sourceType`: `PATIENT_QUEUE` for patient entries; `NON_PATIENT_QUEUE` or omitted for non-patient entries. Only
  needed when the uuid alone cannot disambiguate.

**Requires**: `Task: patientqueueing.manageQueue`


**Response**: the updated entry as ProviderQueueResource serializes it (the 15 fields the list
above documents — uuid, queueNumber, displayName, identifiedBy, currentLocation, serviceLocation,
status, priority, dateCreated, patient, sourceType, locationToUuid, queueRoomUuid, comment,
action). The response carries no per-action timestamps; the effects of an action (the new status,
the transferred row) are read from these fields, by a single-entry GET
`/patientqueueing/providerqueuebylocation/{uuid}` (status-only effects), or by re-fetching the
list after a FORWARD (the new row's uuid is only in the list).

**Example**:
```bash
curl -X POST -H 'Content-Type: application/json' \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/providerqueuebylocation/a1e39c4d-1234-5678-9abc-def456789015' \
  -d '{"status": "CALLED", "sourceType": "PATIENT_QUEUE"}'
```

### Transfer an Entry (FORWARD)

**Endpoint**: `POST /ws/rest/v1/patientqueueing/providerqueuebylocation/:uuid`

Send `action: "FORWARD"` with the destination: the current entry is closed as TRANSFERRED
(keeping its source room for history) and a new entry is created at the destination. Both
`locationToUuid` and `queueRoomUuid` must name the destination in the request — the request
body is overlaid on the entry, so omitting them would reuse the source room and misroute
the patient.

**Request Body**:
```json
{
  "action": "FORWARD",
  "sourceType": "PATIENT_QUEUE",
  "locationToUuid": "b1e39c4d-1234-5678-9abc-def456789013",
  "queueRoomUuid": "c1e39c4d-1234-5678-9abc-def456789014",
  "comment": "Sent to laboratory"
}
```

**Response**: the new entry at the destination (status `WAITING`)

**Example**:
```bash
curl -X POST -H 'Content-Type: application/json' \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/providerqueuebylocation/a1e39c4d-1234-5678-9abc-def456789015' \
  -d '{"action": "FORWARD", "sourceType": "PATIENT_QUEUE", "locationToUuid": "b1e39c4d-1234-5678-9abc-def456789013", "queueRoomUuid": "c1e39c4d-1234-5678-9abc-def456789014"}'
```

---

## Data Models

### QueueEntry

Unified representation of both patient and non-patient queues.

| Field | Type | Description |
|-------|------|-------------|
| uuid | string | Queue entry UUID |
| ticketNumber | string | Unique ticket/visit number |
| status | string | Current status |
| displayName | string | Human-readable name |
| queueType | string | "PATIENT" or "NON_PATIENT" |
| locationTo | Location | Destination location |
| queueRoom | Location | Queue room location |
| dateCreated | Date | Creation timestamp |
| priority | Integer | Priority level (optional) |
| comment | string | Additional notes (optional) |
| isPatientQueue | Boolean | True if patient queue |

### CheckInResult (POST /patientqueueing/checkin)

Result of a generic check-in operation.

| Field | Type | Description |
|-------|------|-------------|
| uuid | string | Created queue entry UUID |
| ticketNumber | string | Assigned ticket number |
| status | string | Entry status (PENDING) |
| dateCreated | Date | Creation timestamp |
| queuePosition | Integer | Position in queue (1-based; count includes this entry) |
| estimatedWaitMinutes | Integer | (queuePosition - 1) × estimatedWaitMinutesPerPerson |
| success | Boolean | True if successful |
| patient | object | `{uuid, display}` |
| visit | object | `{uuid, startDatetime, visitType}` |
| queueEntry | object | `{uuid, visitNumber, status, dateCreated, provider}` |

### NonPatientQueue

Non-patient queue entry.

| Field | Type | Description |
|-------|------|-------------|
| uuid | string | Queue UUID |
| ticketNumber | string | Unique ticket number |
| displayName | string | Display name |
| phoneNumber | string | Contact phone |
| queueType | Concept | Queue type concept |
| currentLocation | Location | Current location |
| locationTo | Location | Destination location |
| queueRoom | Location | Queue room |
| status | enum | WAITING, CALLED, ARRIVED, SERVING, COMPLETED, SKIPPED, CANCELLED, HOLD, NO_SHOW, TRANSFERRED |
| priority | Integer | Priority level |
| comment | string | Notes |
| calledBy | Provider | Provider who called |
| calledAt | Date | Call timestamp |
| arrivedAt | Date | Arrival timestamp |
| servedBy | Provider | Serving provider |
| startedAt | Date | Service start timestamp |
| endedAt | Date | End timestamp |

---

## Error Responses

### Self-checked problem (HTTP 500)

The check-in, kiosk and self check-in resources answer the problems they check themselves with
HTTP 500 and the reason in the message — not 400:

```json
{
  "error": {
    "message": "[patient is required]",
    "code": "CheckInResource:414",
    "detail": {},
    "rawMessage": "patient is required",
    "translatedMessage": "patient is required"
  }
}
```
The `message` wraps the reason in brackets and `code` names the throwing class and line; prefer
`rawMessage`/`translatedMessage` when displaying it.

### Not Found (404)

```json
{
  "error": {
    "message": "Patient not found",
    "code": "not.found"
  }
}
```

### Server Error (500)

```json
{
  "error": {
    "message": "Check-in failed: Database connection error",
    "code": "server.error"
  }
}
```

---

## Representations

All resources support multiple representations:

- **default** (ref): Basic fields with references
- **full**: All fields including complete objects

Use `v=full` parameter:
```bash
curl -X GET \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/nonpatientqueue/:uuid?v=full'
```

---

## Pagination

List endpoints support pagination:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| startIndex | int | 0 | Starting index |
| limit | int | 100 | Max results per page |

**Example**:
```bash
curl -X GET \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/nonpatientqueue?startIndex=0&limit=20'
```

---

## Authentication

All endpoints require valid OpenMRS authentication. Include session cookie or basic auth headers:

```bash
# Session authentication
curl -b 'JSESSIONID=your-session-id' \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/kiosk'

# Basic authentication
curl -u 'admin:Admin123' \
  'http://localhost:8080/openmrs/ws/rest/v1/patientqueueing/kiosk'
```

---

## Rate Limiting

No rate limiting is currently enforced. Clients should implement reasonable polling intervals (recommended: 5-10 seconds for display updates).

---

## Best Practices

1. **Date formats differ by endpoint**: the raw create responses (`/checkin`, `/selfcheckin`) and the `/display` row dates serialize as epoch milliseconds (`lastUpdated` on the display payload is an ISO string); the kiosk lookups (`?ticketNumber=`, `?visitNumber=`) and the journey (`?journey=true`) format dates as ISO 8601 strings
2. **Always handle errors**: go by the HTTP status. `/checkin`, `/selfcheckin` and `/kiosk` answer the problems they check themselves (an unknown patient, location or identifier, or a missing required field) with HTTP 500 and the reason in `error.message`. `success` on `/checkin` is always `true`
3. **Use polling for displays**: Don't poll more frequently than every 5 seconds
4. **Filter by location**: Always provide location/queueRoom filters when possible
5. **Use date ranges**: Specify dateFrom/dateTo for better performance
6. **Handle queue types**: Be prepared for both PATIENT and NON_PATIENT entries
7. **Respect priority**: Higher priority numbers = higher priority (if used)
8. **Cache display data**: Queue displays can cache data for 5-10 seconds

---

## Changelog

### Version 3.0.0-SNAPSHOT (Current)

The README's changelog is canonical; this release requires OpenMRS Platform 2.8.x and the
privilege grants listed under Privileges & Roles there.
- ✅ Added self check-in endpoints
- ✅ Added unified queue display APIs
- ✅ Added non-patient queue management
- ✅ Added provider auto-assignment strategies
- ✅ Distribution-agnostic configuration

### Version 2.0.1
- Initial patient queue management
- Basic REST APIs for patient queues
