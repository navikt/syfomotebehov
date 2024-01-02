DO $$
BEGIN
  CREATE ROLE cloudsqliamuser WITH NOLOGIN;
  EXCEPTION WHEN DUPLICATE_OBJECT THEN
  RAISE NOTICE 'not creating role cloudsqliamuser -- it already exists';
END
$$;
