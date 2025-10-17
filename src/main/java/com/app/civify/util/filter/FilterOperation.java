package com.app.civify.util.filter;

/**
 * Enhanced FilterOperation enum with comprehensive filtering capabilities
 * Supports various data types and operations for generic filtering
 */
public enum FilterOperation {
    // Comparison Operations
    GREATER_THAN("gt", "Greater than", true),
    LESS_THAN("lt", "Less than", true),
    GREATER_THAN_EQUAL("gte", "Greater than or equal", true),
    LESS_THAN_EQUAL("lte", "Less than or equal", true),
    NOT_EQUAL("not_equal", "Not equal", false),
    EQUAL("equal", "Equal", false),
    // String Operations
    MATCH("match", "Contains (case insensitive)", false),
    MATCH_START("match_start", "Starts with (case insensitive)", false),
    MATCH_END("match_end", "Ends with (case insensitive)", false),
    MATCH_EXACT("match_exact", "Exact match (case sensitive)", false),
    MATCH_REGEX("match_regex", "Regular expression match", false),
    // Collection Operations
    IN_LONG_LIST("in_Long_List", "In long list", false),
    NOT_IN_LONG_LIST("not_in_Long_List", "Not in long list", false),
    IN_STRING_LIST("in_String_List", "In string list", false),
    NOT_IN_STRING_LIST("not_in_String_List", "Not in string list", false),
    IN_INTEGER_LIST("in_Integer_List", "In integer list", false),
    NOT_IN_INTEGER_LIST("not_in_Integer_List", "Not in integer list", false),
    // Null Operations
    IS_NOT_NULL("is_not_null", "Is not null", false),
    IS_NULL("is_null", "Is null", false),
    // Date Operations
    DAY_EQUAL("day_equal", "Day equals", false),
    DATE_GREATER_THAN("date_gt", "Date greater than", true),
    DATE_GREATER_THAN_EQUAL("date_gte", "Date greater than or equal", true),
    DATE_LESS_THAN("date_lt", "Date less than", true),
    DATE_LESS_THAN_EQUAL("date_lte", "Date less than or equal", true),
    DATE_BETWEEN("date_between", "Date between", false),
    DATE_RANGE("date_range", "Date in range", false),
    // Numeric Range Operations
    BETWEEN("between", "Between values", false),
    NOT_BETWEEN("not_between", "Not between values", false),
    // Special Operations
    FULL_TEXT_SEARCH("full_text_search", "Full text search", false);

    private final String label;
    private final String description;
    private final boolean requiresComparable;

    FilterOperation(String label, String description, boolean requiresComparable) {
        this.label = label;
        this.description = description;
        this.requiresComparable = requiresComparable;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresComparable() {
        return requiresComparable;
    }

    /**
     * Get operation by label
     */
    public static FilterOperation fromLabel(String label) {
        for (FilterOperation op : values()) {
            if (op.label.equals(label)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown filter operation: " + label);
    }

    /**
     * Check if operation requires a value
     */
    public boolean requiresValue() {
        return this != IS_NULL && this != IS_NOT_NULL;
    }

    /**
     * Check if operation supports multiple values
     */
    public boolean supportsMultipleValues() {
        return this == IN_LONG_LIST || this == NOT_IN_LONG_LIST
                || this == IN_STRING_LIST || this == NOT_IN_STRING_LIST
                || this == IN_INTEGER_LIST || this == NOT_IN_INTEGER_LIST
                || this == BETWEEN || this == NOT_BETWEEN
                || this == DATE_BETWEEN || this == DATE_RANGE;
    }
}
