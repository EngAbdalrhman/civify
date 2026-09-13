# Civify Project Rules

## Knowledge Base
A comprehensive knowledge base exists at `d:\Personal\work\odc\knowledge-base\` covering both Civify and the new ODC job platform. Check it before deep-diving into the codebase.

## Project Context
- **Civify** is an AI-powered SaaS platform for CV/Resume building, AI tailoring, ATS scoring, and career tooling
- **Tech Stack**: Spring Boot 3.4 (Java 21), PostgreSQL, Redis, Quartz, Google Gemini/Vertex AI
- **Payments**: Paymob (MENA), Fawaterak (MENA), Dodo Payments (Global) with GeoIP routing
- **Auth**: JWT + OAuth2 (Google, LinkedIn) + API Keys, RBAC with fine-grained permissions
- **Related Project**: ODC job platform at `d:\Personal\work\odc` — built on top of Civify

## Key Metrics
- ~1,500 users, ~2,000 CVs stored
- Subscription tiers: Free, Pro, Premium, Enterprise with hybrid token economics
