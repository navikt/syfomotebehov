REVOKE ALL ON ALL TABLES IN SCHEMA public FROM cloudsqliamuser;

-- GRANT SELECT ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO cloudsqliamuser;

DO $$
BEGIN
  CREATE ROLE cloudsqlsuperuser WITH NOLOGIN;
  EXCEPTION WHEN DUPLICATE_OBJECT THEN
  RAISE NOTICE 'not creating role cloudsqlsuperuser -- it already exists';
END
$$;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO cloudsqlsuperuser;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO cloudsqlsuperuser;

DO $$
BEGIN
  CREATE USER "esyfo-analyse";
  EXCEPTION WHEN DUPLICATE_OBJECT THEN
  RAISE NOTICE 'not creating role esyfo-analyse -- it already exists';
END
$$;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO "esyfo-analyse";
