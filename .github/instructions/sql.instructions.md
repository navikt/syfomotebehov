---
applyTo: "**/db/migration/**/*.sql"
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Database Migration Standards (Flyway)

## Flyway
- File naming: `V{number}__{description}.sql` (double underscore)
- Migrations are immutable — NEVER modify an existing migration
- Use sequential version numbers
- Each migration should be focused on a single change

## PostgreSQL
- Use `TIMESTAMPTZ` for all timestamps
- Use `UUID` for primary keys where appropriate (with `gen_random_uuid()`)
- Use `TEXT` instead of `VARCHAR` (unless max length constraint is needed)
- Use `IF NOT EXISTS` / `IF EXISTS` selectively where idempotency is intentional — prefer fail-fast in versioned migrations
- Add indexes for columns used in WHERE, JOIN, ORDER BY

## Patterns

```sql
-- Creating a table (prefer fail-fast in versioned migrations)
CREATE TABLE table_name (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ident VARCHAR(11) NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Indexes
CREATE INDEX idx_table_ident ON table_name(ident);
CREATE INDEX idx_table_status ON table_name(status);

-- Updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_updated_at
BEFORE UPDATE ON table_name
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
```

```sql
-- Adding a column (use IF NOT EXISTS only when idempotency is intentional)
ALTER TABLE table_name ADD COLUMN column_name TEXT;

-- Adding an index
CREATE INDEX idx_table_column ON table_name(column_name);

-- Foreign key with CASCADE
ALTER TABLE child_table
ADD CONSTRAINT fk_parent
FOREIGN KEY (parent_id) REFERENCES parent_table(id) ON DELETE CASCADE;
```

## Best Practices
- Always include `created_at` and `updated_at` timestamps
- Add `updated_at` trigger for automatic updates
- Index foreign keys
- Use partial indexes for filtered queries: `CREATE INDEX idx_active ON orders(user_id) WHERE status = 'active'`
- Use `BIGSERIAL` for auto-incrementing IDs, `UUID` for distributed systems

## Security
- Never use string concatenation in dynamic SQL
- Grant minimum required privileges
- Never store plaintext passwords or tokens

## Boundaries

### ✅ Always
- Follow `V{n}__{description}.sql` naming
- Add indexes for foreign keys
- Include `created_at` and `updated_at` timestamps
- Test migrations in dev environment first

### ⚠️ Ask First
- Schema changes affecting multiple tables
- Dropping columns or tables
- Large data migrations

### 🚫 Never
- Modify existing migration files
- Skip version numbers
- Deploy untested migrations to production
