DO $$
BEGIN
  CREATE USER "disykefravar-x4wt@knada-gcp.iam";
  EXCEPTION WHEN DUPLICATE_OBJECT THEN
  RAISE NOTICE 'not creating user disykefravar -- it already exists';
END
$$;

GRANT SELECT ON motebehov to "disykefravar-x4wt@knada-gcp.iam";
