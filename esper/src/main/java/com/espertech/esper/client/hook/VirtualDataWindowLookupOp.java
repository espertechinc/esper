/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.client.hook;

/**
 * Enumeration for indicating the type of operator for a lookup against a virtual data window, see {@link VirtualDataWindowLookupContext}.
 */
public enum VirtualDataWindowLookupOp {
    /**
     * Equals (=).
     */
    EQUALS("="),

    /**
     * Less (&lt;).
     */
    LESS("<"),

    /**
     * Less or equal (&lt;=).
     */
    LESS_OR_EQUAL("<="),

    /**
     * Greater or equal (&gt;=).
     */
    GREATER_OR_EQUAL(">="),

    /**
     * Greater (&gt;).
     */
    GREATER(">"),

    /**
     * Range contains neither endpoint, i.e. (a,b)
     */
    RANGE_OPEN("(,)"),

    /**
     * Range contains low and high endpoint, i.e. [a,b]
     */
    RANGE_CLOSED("[,]"),

    /**
     * Range includes low endpoint but not high endpoint, i.e. [a,b)
     */
    RANGE_HALF_OPEN("[,)"),

    /**
     * Range includes high endpoint but not low endpoint, i.e. (a,b]
     */
    RANGE_HALF_CLOSED("(,]"),

    /**
     * Inverted-Range contains neither endpoint, i.e. (a,b)
     */
    NOT_RANGE_OPEN("-(,)"),

    /**
     * Inverted-Range contains low and high endpoint, i.e. [a,b]
     */
    NOT_RANGE_CLOSED("-[,]"),

    /**
     * Inverted-Range includes low endpoint but not high endpoint, i.e. [a,b)
     */
    NOT_RANGE_HALF_OPEN("-[,)"),

    /**
     * Inverted-Range includes high endpoint but not low endpoint, i.e. (a,b]
     */
    NOT_RANGE_HALF_CLOSED("-(,]");

    private final String op;

    private VirtualDataWindowLookupOp(String op) {
        this.op = op;
    }

    /**
     * Returns the string-value of the operator.
     *
     * @return operator string value
     */
    public String getOp() {
        return op;
    }

    /**
     * Map the operator from a string-value.
     *
     * @param stringOp to map from
     * @return operator
     * @throws IllegalArgumentException if the string operator cannot be understood
     */
    public static VirtualDataWindowLookupOp fromOpString(String stringOp) {
        for (VirtualDataWindowLookupOp op : VirtualDataWindowLookupOp.values()) {
            if (op.getOp().equals(stringOp)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Failed to recognize operator '" + stringOp + "'");
    }
}
