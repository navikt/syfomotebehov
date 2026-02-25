---
name: flyway-migration
description: Database migration patterns using Flyway with versioned SQL scripts
---

# Flyway Migration Skill

This skill provides patterns for managing database schema changes with Flyway.

## Migration File Naming

This project uses `V1_{n}__description.sql` numbering:

```text
src/main/resources/db/migration/V1_{next_number}__{description}.sql
```

Check the existing migrations to find the next version number.

## Creating Tables

```sql
CREATE TABLE new_table (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    fnr VARCHAR(11) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_new_table_fnr ON new_table(fnr);
```

## Adding Columns

```sql
ALTER TABLE MOTEBEHOV
ADD COLUMN new_column VARCHAR(255);

CREATE INDEX idx_motebehov_new_column ON MOTEBEHOV(new_column);
```

## Best Practices

1. **Never modify existing migrations**: Create a new migration instead
2. **Test migrations locally**: Run `./gradlew test` — TestContainers runs all migrations
3. **Keep migrations small**: One logical change per migration
4. **Add indexes**: For columns used in WHERE clauses or JOINs
