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

## Model Context Protocol (MCP) & Agent Rules
- **External Endpoint Isolation (CRITICAL)**:
  - All autonomous AI agents and MCP tools MUST interact exclusively through `/apis/v1/external/**` endpoints.
  - NEVER modify `ApiKeyAuthenticationFilter.java` to intercept internal web routes (`/apis/v1/cvs/**`, `/apis/v1/job-applications/**`). Internal routes belong to the browser UI with JWT cookie authentication.
  - Dedicated external controllers: `ExternalCvController`, `ExternalJobApplicationController`, `ExternalPayPerCvController`.
  - Public external endpoints: `/apis/v1/external/pay-per-cv/pricing` and `/apis/v1/external/cvs/scrape-jd` bypass API key requirements.
- **Dynamic Multi-User Authentication**:
  - NEVER use global or static API keys in agent configurations. Every agent connection (CLI or SSE session) manages its own authentication state.
  - Supported flows: interactive sign-in (`civify_login` + `civify_verify_2fa`), in-chat registration (`civify_register`), or user-provided key (`civify_set_api_key`).
  - Access token exchange: `POST /apis/api-keys/auto-generate` (with `Bearer <jwt>`) dynamically provisions a developer key upon login.
- **MCP Server Deployment & Infrastructure**:
  - Live Dokploy + Traefik deployment: `https://mcp.civify.cv`
  - Transports: `GET /sse` (primary stream), `GET /` (auto-detects SSE via `Accept: text/event-stream`), `POST /messages`, `/sse`, `/`.
  - Static Server Card: `GET https://mcp.civify.cv/.well-known/mcp/server-card.json` (SEP-1649 for 1-click Smithery/Glama registry indexing).
  - Docker DNS Reliability: Alpine containers must specify `dns: [8.8.8.8, 1.1.1.1]` and use `dns.setDefaultResultOrder("ipv4first")` in Node.js to eliminate `EAI_AGAIN` query drops.
- **Public Documentation Screens (Next.js Frontend)**:
  - REST API Docs: `https://civify.cv/api-docs` (`src/app/[locale]/api-docs`)
  - MCP Server Docs: `https://civify.cv/mcp-docs` (`src/app/[locale]/mcp-docs`) and alias `https://civify.cv/developers/mcp`
  - Discoverability Rule: Whenever adding/updating public documentation screens, always update:
    1. `src/components/Footer.tsx` (product links in en & ar)
    2. `src/app/sitemap.ts` (SEO sitemap across all locales)
    3. `src/app/llms.txt/route.ts` (AI search engines & LLM crawler guides)
    4. `src/messages/en/metadata.json` and `src/messages/ar/metadata.json`

