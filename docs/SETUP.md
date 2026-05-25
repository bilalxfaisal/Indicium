# 🚀 Setup Guide

> [← Back to README](../README.md)

---

## Prerequisites

| Requirement | Version |
|-------------|---------|
| JDK | 21+ |
| JavaFX SDK | 25 |
| Maven | 3.8+ |
| Supabase Account | — |

---

## Installation

**1. Clone the repository**
```bash
git clone https://github.com/Hashimk101/indicium.git
cd indicium
```

**2. Set up your Supabase project**
- Go to [supabase.com](https://supabase.com) and create a new project
- Navigate to **Settings → Database** and copy your connection string
- It will look like:
```
jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:5432/postgres
```

**3. Run the schema**
- Go to your Supabase project → **SQL Editor**
- Paste and run the contents of `schema/indicium_schema.sql`
- This creates all tables, constraints, and RLS policies in one shot

**4. Configure your connection**

Copy the example config:
```bash
cp src/main/resources/com/indicium/config/db.properties.example \
   src/main/resources/com/indicium/config/db.properties
```

Then edit `db.properties` with your real credentials:
```properties
db.url="connection_string_from_supabase"
db.username=postgres
db.password=your_supabase_password
```

> ⚠️ `db.properties` is in `.gitignore` — your credentials will never be committed.

**5. Build and run**
```bash
mvn clean javafx:run
```

---

## Maven Dependencies

Make sure your `pom.xml` includes:

```xml
<!-- PostgreSQL JDBC Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>

<!-- HikariCP Connection Pooling -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

---

## .gitignore

Make sure this is in your `.gitignore`:
```gitignore
# Secrets
src/main/resources/com/indicium/config/db.properties

# Build output
target/

# IDE files
.idea/
*.iml
.vscode/
```
