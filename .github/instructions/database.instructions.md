---
applyTo: "**/db/migration/**/*.sql"
---

# Database Migration Standards (Flyway)

## Migration File Naming

Follow Flyway naming convention: `V{major}_{minor}__{description}.sql`

This project uses `V1_{n}__description.sql` numbering (e.g., `V1_18__delete_motebehov_for_wrong_person.sql`).

### Rules

- Version numbers must be sequential within the V1_ series
- Use double underscore `__` between version and description
- Description should be lowercase with underscores
- **NEVER modify existing migrations** - always create new ones
- Check existing migrations in `src/main/resources/db/migration/` for the next number

## Migration File Structure

```sql
-- V1_19__add_new_column.sql

ALTER TABLE MOTEBEHOV
ADD COLUMN new_column VARCHAR(255);

CREATE INDEX idx_motebehov_new_column ON MOTEBEHOV(new_column);
```

## Key Tables

- `MOTEBEHOV` — Main table for meeting need submissions
- `MOTEBEHOV_FORM_VALUES` — Form snapshot values (linked via `motebehov_row_id`)
- `DIALOGMOTEKANDIDAT` — Dialogue meeting candidate tracking
- `OPPFOLGINGSTILFELLE` — Follow-up case tracking

## Best Practices

### Data Types

```sql
VARCHAR(n)      -- For strings with known max length
TEXT            -- For strings with unknown length
BIGINT          -- For large numbers
TIMESTAMP       -- For date/time
DATE            -- For dates only
BOOLEAN         -- For true/false
UUID            -- For unique identifiers
```

### Adding a Column

```sql
ALTER TABLE MOTEBEHOV
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'pending';

CREATE INDEX idx_motebehov_status ON MOTEBEHOV(status);
```

## Boundaries

### ✅ Always

- Follow `V1_{n}__description.sql` naming convention
- Add indexes for columns used in WHERE clauses
- Test migrations locally before pushing

### ⚠️ Ask First

- Schema changes affecting multiple tables
- Dropping columns or tables
- Large data migrations

### 🚫 Never

- Modify existing migration files
- Skip version numbers
- Deploy untested migrations to production
