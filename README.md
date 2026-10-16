# Civify Backend Core (`civify`)

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-emerald)](https://www.gnu.org/licenses/agpl-3.0.html)

The enterprise core API backend for [Civify](https://civify.cv): AI resume parsing, ATS scoring, CV tailoring, document OCR, payment routing, and MCP agent backend services.

For full ecosystem overview, see the [Unified Civify README](../README.md).

---

## 🌟 Architecture & Features

- **Multi-Format Ingestion**: Parses PDF, DOCX, and scanned documents with Apache PDFBox, Apache POI, and Tesseract OCR (Tess4J).
- **AI Orchestration**: Google Gemini 3 Flash and Vertex AI integration with automatic fallback to Gemini 2.5 Flash.
- **Dual-Engine ATS Scoring**: Combines hybrid semantic AI evaluation with deterministic keyword gap analysis.
- **Intelligent CV Tailoring**: Automatically optimizes candidate bullet points and achievements against job specifications.
- **PII Redaction**: Privacy-first candidate anonymization redacting sensitive contact details, addresses, and IDs.
- **Scoped API Key Engine**: Fine-grained access control (`cv:parse`, `cv:tailor`, `ats:score`, `pii:mask`, etc.) using `@RequiresScope`.
- **MCP Account Link Integration**: Secure browser-based OAuth 2.0 code exchange via Redis `GETDEL` single-use tokens.
- **Multi-Gateway Payment Engine**: Paymob (Cards, Mobile Wallets in EGP), Fawaterak, and Dodo Payments (USD) with MaxMind GeoLite2 country-based routing.
- **High-Performance Infrastructure**: PostgreSQL with Flyway database migrations, Redis caching, Bucket4j rate limiting, and Cloudflare R2 object storage.

---

## 🚀 Getting Started

### Prerequisites
- Java 21 (JDK 21 LTS)
- Maven 3.9+
- PostgreSQL 16+
- Redis 7+

### Running Locally

```bash
# Set required environment variables
export DB_URL="jdbc:postgresql://localhost:5432/civify"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password"
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export JWT_SECRET="your_secure_jwt_secret_minimum_64_characters_long_for_hmac_sha512"
export GEMINI_API_KEY="your_gemini_api_key"

# Run Spring Boot
mvn spring-boot:run
```

The application runs on port `8077` by default with context path `/apis`.

### API Documentation (OpenAPI / Swagger)
- Swagger UI: `http://localhost:8077/apis/civify-docs-ui`
- OpenAPI JSON: `http://localhost:8077/apis/civify-docs-api`

---

## 📄 License

This project is licensed under the GNU Affero General Public License v3.0 or later
(AGPL-3.0-or-later). See [LICENSE](LICENSE).

The Civify name and logo are not granted by this license. Commercial licensing
enquiries are available at [civify.cv](https://civify.cv).
