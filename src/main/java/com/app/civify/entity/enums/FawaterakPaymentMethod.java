package com.app.civify.entity.enums;

/**
 * Fawaterak Payment Method IDs
 * Based on Fawaterak API documentation
 */
public enum FawaterakPaymentMethod {
    CARD(2, "Credit/Debit Card"),
    FAWRY(3, "Fawry"),
    MEEZA(4, "Meeza Digital"),
    MOBILE_WALLET(5, "Mobile Wallet"),
    BANK_INSTALLMENTS(6, "Bank Installments"),
    VALU(7, "valU"),
    SYMPL(8, "Sympl"),
    SOUHOOLA(9, "Souhoola"),
    GETGO(10, "GetGo"),
    FORSA(11, "Forsa"),
    HALAN(12, "Halan"),
    SHAHRY(13, "Shahry"),
    PREMIUM_CARD(14, "Premium Card"),
    CONTACT(15, "Contact"),
    AMAN(16, "Aman"),
    MASARY(17, "Masary");

    private final int id;
    private final String displayName;

    FawaterakPaymentMethod(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static FawaterakPaymentMethod fromId(int id) {
        for (FawaterakPaymentMethod method : values()) {
            if (method.id == id) {
                return method;
            }
        }
        return CARD; // Default to card
    }

    public static FawaterakPaymentMethod fromString(String name) {
        if (name == null || name.isEmpty()) {
            return CARD;
        }
        try {
            return valueOf(name.toUpperCase().replace("-", "_").replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Try matching by display name or common aliases
            String normalizedName = name.toLowerCase().trim();
            return switch (normalizedName) {
                case "visa", "mastercard", "card", "credit", "debit" -> CARD;
                case "fawry" -> FAWRY;
                case "meeza" -> MEEZA;
                case "wallet", "vodafone", "orange", "etisalat", "we" -> MOBILE_WALLET;
                default -> CARD;
            };
        }
    }
}
