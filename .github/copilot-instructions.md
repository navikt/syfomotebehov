# syfomotebehov

- `mise test` runs tests with `SPRING_PROFILES_ACTIVE=unittest`; plain mise
  sessions default to `local`. Use `mise lint` for ktlint and
  `SPRING_PROFILES_ACTIVE=unittest ./gradlew build` for the full build.
- Local startup: `mise start` uses `bootRunLocal`. Docker is required; local/test infrastructure is provided by Testcontainers.
- All routes have the `/syfomotebehov` context path.
- Preserve TokenX high-assurance `acr` and allowed-client checks. Employer
  requests also require access to the employee through `BrukertilgangService`.
- Veileder requests use Azure AD and `Nav-Personident`; person access is
  checked through `VeilederTilgangConsumer` against `istilgangskontroll`.
- `formSnapshot` preserves submitted form values for history. Meeting needs
  and answers may contain health information; keep them and person identifiers
  out of ordinary logs and metric labels.
- Migrations use `V1_{n}__description.sql` in `src/main/resources/db/migration/`.
  Add the next migration instead of changing an already-applied one.
