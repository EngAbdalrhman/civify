# Paymob Payment Gateway Setup

This document provides instructions for configuring Paymob as a payment gateway.

## Required Environment Variables

| Variable | Description | Where to Find |
|----------|-------------|---------------|
| `PAYMOB_API_KEY` | API Key for authentication | [Dashboard](https://accept.paymob.com/portal2/en/settings) → Account Settings → API Key |
| `PAYMOB_CARD_INTEGRATION_ID` | Integration ID for card payments | Dashboard → Payment Integrations → Card integration |
| `PAYMOB_WALLET_INTEGRATION_ID` | Integration ID for wallet payments | Dashboard → Payment Integrations → Wallet integration |
| `PAYMOB_IFRAME_ID` | Iframe ID for hosted checkout | Dashboard → Online Checkout → Iframe ID |
| `PAYMOB_HMAC_SECRET` | HMAC secret for callback verification | Dashboard → Account Settings → HMAC Secret |
| `PAYMOB_ENABLED` | Enable/disable gateway (`true`/`false`) | Application property |

## Configuration

Add these to your environment or `.env` file:

```bash
PAYMOB_API_KEY=your_api_key_here
PAYMOB_CARD_INTEGRATION_ID=your_card_integration_id
PAYMOB_WALLET_INTEGRATION_ID=your_wallet_integration_id
PAYMOB_IFRAME_ID=your_iframe_id
PAYMOB_HMAC_SECRET=your_hmac_secret
PAYMOB_ENABLED=true
```

## Webhook Configuration

Configure webhook URL in Paymob Dashboard:
- **Callback URL**: `https://your-domain.com/api/v1/payments/callback/paymob`
- **Redirect URL**: `https://your-frontend.com/payment/result`

## Payment Flow

The implementation uses the legacy 3-step API flow:

1. **Authentication** (`POST /api/auth/tokens`)
   - Authenticates using `PAYMOB_API_KEY`
   - Returns auth token for subsequent requests

2. **Order Creation** (`POST /api/ecommerce/orders`)
   - Creates order with amount in cents (EGP × 100)
   - Returns order ID

3. **Payment Key** (`POST /api/acceptance/payment_keys`)
   - Generates payment key using `PAYMOB_INTEGRATION_ID`
   - Key used for iframe checkout

4. **Iframe Checkout**
   - User redirected to: `https://accept.paymob.com/api/acceptance/iframes/{IFRAME_ID}?payment_token={payment_key}`

5. **Callback Processing**
   - Paymob sends webhook with payment result
   - HMAC signature verified using `PAYMOB_HMAC_SECRET`

## HMAC Verification

Callback HMAC is calculated using these fields (concatenated in order):
- `amount_cents`, `created_at`, `currency`, `error_occured`, `has_parent_transaction`
- `id`, `integration_id`, `is_3d_secure`, `is_auth`, `is_capture`, `is_refunded`
- `is_standalone_payment`, `is_voided`, `order.id`, `owner`, `pending`
- `source_data.pan`, `source_data.sub_type`, `source_data.type`, `success`

Algorithm: **HMAC-SHA512**

## Test Credentials

For testing, use Paymob test credentials:
- Test card: `5123456789012346`
- Expiry: Any future date
- CVV: `123`

Refer to [Paymob Test Credentials](https://developers.paymob.com/egypt/test-credentials) for more details.

## API Documentation

- [Getting Started](https://developers.paymob.com/egypt/getting-started-egypt)
- [API Reference](https://developers.paymob.com/egypt/api-reference-guide)
- [HMAC Calculation](https://developers.paymob.com/egypt/manage-callback/hmac)
