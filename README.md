# Civify - AI-Powered CV/Resume Management Platform

Civify is a comprehensive CV/resume building and management platform that leverages AI to help users create, tailor, and manage their professional documents. Built with Spring Boot, it offers a subscription-based service with both free and premium tiers.

## Features

### ✅ Implemented Features

#### Authentication & Security

- **Multi-method Authentication**: Support for OAuth2 and traditional username/password login
- **Email Verification**: Mandatory email verification during registration
- **Two-Factor Authentication (2FA)**: Optional OTP-based 2FA for enhanced security
- **Role-Based Access Control**: Different permissions for users, licensed users, and admins
- **JWT Token Management**: Secure token-based authentication with refresh token rotation

#### AI-Powered CV Services

- **CV Parsing**: AI-powered extraction of structured data from uploaded CVs using Gemini AI
- **CV Tailoring**: Intelligent customization of CVs to match specific job descriptions
- **Multi-Provider AI Support**: Configurable AI providers with automatic fallback mechanisms
- **Retry & Error Handling**: Robust error handling with exponential backoff for AI calls

#### Subscription Management

- **Flexible Plans**: Free and premium subscription tiers with different feature access
- **Usage Tracking**: Real-time monitoring of CV generations and API calls with monthly limits
- **Automatic Plan Transitions**: Seamless activation/deactivation of subscriptions
- **Licensed User System**: Special status for premium users with enhanced capabilities

#### Payment Integration

- **Multi-Gateway Support**: Extensible payment gateway system (Paymob integration)
- **Billing Cycles**: Monthly and annual subscription options
- **Promo Code System**: Discount codes with usage tracking and validation
- **Payment Verification**: Secure payment processing with transaction management

#### User Management

- **Profile Management**: Comprehensive user profile filling and management
- **Usage Analytics**: Detailed tracking of user activities and limits
- **Notification System**: Email notifications for important events

#### API & Integration

- **RESTful APIs**: Well-documented APIs for licensed users
- **Swagger Documentation**: Interactive API documentation
- **Generic Filtering**: Advanced filtering capabilities for data retrieval
- **Masked Logging**: Secure logging with sensitive data masking

#### Admin Features

- **Admin Dashboard**: Comprehensive admin interface for user and license management
- **License Management**: Ability to grant/revoke user licenses
- **System Monitoring**: Built-in monitoring and logging capabilities

### 🚧 Planned Features (Not Yet Implemented)

#### Advanced CV Features

- **CV Generation Wizard**: Step-by-step CV creation with AI assistance
- **Cover Letter Generation**: Automated cover letter creation
- **PDF Processing**: Advanced PDF extraction and manipulation
- **Watermark System**: Branded watermarks for free plan CVs
- **CV Templates**: Pre-built templates similar to FlowCV

#### Social & Sharing

- **CV Sharing**: Public sharing and posting of CVs
- **Community Features**: User community for networking and collaboration

#### Infrastructure & DevOps

- **CI/CD Pipeline**: Automated deployment and testing pipeline
- **ELK Stack Logging**: Centralized logging with Elasticsearch, Logstash, and Kibana

## Technology Stack

- **Backend**: Spring Boot 3.x
- **Database**: PostgreSQL with JPA/Hibernate
- **Security**: Spring Security with JWT
- **AI Integration**: Google Gemini AI (extensible to other providers)
- **Payment**: Paymob payment gateway (extensible architecture)
- **Caching**: Redis for session and data caching
- **Documentation**: Swagger/OpenAPI
- **Build Tool**: Maven
- **Testing**: JUnit 5 with comprehensive test coverage

## Architecture

The application follows a microservices-ready architecture with:

- Layered architecture (Controller → Service → Repository)
- Strategy pattern for payment gateways and AI providers
- Event-driven notifications
- Transactional service methods
- Comprehensive error handling and logging

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+
- PostgreSQL
- Redis (optional, for caching)

### Configuration

Configure the following in `application.properties`:

- Database connection settings
- AI provider API keys (Gemini)
- Payment gateway credentials
- Email service configuration

### Running the Application

```bash
mvn spring-boot:run
```

The application will start on port 8080 with Swagger documentation available at `/swagger-ui.html`.

## API Documentation

Licensed users can access comprehensive API documentation through Swagger UI, including endpoints for:

- CV parsing and generation
- Subscription management
- User profile operations
- Usage analytics

## Contributing

This project is under active development. Features marked as "planned" are prioritized for future releases based on user feedback and business requirements.
