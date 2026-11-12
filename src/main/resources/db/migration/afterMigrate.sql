REVOKE ALL ON ALL TABLES IN SCHEMA public FROM cloudsqliamuser;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM "disykefravar-x4wt@knada-gcp.iam";
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM "disykefravar-x4wt@knada-gcp.iam.gserviceaccount.com";

GRANT SELECT ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
GRANT SELECT ON MOTEBEHOV_DVH TO "disykefravar-x4wt@knada-gcp.iam.gserviceaccount.com";
GRANT SELECT ON ALL TABLES IN SCHEMA public TO "esyfo-analyse";

DO $$
DECLARE
  dvh_user text := CASE '${naisCluster}'
    WHEN 'dev-gcp' THEN 'disykefravar-developme-4c2f9@nav-data-union-restricted-prod.iam'
    WHEN 'prod-gcp' THEN 'disykefravar-sa@nav-data-union-restricted-prod.iam'
  END;
BEGIN
  IF dvh_user IS NULL THEN
    RETURN;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = dvh_user) THEN
    RAISE NOTICE 'not granting DVH access -- role % does not exist', dvh_user;
    RETURN;
  END IF;
  EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA public FROM %I', dvh_user);
  EXECUTE format('GRANT SELECT ON MOTEBEHOV_DVH TO %I', dvh_user);
END
$$;
