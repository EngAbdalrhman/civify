# Production Monitoring Setup Guide

## Overview

This guide provides step-by-step instructions for setting up comprehensive monitoring for the Civify backend using **Sentry** for error tracking and **Prometheus + Grafana** for metrics visualization.

---

## 1. Sentry Setup (Error Tracking)

### **Step 1: Create Sentry Project**

1. Go to [sentry.io](https://sentry.io) and create account
2. Create new project:
   - Platform: **Java/Spring Boot**
   - Project name: **civify-backend**
3. Copy your **DSN** (Data Source Name)

### **Step 2: Add Dependencies**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-starter</artifactId>
    <version>7.0.0</version>
</dependency>
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-logback</artifactId>
    <version>7.0.0</version>
</dependency>
```

### **Step 3: Configuration**

```properties
# application.properties
sentry.dsn=${SENTRY_DSN}
sentry.environment=${SPRING_PROFILES_ACTIVE:production}
sentry.traces-sample-rate=0.1
sentry.enable-tracing=true
sentry.send-default-pii=false

sentry.tags.application=civify-backend
sentry.tags.version=${app.version:1.0.0}
```

### **Step 4: Environment Variables**

```bash
# .env (production)
SENTRY_DSN=https://your-sentry-dsn@sentry.io/project-id
SPRING_PROFILES_ACTIVE=production
```

### **Step 5: Custom Alerts**

In Sentry dashboard:
1. **Alerts** → **Create Alert Rule**
2. Set conditions:
   - **Payment webhook failures** > 5 in 1 hour
   - **Permission denied errors** > 50 in 1 hour
   - **Token limit exceeded** > 100 in 1 hour

---

## 2. Prometheus Setup (Metrics Collection)

### **Step 1: Add Dependencies**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### **Step 2: Actuator Configuration**

```properties
# application.properties
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true

# Security for actuator endpoints
management.endpoints.web.base-path=/actuator
management.server.port=9090
```

### **Step 3: Install Prometheus Server**

```bash
# Using Docker
docker run -d \
  --name prometheus \
  -p 9091:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

### **Step 4: Prometheus Configuration**

Create `prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'civify-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:9090']
        labels:
          application: 'civify'
          environment: 'production'
```

---

## 3. Grafana Setup (Visualization)

### **Step 1: Install Grafana**

```bash
# Using Docker
docker run -d \
  --name grafana \
  -p 3000:3000 \
  -v grafana-storage:/var/lib/grafana \
  grafana/grafana
```

### **Step 2: Configure Data Source**

1. Access Grafana: `http://localhost:3000` (admin/admin)
2. **Configuration** → **Data Sources** → **Add data source**
3. Select **Prometheus**
4. Set URL: `http://prometheus:9091`
5. Click **Save & Test**

### **Step 3: Create Dashboard**

Import dashboard with these key panels:
- Payment webhook success rate
- Token usage trends
- Permission denial rates
- API response times
- Active subscriptions count

---

## 4. Custom Metrics Implementation

### **Webhook Monitoring**

```java
@Aspect
@Component
public class WebhookMonitoringAspect {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Around("execution(* com.app.civify.service.payment.*Gateway.handleCallback(..))")
    public Object monitorWebhook(ProceedingJoinPoint joinPoint) throws Throwable {
        String gateway = extractGatewayName(joinPoint);
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            PaymentCallbackResult result = (PaymentCallbackResult) joinPoint.proceed();
            
            Counter.builder("civify.payment.webhook")
                    .tag("gateway", gateway)
                    .tag("status", result.isSuccess() ? "success" : "failure")
                    .register(meterRegistry)
                    .increment();
            
            sample.stop(Timer.builder("civify.payment.webhook.duration")
                    .tag("gateway", gateway)
                    .register(meterRegistry));
            
            return result;
        } catch (Throwable e) {
            Counter.builder("civify.payment.webhook")
                    .tag("gateway", gateway)
                    .tag("status", "error")
                    .register(meterRegistry)
                    .increment();
            throw e;
        }
    }
}
```

---

## 5. Production Deployment Checklist

- [ ] Sentry DSN configured in environment variables
- [ ] Prometheus scraping `/actuator/prometheus`
- [ ] Grafana dashboards created and alerts configured
- [ ] Health checks accessible at `/actuator/health`
- [ ] Metrics endpoint secured or restricted
- [ ] Alert channels configured (Email, Slack)
- [ ] On-call rotation established

---

## Summary

✅ **Sentry** - Real-time error tracking  
✅ **Prometheus** - Metrics collection  
✅ **Grafana** - Dashboard visualization  
✅ **Custom metrics** - Webhook, tokens, permissions  
✅ **Alerts** - Proactive issue detection
