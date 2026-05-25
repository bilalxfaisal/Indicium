# 🔒 Security

> [← Back to README](../README.md)

Indicium is engineered with forensic-grade security at every layer — from application-level RBAC down to database-level Row Level Security policies.

---

## Application Security

### Authentication
- Passwords are hashed using **SHA-256** before storage — plaintext is never persisted
- Login attempts are validated against the stored hash only
- New users are provisioned with a **temporary password** that forces a reset on first login

### Role-Based Access Control (RBAC)

| Action | ADMIN | INVESTIGATOR |
|--------|-------|--------------|
| View cases | ✅ | ✅ |
| Create / edit cases | ✅ | ✅ |
| Lock / archive cases | ✅ | ❌ |
| Upload evidence | ✅ | ✅ |
| Delete / discard evidence | ✅ | ❌ |
| View audit log | ✅ | ❌ |
| Manage users | ✅ | ❌ |
| Trigger system lockdown | ✅ | ❌ |

### Emergency Lockdown
- Admin-only kill switch that **terminates all active non-admin sessions**
- Requires a **secondary security code** to activate — prevents accidental triggers
- Lockdown state is **persisted to the database** — survives app restarts
- Every lockdown and lift event is written to `ForensicAuditLog`

---

## Database Security

### Row Level Security (RLS)

RLS is enabled on all core tables. Restrictive policies explicitly deny all access via Supabase's auto-generated REST and GraphQL APIs — the database is **only accessible through the authorized JDBC application binary**.

```sql
-- Enable RLS on all core tables
ALTER TABLE users           ENABLE ROW LEVEL SECURITY;
ALTER TABLE cases           ENABLE ROW LEVEL SECURITY;
ALTER TABLE evidence        ENABLE ROW LEVEL SECURITY;
ALTER TABLE forensicauditlog ENABLE ROW LEVEL SECURITY;

-- Deny all public API access (anon + authenticated web roles)
CREATE POLICY "Deny all public API access"
ON users AS RESTRICTIVE FOR ALL
TO anon, authenticated USING (false);

CREATE POLICY "Deny all public API access"
ON cases AS RESTRICTIVE FOR ALL
TO anon, authenticated USING (false);

CREATE POLICY "Deny all public API access"
ON evidence AS RESTRICTIVE FOR ALL
TO anon, authenticated USING (false);

CREATE POLICY "Deny all public API access"
ON forensicauditlog AS RESTRICTIVE FOR ALL
TO anon, authenticated USING (false);
```

> These policies ensure that even if the Supabase project URL and anon key were leaked, **zero data would be accessible** through the web API surface.

### JDBC-Only Access
- Direct database access is restricted to the **master Supabase account** via JDBC
- No REST, GraphQL, or Realtime surface is used or exposed
- Supabase's auto-generated API is effectively neutralized by the RLS restrictive policies above

### Audit Trail
- Every sensitive action is logged to `ForensicAuditLog` with:
    - `UserID` — who performed the action
    - `Action` — what was done
    - `Role` — the role at time of action
    - `Timestamp` — synchronized to `Asia/Karachi` for jurisdiction-aware chain of custody

---

## Security Architecture Diagram

```
┌─────────────────────────────────────────────┐
│              Login Screen                   │
│         SHA-256 Password Verification       │
└──────────────────┬──────────────────────────┘
                   │
       ┌───────────▼───────────┐
       │     Session Manager   │  ← Singleton, holds role + user ID
       └───────────┬───────────┘
                   │
      ┌────────────▼────────────┐
      │   Role-Based Access     │
      │  ADMIN  │  INVESTIGATOR │
      │─────────┼───────────────│
      │ All ops │ Read + Upload │
      └─────────────────────────┘
                   │
      ┌────────────▼────────────┐
      │    Access Manager       │  ← Lockdown state (persisted to DB)
      │  Kill Switch / Lift     │
      └─────────────────────────┘
                   │
      ┌────────────▼────────────┐
      │  Supabase PostgreSQL    │
      │  ├─ HikariCP pool       │  ← JDBC only, no web API
      │  ├─ RLS: API blocked    │  ← anon + authenticated denied
      │  └─ TZ: Asia/Karachi    │  ← Forensic audit timestamps
      └─────────────────────────┘
```
