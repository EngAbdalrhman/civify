# Civify Production backend

This checkout is based on Production. Read `../Knowledge/README.md` and
`../Knowledge/backend-contracts.md` before changing MCP contracts. The original
`../civify/` main checkout is older; preserve it.

External controller: `controller/external/ExternalCvController.java` under
`src/main/java/com/app/civify/`. Tailoring accepts multipart originals or JSON
CvTailorRequest with resumeData; both retain cv:tailor scope and the same billing
path. JSON avoids a second parse. Watermark policy uses account:read and the
authenticated user's existing resume entitlement. See `docs/mcp-contracts.md`.

Run Java 21 and `mvn -Dtest=ExternalCvServiceTest,RequestLoggingFilterTest test`
for these changes. Request logging runs before security; never log credentials,
resume contents, arbitrary URLs or filenames. Update shared Knowledge after
verifying behavior. Local tests and commits do not deploy Production.

For live CV quality fixes, see ../Knowledge/2026-09-25-live-mcp-validation.md.

McpAccountLinkService bridges the existing authenticated web session to gateway OAuth.
It reads request metadata only from configured civify.mcp.origin, issues a 60-second
proof-bound code and creates a scoped 30-day key only at exchange. Redis GETDEL is
required in production; fail closed on uncertain consumption. Never log these codes,
verifiers or returned keys. See ../Knowledge/2026-09-25-account-linking.md.
Also run CvTailoringQualityTest, ResumeMaskingTest and AtsCompatibilityServiceTest.
Use Maven clean test: incremental annotation-generated sources are stale in this workspace.
Preserve identity across renamed/moved items; never inflate a lower ATS score or hide
KEYWORD_FALLBACK. Deploy structured masking changes before rich custom PDF rendering.
