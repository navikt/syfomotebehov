REVOKE ALL ON ALL TABLES IN SCHEMA public FROM cloudsqliamuser;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM "disykefravar-x4wt@knada-gcp.iam";
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM "disykefravar-x4wt@knada-gcp.iam.gserviceaccount.com";

GRANT SELECT ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
GRANT SELECT ON MOTEBEHOV_DVH TO "disykefravar-x4wt@knada-gcp.iam.gserviceaccount.com";
GRANT SELECT ON ALL TABLES IN SCHEMA public TO "esyfo-analyse";

REVOKE ALL ON ALL TABLES IN SCHEMA public FROM "disykefravar@nav-data-union-restricted-prod.iam";
GRANT SELECT ON ALL TABLES IN SCHEMA public to cloudsqliamserviceaccount;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO cloudsqliamserviceaccount;
