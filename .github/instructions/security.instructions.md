---
applyTo: "**/*"
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Security Standards

## NAV Security Principles
1. **Defense in Depth**: Multiple layers of security controls
2. **Least Privilege**: Minimum necessary permissions
3. **Zero Trust**: Never trust, always verify
4. **Privacy by Design**: GDPR compliance built-in

## Golden Path (sikkerhet.nav.no)

### Priority 1: Platform Basics
- Use NAIS defaults for auth
- Set up monitoring and alerts
- Control secrets (never copy prod secrets locally)

### Priority 2: Scanning Tools
- Dependabot for dependency vulnerabilities
- Trivy for Docker image scanning
- Static analysis (CodeQL, Semgrep)

### Priority 3: Secure Development
- Chainguard/Distroless base images
- Validate all input
- No sensitive data in logs (FNR, JWT tokens, database connection strings)
- Prefer OAuth/Maskinporten for new M2M integrations (service users are legacy — avoid in new code)

## Network Policies
```yaml
accessPolicy:
  outbound:
    rules:
      - application: user-service
        namespace: team-user
    external:
      - host: api.external.com
  inbound:
    rules:
      - application: frontend
        namespace: team-web
```
**Default Deny**: All traffic blocked unless explicitly allowed.

## Security Checklist
- No hardcoded credentials
- Parameterized SQL queries
- Input validation at all boundaries
- No PII in logs
- accessPolicy defined
- Dependabot enabled

## Boundaries

### ✅ Always
- Check for parameterized queries
- Validate all inputs at boundaries
- Define `accessPolicy` for every service
- Follow Golden Path priorities

### ⚠️ Ask First
- Modifying `accessPolicy` in production
- Changing authentication mechanisms
- Granting elevated permissions

### 🚫 Never
- Bypass security controls
- Commit secrets to git
- Log FNR, JWT tokens, passwords, or database connection strings
- Skip input validation
