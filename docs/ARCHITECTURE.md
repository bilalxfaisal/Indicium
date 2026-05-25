# 📐 Architecture

> [← Back to README](../README.md)

## Project Structure

```
indicium/
├── src/main/java/com/indicium/
│   ├── controllers/          # Business logic (CaseManager, EvidenceManager)
│   ├── models/               # Domain models (Case, Evidence, UserAuth...)
│   ├── repository/           # Data access layer (CaseRepository, EvidenceRepo...)
│   ├── services/             # Cross-cutting services
│   │   ├── SessionManager    # Singleton session state
│   │   ├── AccessManager     # Lockdown state management
│   │   ├── AuditLog          # Forensic audit trail
│   │   └── HashGenerator     # SHA-256 file fingerprinting
│   └── ui/                   # JavaFX controllers + FXML + CSS
│       ├── HomeController
│       ├── CaseDashBoardController
│       ├── EvidenceDashBoardController
│       ├── IntegrityManagerDashboardController
│       └── IdentityManagerDashboardController
├── src/main/resources/
│   └── com/indicium/ui/
│       ├── *.fxml            # UI layouts
│       ├── *.css             # Stylesheets
│       └── Assets/           # Icons
├── docs/                     # Documentation
├── schema/
│   └── indicium_schema.sql   # Full PostgreSQL schema
└── screenshots/              # UI screenshots
```

---

## Design Patterns

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Singleton** | `SessionManager`, `AccessManager` | Single source of truth for session and lockdown state |
| **Repository** | `CaseRepository`, `EvidenceRepo` | Decouples data access from business logic |
| **MVC** | All JavaFX controllers + FXML | Separates UI layout from controller logic |
| **RBAC** | Role checks on every sensitive action | Enforces least-privilege access per user role |

---

## System Flow

```
┌──────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  JavaFX UI   │────▶│   Controllers    │────▶│  Repositories   │
│  (FXML/CSS)  │◀────│  (Business Logic)│◀────│  (Data Access)  │
└──────────────┘     └──────────────────┘     └────────┬────────┘
                                                        │
                                              ┌─────────▼────────┐
                                              │  HikariCP Pool   │
                                              └─────────┬────────┘
                                                        │
                                              ┌─────────▼────────┐
                                              │Supabase PostgreSQL│
                                              └──────────────────┘
```

---

## Key Services

### SessionManager
Singleton that holds the currently authenticated user's ID, name, and role for the duration of the session. All role-checks across the app query this.

### AccessManager
Manages the system lockdown state. Persists lockdown status to the database so it survives application restarts. Only `ADMIN` role can toggle.

### AuditLog
Every sensitive action — login, evidence upload, case status change, lockdown — is written to `ForensicAuditLog` with a timestamp, user ID, and category tag.

### HashGenerator
Computes SHA-256 fingerprints for evidence files at upload time. The stored hash is later compared on-demand to detect tampering.
