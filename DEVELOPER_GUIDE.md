# FleetCheck Pro - Project & API Documentation

## Project Overview
FleetCheck Pro is a comprehensive Fleet Management and Inspection System designed to streamline vehicle inspections, shipment tracking, and user management. The system ensures that all vehicles are inspected periodically and that shipments are handled by authorized personnel.

### Core Features:
- **Duo Drivers**: Support for "Single" or "Duo" trips, capturing both primary and co-drivers for enhanced safety and compliance.
- **Defect Tracking**: Granular tracking of "FAIL" responses, including mandatory photos and a "Fixed" status for follow-up verification.
- **Time Tracking**: Precision timing of each inspection zone and checklist item to ensure thoroughness.
- **Security**: Secured with JWT (JSON Web Token) authentication and PIN-based login.

## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Persistence**: JPA / Hibernate with PostgreSQL (or H2 for dev)
- **Security**: Spring Security 6.x with JWT
- **Configuration**: Dotenv for environment variable management
- **Documentation**: Postman Collections (provided)

---

## Data Models (Entities)
| Entity | Description |
| :--- | :--- |
| **User** | Represents personnel (ID, Name, Role, PIN hash, Fleet Group). |
| **Vehicle** | Represents fleet assets (ID, Registration, Category, **4 Photos: Front, Back, Left, Right**). |
| **VehicleCategory** | Lookups for vehicle types (e.g., TT, RIGID). |
| **Shipment** | Links a driver, optional **co-driver**, vehicle, optional **trailer**, and **tripType** (SINGLE/DUO). |
| **ShipmentStatus** | Lookups for shipment states (e.g., DISPATCHED, COMPLETED). |
| **Inspection** | Records an inspection event for a vehicle and **trailer**, including GPS, signatures, and overall status. |
| **ChecklistTemplate** | Pre-defined inspection items categorized by Zone (e.g., GAUGES, TYRES). |
| **ChecklistItem** | The actual response (YES/NO/NA), **photoUrl**, **isFixed** status, and **Timing** (Start/End). |

---

## API Endpoints

### 1. Authentication (`/api/auth`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/login` | Authenticate with `userId` and `pin`. Returns JWT token. |
| `POST` | `/set-pin` | Set or reset a user's PIN (used after user creation). |

### 2. User Management (`/api/users`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Retrieve all users. |
| `GET` | `/{id}` | Retrieve a specific user by ID. |
| `POST` | `/` | Create a new user (with role and group). |
| `PUT` | `/{id}` | Update an existing user. |
| `DELETE` | `/{id}` | Remove a user from the system. |

### 3. Inspection System (`/api/inspections`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Retrieve all inspection records. |
| `POST` | `/start` | Step 1: Initialize a new inspection for a shipment. |
| `GET` | `/checklist/templates` | Retrieve the inspection checklist (optional `zone` filter). |
| `POST` | `/{id}/checklist` | Step 2: Submit all checklist responses for an inspection (incl. `photoUrl`, `startTime`, `endTime`). |
| `PATCH` | `/{id}/checklist/{itemCode}` | **Follow-up**: Update a specific checklist item (e.g., mark as `isFixed`). |
| `GET` | `/{id}/checklist` | Retrieve submitted responses for a specific inspection. |
| `POST` | `/{id}/end` | Step 3: Complete inspection with signatures and final status. |
| `GET` | `/{id}/verify` | **Gate Pass**: Quick verification for Security to check the inspection status. |
| `DELETE` | `/{id}` | Delete an inspection record. |

### 4. Shipment Management (`/api/shipments`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Retrieve all shipments. |
| `GET` | `/{id}` | Retrieve a specific shipment by ID. |
| `POST` | `/` | Create a new shipment (assign driver/vehicle). |
| `PUT` | `/{id}` | Update shipment details. |
| `DELETE` | `/{id}` | Delete a shipment record. |

### 5. Vehicle Management (`/api/vehicles`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Retrieve all vehicles in the fleet. |
| `GET` | `/{id}` | Retrieve a specific vehicle by ID. |
| `POST` | `/` | Add a new vehicle to the fleet. |
| `PUT` | `/{id}` | Update vehicle details. |
| `DELETE` | `/{id}` | Remove a vehicle from the fleet. |

### 6. Lookup & Configuration (`/api/lookups`, `/api/roles`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/roles` | List all available user roles (e.g., ADMIN, DRIVER). |
| `POST` | `/api/roles` | Create a new role definition. |
| `GET` | `/api/lookups/shipment-statuses` | List shipment statuses (e.g., PENDING, LOADED). |
| `GET` | `/api/lookups/vehicle-categories` | List vehicle types (e.g., FLATBED, TANKER). |

---

## Core Use Cases & API Flows

### 1. Daily Vehicle Inspection Flow
This is the primary workflow for drivers reporting for duty.

1.  **Start Inspection**: `POST /api/inspections/start`
    - Provide the `shipmentId` and current `gpsLocation`.
    - Returns an `inspectionId` (UUID) with linked **Truck** and **Trailer**.
2.  **Fetch Checklist**: `GET /api/inspections/checklist/templates`
    - Dynamically builds the UI based on mandated checks (e.g., GAUGES, TYRES).
3.  **Submit Responses**: `POST /api/inspections/{id}/checklist`
    - Sends an array of responses (itemCode, response, remarks, **photoUrl**).
    - System flags any "FAIL" items.
4.  **End & Sign**: `POST /api/inspections/{id}/end`
    - Submit final signatures (Driver, Supervisor, Security).
    - Returns the final `overall_status` (PASS/FAIL).
5.  **Verify (Security Scan)**: `GET /api/inspections/{id}/verify`
    - Security scans the QR (inspectionId) to verify the "PASS" status before exit.

### 2. User & PIN Onboarding
1.  **Admin Creates User**: `POST /api/users`
    - Creates the base record with `userId`, `username`, and `role`.
2.  **User Sets PIN**: `POST /api/auth/set-pin`
    - Standard onboarding step where the user sets their 4-digit PIN for the first time.
3.  **Authentication**: `POST /api/auth/login`
    - User logs in with `userId` and `pin` to receive a JWT.

### 3. Shipment Lifecycle Management
1.  **Create Shipment**: `POST /api/shipments`
    - Assigns a `shipmentId`, a `driver`, a `vehicle` (Truck), and a **trailer**.
2.  **Update Status**: `PUT /api/shipments/{id}`
    - As the shipment progresses, update its status (e.g., from PENDING to DISPATCHED).

---

## Postman Collections
The project includes two Postman collections for testing and integration:
1. `FleetCheck_Pro.postman_collection.json`: Main collection for the core API (Secured).
2. `ERP_HRM_System.postman_collection.json`: Reference collection for HRM integration (if applicable).

---
*Generated by Antigravity AI*
