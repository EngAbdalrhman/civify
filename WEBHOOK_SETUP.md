# Fawaterak Webhook Setup Guide

## Why ngrok for Backend Only?

### Frontend (localhost:9002)
- ✅ **No ngrok needed** - Users access it directly in their browser
- The browser runs on the user's machine and connects to localhost:9002
- Fawaterak redirects users to these URLs via the browser (client-side)

### Backend (localhost:8077)
- ⚠️ **Ngrok required for webhooks** - Fawaterak servers need to reach it
- Webhooks are **server-to-server** communications from Fawaterak → Your Backend
- Fawaterak's servers cannot access `localhost:8077` (it's private to your machine)
- Ngrok creates a public tunnel: `Internet → ngrok URL → localhost:8077`

### Flow Diagram
```
User Payment Flow (no ngrok needed):
User Browser → Fawaterak Payment Page → Redirect → localhost:9002/payment/success

Webhook Flow (ngrok required):
Fawaterak Servers → ngrok URL → localhost:8077/apis/v1/payments/webhook/fawaterak
```

---

## Webhook Handler: All Transaction Types Supported ✅

The webhook endpoint **handles ALL transaction types** with a single URL:

### Supported Statuses
The `FawaterakPaymentGateway.handleCallback()` method processes:

| Fawaterak Status | Transaction Status | Database Updated |
|------------------|-------------------|------------------|
| `paid` | `COMPLETED` ✅ | Yes + completion timestamp |
| `failed` | `FAILED` ❌ | Yes |
| `expired` | `FAILED` ❌ | Yes |
| Other/Unknown | No change ⚠️ | Logged only |

### Code Reference
```java
// FawaterakPaymentGateway.java - handleCallback()
if ("paid".equalsIgnoreCase(status)) {
    transaction.setStatus(PaymentStatus.COMPLETED);
    transaction.setCompletedAt(LocalDateTime.now());
    result.setSuccess(true);
} else if ("failed".equalsIgnoreCase(status) || "expired".equalsIgnoreCase(status)) {
    transaction.setStatus(PaymentStatus.FAILED);
    result.setSuccess(false);
}
paymentRepository.save(transaction); // ✅ Always saves to database
```

### Single Webhook URL for All Types
You **should use the same webhook URL** for:
- ✅ Paid transactions webhook
- ✅ Failed transactions webhook  
- ✅ Refund transactions webhook
- ✅ Cancellation webhook (Fawry/Aman/Masary)

The handler automatically detects the `invoice_status` field and processes accordingly.

---

## Local Development Setup

### 1. Install ngrok
```bash
# Download from https://ngrok.com/download
# Or via package manager:
choco install ngrok  # Windows
brew install ngrok   # macOS
```

### 2. Start ngrok tunnel
```bash
ngrok http 8077
```

You'll see output like:
```
Forwarding  https://abc123def456.ngrok.io -> http://localhost:8077
```

### 3. Update Fawaterak Dashboard

#### Redirect URLs (Frontend - No ngrok)
- **Success URL**: `http://localhost:9002/dashboard/payment/success`
- **Fail URL**: `http://localhost:9002/dashboard/payment/failure`

#### Webhook URLs (Backend - Use ngrok)
Set **all webhook fields** to the same ngrok URL:
```
https://abc123def456.ngrok.io/apis/v1/payments/webhook/fawaterak
```

Apply this URL to:
- Paid transactions webhook
- Failed transactions webhook
- Refund transactions webhook
- Cancellation webhook

---

## Production Deployment

### Redirect URLs
Replace `localhost:9002` with your production frontend domain:
```
https://yourdomain.com/dashboard/payment/success
https://yourdomain.com/dashboard/payment/failure
```

### Webhook URLs
Replace ngrok URL with your production backend domain:
```
https://api.yourdomain.com/apis/v1/payments/webhook/fawaterak
```

Use the **same URL for all webhook types**.

---

## Testing Webhooks Locally

1. Start backend: `mvn spring-boot:run` (port 8077)
2. Start ngrok: `ngrok http 8077`
3. Copy ngrok URL to Fawaterak dashboard
4. Make a test payment
5. Watch backend logs for webhook receipt:
   ```
   INFO: Received payment webhook for gateway fawaterak: {invoice_id=123, invoice_status=paid}
   INFO: Handling Fawaterak callback: {invoice_id=123, invoice_status=paid}
   ```

---

## Troubleshooting

**Webhooks not arriving?**
- ✅ Check ngrok is running: `curl https://your-ngrok-url.ngrok.io/health`
- ✅ Verify Fawaterak dashboard has correct ngrok URL
- ✅ Check ngrok web interface: `http://localhost:4040` for request logs
- ✅ Ensure backend endpoint is public (no authentication required for webhook)

**Transaction status not updating?**
- Check backend logs for webhook processing
- Verify `invoice_id` matches `PaymentTransaction.orderId` in database
- Ensure `invoice_status` field is present in webhook payload
