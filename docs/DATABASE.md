# 🗄️ Database

> [← Back to README](../README.md)

Indicium uses **Supabase (PostgreSQL)** as its hosted cloud database, connecting via JDBC through the `aws-1-ap-southeast-1` pooler endpoint for optimized IPv4 connectivity.

---

## Connection

| Property | Value |
|----------|-------|
| **Host** | `aws-1-ap-southeast-1.pooler.supabase.com` |
| **Port** | `5432` |
| **Driver** | PostgreSQL JDBC `42.7.3` |
| **Pooling** | HikariCP `5.1.0` |
| **Timezone** | `Asia/Karachi` (audit timestamps) |

---

## Schema

### Users
```sql
CREATE TABLE Users (
    UserID       SERIAL PRIMARY KEY,
    FullName     VARCHAR(255)  NOT NULL,
    Email        VARCHAR(255)  UNIQUE NOT NULL,
    PasswordHash VARCHAR(64)   NOT NULL,  -- SHA-256
    Role         VARCHAR(20)   DEFAULT 'INVESTIGATOR'
                 CHECK (Role IN ('ADMIN', 'INVESTIGATOR')),
    IsActive     BOOLEAN       DEFAULT TRUE
);
```

### Cases
```sql
CREATE TABLE Cases (
    CaseID      SERIAL PRIMARY KEY,
    Title       VARCHAR(255)  NOT NULL,
    Description TEXT,
    Status      VARCHAR(20)   DEFAULT 'OPEN'
                CHECK (Status IN ('OPEN', 'ARCHIVED', 'CLOSED')),
    CreatedAt   TIMESTAMP     DEFAULT NOW()
);
```

### Evidence
```sql
CREATE TABLE Evidence (
    EvidenceID   SERIAL PRIMARY KEY,
    CaseID       INT           NOT NULL REFERENCES Cases(CaseID),
    EvidenceType VARCHAR(50),
    HashValue    VARCHAR(64)   -- SHA-256 fingerprint for integrity tracking
);
```

### ForensicAuditLog *(append-only)*
```sql
CREATE TABLE ForensicAuditLog (
    LogID     SERIAL PRIMARY KEY,
    UserID    INT           REFERENCES Users(UserID),
    Action    VARCHAR(255)  NOT NULL,
    Role      VARCHAR(20),
    Timestamp TIMESTAMP     DEFAULT (NOW() AT TIME ZONE 'Asia/Karachi')
);
```

---

## Entity Relationships

```
Users ──────────────────────── ForensicAuditLog
  │                                    │
  │                                 UserID (FK)
  │
Cases ──────────────────────── Evidence
                                       │
                                 CaseID (FK)
```

---

## Running the Schema

1. Go to your Supabase project → **SQL Editor**
2. Paste the contents of `schema/indicium_schema.sql`
3. Click **Run**

> The full schema file including RLS policies is at [`/schema/indicium_schema.sql`](../schema/indicium_schema.sql)
