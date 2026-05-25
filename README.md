<div align="center">

<img src="src/main/resources/com/indicium/ui/Assets/Indicium_Logo" alt="Indicium Logo" width="200" />

# INDICIUM
### Forensic Case Management System

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-25-0097A7?style=for-the-badge&logo=java&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)
![HikariCP](https://img.shields.io/badge/HikariCP-Pooling-FF6F00?style=for-the-badge&logoColor=white)
![Status](https://img.shields.io/badge/Status-Active-2E7D32?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-7B1FA2?style=for-the-badge)
![Contributors](https://img.shields.io/badge/Contributors-3-0097A7?style=for-the-badge&logo=github&logoColor=white)

*A full-stack desktop platform for managing forensic investigations,*
*evidence chain-of-custody, and secure administrative oversight.*

[Features](#-features) · [Screenshots](#-screenshots) · [Quick Start](#-quick-start) · [Docs](#-documentation) · [Team](#-team)

</div>

---

## 📌 Overview

**Indicium** is a desktop forensic case management system built with **Java**, **JavaFX**, and **Supabase (PostgreSQL)**. It centralizes the full investigative workflow — from case creation and evidence ingestion to timeline tracking, integrity verification, and emergency system controls.

Designed with a real-world forensic environment in mind, Indicium enforces **role-based access control**, maintains a **tamper-evident audit log**, and provides administrators with powerful tools including an **emergency lockdown kill switch**.

> Built as a team project by 3 contributors as part of a Software Design and Analysis SDA course.

---

## ✨ Features

### 🗂️ Case Management
- Create, edit, lock, and archive cases with a single click
- Filter by status, priority, and date range
- Overflow context menu with role-aware actions *(admin-only lock/archive)*
- Live count badges — Active, Archived, Locked

### 🔬 Evidence Module
- Upload evidence via **drag & drop** or file browser
- **Bulk import** an entire folder in one operation
- SHA-256 hash fingerprinting on every file at ingest
- One-click **integrity verification** — detects tampering instantly
- **OS-native file viewer** — opens any file in the system's default app
- Cross-case evidence linking with a dedicated Link Manager
- Evidence statuses: `Verified` · `Collected` · `Linked` · `Archived` · `Discarded`

### 📅 Timeline
- Attach timestamped events to any case
- Chronological event log per investigation

### 🔐 Identity & Access Control
- SHA-256 password hashing
- Role-Based Access Control — `ADMIN` / `INVESTIGATOR`
- Temporary password flow for new user provisioning
- Live password strength indicator
- Admin-driven Identity Manager for user creation and role assignment

### 🛡️ Integrity Manager *(Admin Only)*
- Real-time **System Online / System Locked** status chip
- **Emergency Lockdown Kill Switch** — terminates all non-admin sessions instantly
- Secondary security code required to activate
- Full audit trail on every lockdown and lift event

### 📊 Home Dashboard
- Real-time metrics: Active Cases · Evidence Items · Timeline Events · Audit Entries
- Personalized session greeting with avatar initials

### 📋 Audit Log
- Every critical action is logged with user ID, timestamp, and category
- Categories: `SECURITY` · `EVIDENCE` · `CASE` · `ACCESS`

---

## 🖥️ Screenshots

> *Add your screenshots in a `/screenshots` folder and update the paths below.*

| Home Dashboard | Case Manager |
|:-:|:-:|
| ![Home](screenshots/Home.png) | ![Cases](screenshots/Cases.png) |

| Evidence Module | Integrity Manager |
|:-:|:-:|
| ![Evidence](screenshots/Evidence.png) | ![Integrity](screenshots/Integrity.png) |

---

## ⚡ Quick Start

```bash
git clone https://github.com/Hashimk101/indicium.git
cd indicium
# Fill in your Supabase credentials in db.properties
mvn clean javafx:run
```

> Full setup instructions → **[docs/SETUP.md](docs/SETUP.md)**

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [📐 Architecture](docs/ARCHITECTURE.md) | Project structure, design patterns, and system design |
| [🗄️ Database](docs/DATABASE.md) | Full schema, entity relationships, and Supabase config |
| [🔒 Security](docs/SECURITY.md) | RLS policies, RBAC model, and access control design |
| [🚀 Setup](docs/SETUP.md) | Prerequisites, installation, and configuration guide |

---

## 🧪 Demo Data

The system ships with a curated set of real-world famous cases for demonstration:

| # | Case | Type | Status |
|---|------|------|--------|
| 1 | Zodiac Killer | Serial Murder | 🔴 Open |


---

## 🛠️ Built With

- **[Java 21](https://openjdk.org/)** — Core application language
- **[JavaFX 25](https://openjfx.io/)** — Desktop UI framework
- **[Supabase](https://supabase.com/)** — Hosted PostgreSQL backend
- **[PostgreSQL JDBC Driver](https://jdbc.postgresql.org/)** — Database connectivity
- **[HikariCP](https://github.com/brettwooldridge/HikariCP)** — High-performance connection pooling
- **[Maven](https://maven.apache.org/)** — Build and dependency management
- **[Icons8](https://icons8.com/)** — UI iconography

---

## 👥 Team

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/Hashimk101">
        <img src="https://github.com/Hashimk101.png" width="80" style="border-radius:50%" /><br/>
        <b>Hashim Khushal Khan</b>
      </a><br/>
      <a href="https://github.com/Hashimk101">@Hashimk101</a>
    </td>
    <td align="center">
      <a href="https://github.com/Hadiah-Batool">
        <img src="https://github.com/Hadiah-Batool.png" width="80" style="border-radius:50%" /><br/>
        <b>Hadiah Batool</b>
      </a><br/>
      <a href="https://github.com/Hadiah-Batool">@Hadiah-Batool</a>
    </td>
    <td align="center">
      <a href="https://github.com/bilalxfaisal">
        <img src="https://github.com/bilalxfaisal.png" width="80" style="border-radius:50%" /><br/>
        <b>Muhammad Bilal Faisal</b>
      </a><br/>
      <a href="https://github.com/bilalxfaisal">@bilalxfaisal</a>
    </td>
  </tr>
</table>

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

*Built as a collaborative team project demonstrating full-stack Java desktop development,*
*forensic data management, and secure system design.*

⭐ **Star this repo if you found it useful!**

</div>
