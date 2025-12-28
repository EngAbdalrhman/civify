# Proration Upgrade - Deferred Design

**Status**: Deferred for future implementation

## Overview
This document outlines the proration/credit calculation approach for subscription upgrades. Currently using simpler "queue after expiry" approach.

---

## Credit Calculation Cases

### Case 1: Monthly → Higher Monthly
- **Scenario**: User on Basic ($10/mo) upgrades to Pro ($20/mo)
- **Credit**: Remaining days × (daily rate of Basic)
- **Formula**: `credit = ($10/30) × remaining_days`
- **Charge**: `$20 - credit`

### Case 2: Annual → Monthly
- **Scenario**: User on Annual Basic ($100/yr) wants Monthly Pro ($25/mo)
- **Complexity**: Different billing periods
- **Option A**: Credit = remaining_months × ($100/12)
- **Option B**: No credit for downgrade timing

### Case 3: Monthly → Annual (Same Plan)
- **Scenario**: User on Monthly Pro ($25) → Annual Pro ($250)
- **Credit**: Remaining days value
- **Formula**: `credit = ($25/30) × remaining_days`

### Case 4: Annual → Annual Upgrade
- **Scenario**: Basic Annual ($100) → Pro Annual ($250)
- **Credit**: Proportional remaining value
- **Formula**: `credit = ($100/365) × remaining_days`

---

## Token Handling Complexity

When upgrading, remaining tokens need consideration:
- **Monthly tokens reset**: Lose remaining monthly allowance?
- **Extra purchased tokens**: Should carry over
- **New plan tokens**: Start fresh or add?

**Recommendation**: Carry over `extraTokens`, reset `currentTokenUsage` on upgrade

---

## Implementation Notes

When implementing proration:
1. Calculate `remainingValue` based on subscription type (monthly/annual)
2. Handle token carryover
3. Consider minimum charge threshold (e.g., min $1)
4. Handle edge cases (upgrade on last day, etc.)

---

## Current Approach: Queue After Expiry

For now, upgrades work as:
1. New subscription created with `startDate = currentSub.endDate`
2. Current subscription continues until expiry
3. No credit calculation needed
4. User pays full price for new plan
