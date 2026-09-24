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
