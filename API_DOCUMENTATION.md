# Fleet Management & Checklist System - API Documentation

The Fleet Management System is a robust backend API designed to manage vehicle shipments, pre-trip inspections, and driver compliance. It features Role-Based Access Control (RBAC), multi-zone checklist templates, and automated time-tracking analytics.

## Core Features
1. **User Management**: Support for multiple roles (ADMIN, DRIVER, SUPERVISOR, SECURITY) with PIN-based authentication.
2. **Vehicle & Shipment Monitoring**: Comprehensive tracking of vehicles, trailers, and active shipments.
3. **Flexible Data Mapping**: All entities (Users, Vehicles, Shipments) support "Fleet Group" mapping by both ID and Name (e.g., "Northern Cape").
4. **Electronic Checklists**: Multi-zone inspections (CAB, FRONT, SIDES, REAR) with 23 pre-seeded critical safety items.
5. **Time & Odometer Analytics**: Automated tracking of duration spent per zone and vehicle odometer maintenance.
6. **Digital Signatures & Status**: Overall inspection status (PASS/FAIL/CONDITIONAL) with automated Shipment status transitions.
7. **Trip Type Validation**: Enforces driver counts for SINGLE (1 driver) and DUO (2 drivers) trips.

---

## Authentication
Authentication is via JWT (JSON Web Token). Include the token in the `Authorization: Bearer <TOKEN>` header for all protected requests.

- **Login**: `POST /api/auth/login`
    - Body: `{ "userId": "admin", "pin": "1234" }`
- **Set/Update PIN**: `POST /api/auth/set-pin`
    - Body: `{ "userId": "0000827008", "pin": "1234" }` (Admin only)

---

## Endpoint Reference

### 1. User Module
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/users` | List all users |
| `GET` | `/api/users/{id}` | Get user by ID |
| `POST` | `/api/users` | Create user (Admin only) |
| `PUT` | `/api/users/{id}` | Update user (Admin only) |
| `DELETE` | `/api/users/{id}` | Delete user (Admin only) |

### 2. Vehicle Module
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/vehicles` | List all vehicles |
| `GET` | `/api/vehicles/{id}` | Get vehicle by ID |
| `POST` | `/api/vehicles` | Create vehicle (Admin only) |
| `PUT` | `/api/vehicles/{id}` | Update vehicle (Admin only) |
| `DELETE` | `/api/vehicles/{id}` | Delete vehicle (Admin only) |

### 3. Shipment Module
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/shipments` | List all shipments |
| `GET` | `/api/shipments/{id}` | Get shipment by ID |
| `POST` | `/api/shipments` | Create shipment. **Validation**: `SINGLE` = 1 driver, `DUO` = 2 drivers. |
| `PUT` | `/api/shipments/{id}` | Update shipment. Same validation as POST. |
| `DELETE` | `/api/shipments/{id}` | Delete shipment (Admin only) |

### 4. Inspection Module (The Workflow)
| Method | Endpoint | Step | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/inspections/start` | **1** | Start inspection. Updates Shipment to `IN_PROGRESS`. Captures `startOdometer`. |
| `GET` | `/api/inspections/checklist/templates` | **2a** | Get available checklist items (optional `?zone=`) |
| `POST` | `/api/inspections/{id}/checklist` | **2b** | Submit the full completed checklist |
| `GET` | `/api/inspections/{id}/checklist` | **2c** | View submitted responses for an inspection |
| `PATCH` | `/api/inspections/{id}/checklist/{code}` | **2d** | Update a specific item (e.g., mark as FIXED) |
| `POST` | `/api/inspections/{id}/end` | **3** | Finish inspection. Updates Shipment to `COMPLETED`/`REJECTED`. Captures `endOdometer` & `notes`. |
| `GET` | `/api/inspections/{id}/analytics` | **Data** | Get total time duration summary per zone |

### 5. Lookups & Meta-data
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/roles` | List all system roles |
| `GET` | `/api/lookups/vehicle-categories` | List vehicle types (Truck, Trailer, etc.) |
| `GET` | `/api/lookups/shipment-statuses` | List shipment lifecycle statuses |

---

## Technical Design Notes
- **Seeding**: On first run, the system automatically seeds 4 Roles, an Admin user, 4 Fleet Groups, and 23 Checklist Templates.
- **Auto-Cleanup**: The system includes a startup cleanup (`fixDatabaseSchema`) to ensure the legacy `fleet_group` column doesn't interfere with new mappings.
- **Mapping Logic**: You can send `"fleetGroup": "Northern Cape"` instead of an ID in requests for Users, Vehicles, and Shipments.
