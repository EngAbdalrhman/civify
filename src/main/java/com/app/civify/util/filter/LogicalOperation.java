package com.app.civify.util.filter;

/**
 * Logical operations for combining filter conditions Supports complex nested
 * logical expressions
 */
public enum LogicalOperation {
    AND("AND", "All conditions must be true"),
    OR("OR", "At least one condition must be true"),
    NOT("NOT", "Negates the condition");

    private final String label;
    private final String description;

    LogicalOperation(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get operation by label
     */
    public static LogicalOperation fromLabel(String label) {
        for (LogicalOperation op : values()) {
            if (op.label.equalsIgnoreCase(label)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown logical operation: " + label);
    }

    /**
     * Check if operation requires multiple operands
     */
    public boolean requiresMultipleOperands() {
        return this == AND || this == OR;
    }

    /**
     * Check if operation is unary (single operand)
     */
    public boolean isUnary() {
        return this == NOT;
    }
}
