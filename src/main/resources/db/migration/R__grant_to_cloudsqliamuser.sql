-- ${flyway:timestamp} Inserts a timestamp to generate a new hash to trigger re-execution on deploy.
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM cloudsqliamuser;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM "disykefravar-x4wt@knada-gcp.iam";
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM "disykefravar-x4wt@knada-gcp.iam.gserviceaccount.com";

GRANT SELECT ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;

GRANT SELECT ON MOTEBEHOV_DVH TO "disykefravar-x4wt@knada-gcp.iam.gserviceaccount.com";

GRANT SELECT ON ALL TABLES IN SCHEMA public TO "esyfo-analyse";
