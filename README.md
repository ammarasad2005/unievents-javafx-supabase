# UniEvents — University Event & Club Management System

**Course:** Software Design & Analysis (SDA) — Section E | FAST-NUCES Islamabad  
**Members:** Ammar Asad (`23I-0716`), Abdullah Faraz (`25I-1022`)  
**Stack:** Java 17+ · JavaFX 21 · Supabase (PostgreSQL 17) via JDBC (`org.postgresql:postgresql`) · Maven · JUnit 5

---

## 1. Architecture & Package Layout

```text
unievents/
├── Launcher.java                           # Non-modular entry point for IDE & CLI launch
├── ui/                                     # Presentation layer (JavaFX views — talks ONLY to application controllers)
│   ├── UniEventsApp.java                   # Composition root
│   ├── LoginView.java                      # Thin session login + quick-fill demo pills + runtime JDBC config
│   ├── MainShellView.java                  # Polymorphic role navigation (UserRoleVisitor — zero instanceof)
│   ├── BrowseEventsUI.java                 # UC1 (Browse/Filter/Details) & UC2 (Register/Waitlist)
│   ├── MyRegistrationsUI.java              # UC3 (My Registrations & Atomic Cancel + Promotion)
│   ├── CheckInUI.java                      # UC4 (Student Self Check-In via Venue Code)
│   ├── ReviewEventUI.java                  # UC5 (Review Eligibility + Rating & Review Submission)
│   ├── StudentNotificationsView.java       # In-app Notification Inbox
│   ├── ManageEventUI.java                  # UC6 (My Events + Create DRAFT / Edit Event Details)
│   ├── EventStatusUI.java                  # UC7 (Event Status State Machine Transitions)
│   ├── AnnouncementUI.java                 # UC8 (Audience Counts + Broadcast Event Announcement)
│   ├── AttendanceUI.java                   # UC9 (Attendance Sheet + Manual Batch Attendance)
│   ├── AttendanceReportUI.java             # UC10 (Generate & Persist Attendance Report + Participants)
│   ├── CreateClubUI.java                   # UC11 (Admin Create Official Club + Assign President)
│   └── ConflictResolutionUI.java           # UC12 (Admin Inspect & Resolve Registration Conflicts)
├── application/                            # Application controllers (orchestration only)
│   ├── StudentController.java              # D5 Design Class Diagram Student facade
│   ├── ManagementController.java           # D5 Design Class Diagram Organizer/Admin facade
│   ├── EventBrowseController.java          # SD-01
│   ├── RegistrationController.java         # SD-02, SD-03
│   ├── CheckInController.java              # SD-04
│   ├── ReviewController.java               # SD-05
│   ├── EventManagementController.java      # SD-06, SD-07
│   ├── AnnouncementController.java         # SD-08
│   ├── AttendanceManagementController.java # SD-09, SD-10
│   ├── ClubController.java                 # SD-11
│   ├── ConflictController.java             # SD-12
│   └── AuthController.java                 # Thin session precondition
├── domain/                                 # Rich domain entities owning all business rules
│   ├── User.java (abstract), Student.java, ClubOrganizer.java, Admin.java, UserRoleVisitor.java
│   ├── Club.java, Category.java, Event.java, Registration.java, Attendance.java
│   ├── Review.java, Announcement.java, Notification.java, AttendanceReport.java, RegistrationConflict.java
│   └── SummaryFactory.java                 # GoF Simple Factory for immutable cross-layer DTOs
├── infrastructure/persistence/             # Persistence layer (Protected Variation)
│   ├── DataHandler.java                    # Persistence interface
│   ├── SupabaseHandler.java                # PostgreSQL / Supabase JDBC DAO implementation
│   └── DatabaseController.java             # GoF Singleton managing JDBC Connection & transactions
└── shared/                                 # Enums, immutable record DTOs, and domain exceptions
```

### Design ↔ Code Consistency Notes

1. **Persistence naming delta (one-line note):** `SupabaseHandler` implements `DataHandler` over PostgreSQL/Supabase JDBC in place of Deliverable 5's `MySQLHandler`, keeping all `DataHandler` method names and `DatabaseController` singleton operations identical.
2. **Controller alignment:** Deliverable 5's Design Class Diagram consolidates controllers into `StudentController` and `ManagementController`, whereas Sequence Diagrams `SD-01..SD-12` depict 9 cohesive per-flow controllers (`EventBrowseController`, `RegistrationController`, `CheckInController`, `ReviewController`, `EventManagementController`, `AnnouncementController`, `AttendanceManagementController`, `ClubController`, `ConflictController`). Both are present: `StudentController` and `ManagementController` act as clean facades delegating to the 9 SD controllers with zero duplicated logic.
3. **Enum alignment:** `CheckInMethod` uses `CODE` and `MANUAL` to match `sql/schema.sql`.

---

## 2. Supabase JDBC Setup & Launch Instructions

### Zero-Prerequisite Windows Setup (Fresh PC — Installs JDK 21 + Maven + Configures `.env` + Launches App)
If your Windows machine does not yet have Java or Maven installed, run **`setup-and-run.ps1`** in PowerShell (or double-click **`run-unievents.bat`**):

```powershell
# Option 1: From anywhere in PowerShell (even before cloning Git!)
irm https://raw.githubusercontent.com/ammarasad2005/unievents-javafx-supabase/main/setup-and-run.ps1 | iex

# Option 2: If you already downloaded setup-and-run.ps1 or cloned the folder
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup-and-run.ps1
```
It automatically installs **Java JDK 21 LTS** and **Apache Maven 3.9.9** (with a zero-admin portable fallback if `winget` is unavailable), downloads the project if needed, prompts once for your **Supabase Database Password** (saving it to `.env`), and launches the **UniEvents** desktop window.

### Manual Prerequisites (macOS / Linux / Custom Setup)
- **Java 17+** (tested on JDK 17 & JDK 21)
- **Maven 3.8+** *(or use `./mvnw` / `.\mvnw.cmd` included in the repo)*

### Step 1 — Configure Your Supabase Database Password
Your connected Supabase project (`reoscvsfwwefkeoclkvz`, region `ap-south-1`) is already migrated and seeded, and `src/main/resources/db.properties` is pre-configured with its **IPv4-compatible Session Pooler** endpoint (which works over university Wi-Fi, PTCL, Nayatel, and mobile hotspots where direct IPv6 `db.<ref>.supabase.co` may be unreachable):

```properties
db.url=jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require&prepareThreshold=0
db.user=postgres.reoscvsfwwefkeoclkvz
db.password=<YOUR_SUPABASE_DB_PASSWORD>
```

Provide your Supabase database password using **any one** of these three options:
1. **Edit `src/main/resources/db.properties`** and replace `<YOUR_SUPABASE_DB_PASSWORD>`.
2. **Environment variable / `.env` file:** set `UNIEVENTS_DB_PASSWORD=<password>` (or copy `.env.example` to `.env`).
3. **In-App UI:** Launch the app as-is and paste your password directly into the **"Supabase PostgreSQL JDBC Connection Settings"** pane on the Login screen.

### Step 2 — Database Schema & Seed (if setting up a fresh DB or resetting before demo)
- Fresh database: run `sql/schema.sql` followed by `sql/seed.sql` in the Supabase SQL Editor.
- Resetting to a clean state right before your grading demo: run `sql/reset_demo.sql` in the Supabase SQL Editor.

### Step 3 — Run the Desktop Application
```bash
mvn javafx:run
```
*(In IntelliJ IDEA or VS Code, you can also run `unievents.Launcher.main()` directly without configuring JavaFX VM module flags.)*

### Step 4 — Run Unit & Integration Tests
```bash
# Domain unit tests (runs offline with zero DB setup):
mvn test

# Full suite including live PostgreSQL JDBC integration tests across all 12 Use Cases:
mvn test \
  -Dunievents.db.url="jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require&prepareThreshold=0" \
  -Dunievents.db.user="postgres.reoscvsfwwefkeoclkvz" \
  -Dunievents.db.password="<YOUR_SUPABASE_DB_PASSWORD>"
```

---

## 3. OOP & GRASP / GoF Patterns Traceability (Viva Reference)

| Principle / Pattern | Concrete Implementation in Code |
|---------------------|---------------------------------|
| **Encapsulation** | All domain fields are `private`; status changes happen exclusively via invariant-enforcing methods (`Event.register`, `Registration.cancel`, `Registration.markAttended`, `Event.changeStatus`, `RegistrationConflict.resolve`) with no public status setters. |
| **Abstraction** | Abstract `User` base class (`getRole()`, `getRoleDisplayName()`, `getProfileSummary()`, `accept(UserRoleVisitor)`) + `DataHandler` interface. |
| **Inheritance** | `Student`, `ClubOrganizer`, and `Admin` extend `User` and map to PostgreSQL subtype tables (`students`, `club_organizers`, `admins`). |
| **Polymorphism (Zero `instanceof`)** | `MainShellView` invokes `currentUser.getRole()`, `currentUser.getProfileSummary()`, and `currentUser.accept(UserRoleVisitor)` through a `User` reference — **zero `instanceof` checks exist anywhere in `src/`**. |
| **GRASP Controller** | JavaFX UI views call only `unievents.application.*` controllers; zero `java.sql` imports exist in `unievents.ui`. |
| **GRASP Information Expert** | `Event` computes `getRemainingSeats()`, validates `updateDetails` capacity against `getRegisteredCount()`, checks `isEligibleForReview()`, computes `getAverageRating()`, and builds `getAudienceCounts()`. |
| **GRASP Creator** | `Club.createEvent(...)` creates `Event`; `Event.register(...)` creates `Registration`; `Registration.markAttended(...)` creates `Attendance`; `Event.addReview(...)` creates `Review`; `Event.generateReport(...)` creates `AttendanceReport`. |
| **GRASP Low Coupling & High Cohesion** | Dedicated per-flow controllers (`EventBrowseController`, `RegistrationController`, `CheckInController`, `ReviewController`, etc.) keep student, organizer, and admin concerns focused and independent. |
| **GRASP Protected Variation** | Controllers depend solely on `DataHandler`; `SupabaseHandler` encapsulates all PostgreSQL JDBC and enum-casting details. |
| **GoF Singleton** | `DatabaseController.getInstance()` manages connection lifecycle and `beginTransaction()` / `commit()` / `rollback()`. |
| **GoF Simple Factory** | `SummaryFactory` constructs immutable record DTOs (`EventSummary`, `EventDetails`, `RegistrationSummary`, `ConflictSummary`, `ConflictDetails`). |

---

## 4. Seeded Demo Accounts (All Passwords: `demo123`)

| Role | Name | Email | Notes |
|------|------|-------|-------|
| **Student** | Ali Raza (`23I-0001`) | `ali.raza@nuces.edu` | Primary student demo account |
| **Student** | Fatima Noor (`23I-0002`) | `fatima.noor@nuces.edu` | Confirmed seat on *Cloud Native Docker Workshop* (for UC3 waitlist promotion demo) |
| **Organizer** | Hamza Tariq | `hamza.tariq@nuces.edu` | President of **ACM Society** (manages Events #1, #2, #3, #5, #6) |
| **Organizer** | Sara Malik | `sara.malik@nuces.edu` | President of **Sports Society** (manages Event #4 *Marathon 5K*) |
| **Organizer** | Usman Iqbal | `usman.iqbal@nuces.edu` | Unassigned organizer — select as President in **UC11 Create Club** |
| **Admin** | Dr. Ayesha Khan | `ayesha.khan@unievents.edu` | Campus Admin (*Student Affairs*) for **UC11** & **UC12** |

---

## 5. 10-Minute Grading Demo Script (All 12 Use Cases End-to-End)

> **Tip:** Click the quick-fill account pills on the Login screen to switch between Student, Organizer, and Admin in one second.

### Part A — Student Flows: UC1 → UC5 (Minutes 0:00 – 4:30)
1. **Sign In as Student (`Ali Raza` — `ali.raza@nuces.edu` / `demo123`)**
2. **UC1 — Browse Events:**
   - Observe only `PUBLISHED` events appear (*AI & ML Bootcamp*, *Tech Dev Hour*, *Marathon 5K*, *Cloud Native Docker Workshop*); `DRAFT` and `COMPLETED` events are hidden.
   - Filter by Category = `Sports` → only *Marathon 5K* appears. Click **Reset** and select events to inspect remaining seats and reviews on the right card.
3. **UC2 — Register for Event (Happy Path + Full Event Waitlist):**
   - Select **Marathon 5K** (150 seats open) → click **Register Seat (UC2)** → green banner confirms status **`REGISTERED`**.
   - Select **Cloud Native Docker Workshop** (`FULL · Waitlist: 1`) → click **Join Waitlist (UC2)** → banner confirms status **`WAITLISTED` at Position `#2`** (behind Zainab `#1`).
4. **UC4 — Check In at Event:**
   - Open **3. Self Check-In (UC4)** in the sidebar → choose **Tech Dev Hour** (`[REGISTERED]`).
   - First type `WRONG-CODE` → red error banner (`InvalidCheckInCodeException`).
   - Now enter `ACM-DEVHOUR-26` → green banner confirms `Attendance` created via `CODE` and status updated to **`ATTENDED`**.
5. **UC5 — Rate and Review Event:**
   - Open **4. Rate & Review (UC5)** → choose **#3 · Inter-University Hackathon [My Status: ATTENDED]** (current rating `5.0 / 5.0` from Fatima).
   - Badge shows **`ELIGIBLE`**. Submit rating **`4`** with comment `"Great problem set and mentorship!"` → review saved, average updates live to **`4.5 / 5.0`**, and badge flips to **`INELIGIBLE`** (duplicate reviews blocked).
6. **UC3 — Cancel Registration + Automatic Waitlist Promotion:**
   - Click **Switch Role / Sign Out** → click quick-fill **`Student: Fatima Noor`** → Sign In.
   - Open **2. My Registrations (UC3)** → select **Cloud Native Docker Workshop** (`REGISTERED`) → click **Cancel Selected Registration (UC3)** → confirm.
   - Status becomes **`CANCELLED`**, **Zainab Sheikh** (`WAITLISTED #1`) is **automatically promoted to `REGISTERED`** in the same database transaction, and **Ali Raza** advances from Waitlist `#2` to `#1`!

### Part B — Club Organizer Flows: UC6 → UC10 (Minutes 4:30 – 8:00)
7. **Sign Out → Quick-Fill `Organizer: Hamza (ACM)` (`hamza.tariq@nuces.edu` / `demo123`)**
8. **UC6 — Manage Event Details (Create & Capacity Guard):**
   - On **1. My Events & Editor (UC6)**, click **`+ New Draft Event`**, enter Title `"System Design Workshop"`, Category `Technology`, Capacity `40`, Venue `"CS Auditorium"`, click **Save Event Details (UC6)** → created in **`DRAFT`** status with generated check-in code.
   - Now select **Tech Dev Hour** (4 confirmed registrations) in the table, change Capacity to **`2`**, and click **Save** → domain rule rejects it (`Capacity (2) cannot be lower than current confirmed registrations (4)`).
9. **UC7 — Manage Event Status:**
   - Open **2. Manage Status (UC7)** → select **Freshers Welcome Night** (`DRAFT`) → transition to **`PUBLISHED`** → succeeds! (Trying an invalid jump such as `COMPLETED → DRAFT` on *Inter-University Hackathon* displays a domain validation error.)
10. **UC8 — Send Event Announcement:**
    - Open **3. Send Announcement (UC8)** → select **Cloud Native Docker Workshop** → live cards show Registered: `2`, Waitlisted: `1`, Total Combined: `3`.
    - Choose Audience `BOTH`, enter Subject `"Pre-Lab Docker Setup"` and Message `"Please pull postgres:17 before class"`, click **Send Announcement (UC8)** → confirms `Announcement` saved and **`3` student notifications delivered**.
11. **UC9 — Mark Attendance:**
    - Open **4. Mark Attendance (UC9)** → select **Tech Dev Hour** (notice Ali Raza already shows `ATTENDED` via `CODE`).
    - Tick **Fatima Noor** and **Bilal Ahmed** (or click **Select All Pending**) → click **Record Manual Attendance (UC9)** → marked **`ATTENDED`** with method **`MANUAL`**.
12. **UC10 — Generate Attendance Report:**
    - Open **5. Attendance Report (UC10)** → select **Tech Dev Hour** → click **Generate & Save Attendance Report (UC10)**.
    - Displays persisted `Report #`, `Confirmed Registered: 4`, `Total Attended: 3`, `Turnout Rate: 75.0%`, and the complete `Participant` table.

### Part C — Admin Flows: UC11 → UC12 (Minutes 8:00 – 10:00)
13. **Sign Out → Quick-Fill `Admin: Dr. Ayesha Khan` (`ayesha.khan@unievents.edu` / `demo123`)**
14. **UC11 — Create Club:**
    - On **1. Create Official Club (UC11)**, enter Name `"Robotics & Automation Society"`, Description `"Autonomous systems and embedded competitions"`, select President **`Usman Iqbal`**, and click **Create Official Club (UC11)** → club appears immediately in the Campus Societies Directory and Usman is assigned President!
15. **UC12 — Resolve Registration Conflict:**
    - Open **2. Resolve Conflicts (UC12)** → select **Conflict #1** (`AI & ML Bootcamp` · `CAPACITY_BREACH` · `Ahmed Hassan` — 3 active seats on a 2-seat event).
    - Inspect registration history and current waitlist (`Bilal #1`, `Zainab #2`), choose Resolution Policy **`MOVE_TO_WAITLIST`** (or **`INCREASE_CAPACITY`**), enter Audit Reason `"Moved excess bulk-imported registration to waitlist to enforce lab limit"`, and click **Apply Resolution & Mark RESOLVED (UC12)** → conflict is marked **`RESOLVED`** and open conflicts table clears!
