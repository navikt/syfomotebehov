DO $$
BEGIN
  CREATE USER "esyfo-analyse";
  EXCEPTION WHEN DUPLICATE_OBJECT THEN
  RAISE NOTICE 'not creating role esyfo-analyse -- it already exists';
END
$$;

GRANT SELECT ON ALL TABLES IN SCHEMA public TO "esyfo-analyse";
