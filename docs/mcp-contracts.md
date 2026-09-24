# External API contracts for agents

Production additions, 2026-09-24. Deploy this backend before the updated frontend
renderer and MCP gateway. Existing multipart endpoints remain supported.

- POST `/v1/external/cvs/tailor`, Content-Type application/json, cv:tailor:
  CvTailorRequest with resumeData, jobDescription and optional jobTitle,
  companyName, generateCoverLetter, includeInterviewQuestions, includeRoadmap.
  The service skips parsing, retains operation auditing and charges actual tailoring
  usage through the same path as multipart. Response is CvTailorResponse.tailoredCv.
- GET `/v1/external/cvs/watermark-config?resumeId=...`, account:read:
  returns CommonResponse.data with the authenticated user's watermark policy and
  optional existing resume entitlement. The frontend renderer forwards the API key
  only to this first-party backend. It does not trust client-supplied watermark rules.

The gateway automatically exports tailored data. Export failure must retain that
data and retry only PDF rendering; never repeat paid tailoring automatically.
Mask returns PDF bytes, which the hosted gateway exposes via a temporary capability
URL. Download promptly; no durable artifact storage or operation replay is promised.

RequestLoggingFilter runs before authentication. It accepts only UUID-shaped
X-Request-ID and bounded civify_* X-Civify-MCP-Tool labels, then logs fixed route
labels, status, success and duration. Labels never authorize requests. No query,
body, filename, API key or arbitrary request header is added to these logs.
Correlate the request ID returned in MCP data with REQUEST_START/REQUEST_END.
Existing domain audit storage remains subject to its existing access/retention rules.

Validation: Java 21; focused service/filter tests cover no duplicate parse, one
tailoring charge, original-file compatibility, policy delegation, rejected input,
credential exclusion, failure logging and MDC cleanup. Live AI/provider integration
and client UI account linking require a deployed test account.

## Live CV quality follow-up (2026-09-25)

ATS results can include analysisSource=AI_HYBRID or KEYWORD_FALLBACK. Report fallback
analysis as incomplete; do not retry paid tailoring just to replace the score. Scores
are no longer artificially raised to their pre-tailoring value. Organization validation
uses item.subtitle (employer/institution); item.title is the job title or degree.
Stable IDs identify renamed/moved items when restoring content, preventing duplicates.

Structured masking preserves the original object, respects requested mask types,
redacts recognized personal-detail labels and repeated known contact values, and masks
city/country in personalInfo. It is not exhaustive detection of unlabeled personal data.
Deploy these masking changes before the frontend starts rendering rich custom fields.
