# Work Management & Billing

A desktop application for daily work record management and consolidated PDF billing for two fixed companies. Built with Java 17, JavaFX, SQLite, and OpenPDF.

## Features

- Daily work entry with company, date, areas, and work items
- Automatic amount calculation: `Sq.Ft × Nos × Rate`
- Search and filter by company, date, area, and description
- Consolidated PDF bill generation for a date range
- Bill preview before finalization
- Bill history with open, print, and reopen (cancel) support
- Record statuses: Draft → Saved → Billed
- Auto-save every 60 seconds
- Duplicate previous day's entry
- Database backup and restore
- Configurable company names and folder locations

## Requirements

- Java 17 or later
- Maven 3.8+

## Build & Run

```bash
cd work-management-billing
mvn clean javafx:run
```

## Package

```bash
mvn clean package
java -jar target/work-management-billing-1.0.0.jar
```

## Data Location

Application data is stored at:

```
%USERPROFILE%\WorkManagementBilling\
├── work_billing.db
├── Bills\
└── Backups\
```

## Workflow

1. **Work Entry** – Create daily records with areas and work items
2. **Mark as Saved** – Mark entries ready for billing
3. **Generate Bill** – Select company and date range, preview, then confirm
4. **Bill History** – View, open, print, or reopen bills

## Reopen Bill

If a mistake is found after billing, use **Reopen Bill** in Bill History. The bill status becomes Cancelled, and included work entries return to Saved status for editing and re-billing.
