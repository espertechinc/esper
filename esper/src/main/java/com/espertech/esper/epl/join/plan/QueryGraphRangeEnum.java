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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.type.RelationalOpEnum;

public enum QueryGraphRangeEnum {
    /**
     * Less (&lt;).
     */
    LESS(false, "<"),

    /**
     * Less or equal (&lt;=).
     */
    LESS_OR_EQUAL(false, "<="),

    /**
     * Greater or equal (&gt;=).
     */
    GREATER_OR_EQUAL(false, ">="),

    /**
     * Greater (&gt;).
     */
    GREATER(false, ">"),

    /**
     * Range contains neither endpoint, i.e. (a,b)
     */
    RANGE_OPEN(true, "(,)"),

    /**
     * Range contains low and high endpoint, i.e. [a,b]
     */
    RANGE_CLOSED(true, "[,]"),

    /**
     * Range includes low endpoint but not high endpoint, i.e. [a,b)
     */
    RANGE_HALF_OPEN(true, "[,)"),

    /**
     * Range includes high endpoint but not low endpoint, i.e. (a,b]
     */
    RANGE_HALF_CLOSED(true, "(,]"),

    /**
     * Inverted-Range contains neither endpoint, i.e. (a,b)
     */
    NOT_RANGE_OPEN(true, "-(,)"),

    /**
     * Inverted-Range contains low and high endpoint, i.e. [a,b]
     */
    NOT_RANGE_CLOSED(true, "-[,]"),

    /**
     * Inverted-Range includes low endpoint but not high endpoint, i.e. [a,b)
     */
    NOT_RANGE_HALF_OPEN(true, "-[,)"),

    /**
     * Inverted-Range includes high endpoint but not low endpoint, i.e. (a,b]
     */
    NOT_RANGE_HALF_CLOSED(true, "-(,])");

    private boolean range;
    private String stringOp;

    QueryGraphRangeEnum(boolean range, String stringOp) {
        this.range = range;
        this.stringOp = stringOp;
    }

    public static QueryGraphRangeEnum mapFrom(FilterOperator op) {
        if (op == FilterOperator.GREATER) {
            return GREATER;
        } else if (op == FilterOperator.GREATER_OR_EQUAL) {
            return GREATER_OR_EQUAL;
        } else if (op == FilterOperator.LESS) {
            return LESS;
        } else if (op == FilterOperator.LESS_OR_EQUAL) {
            return LESS_OR_EQUAL;
        } else if (op == FilterOperator.RANGE_OPEN) {
            return RANGE_OPEN;
        } else if (op == FilterOperator.RANGE_HALF_CLOSED) {
            return RANGE_HALF_CLOSED;
        } else if (op == FilterOperator.RANGE_HALF_OPEN) {
            return RANGE_HALF_OPEN;
        } else if (op == FilterOperator.RANGE_CLOSED) {
            return RANGE_CLOSED;
        } else if (op == FilterOperator.NOT_RANGE_OPEN) {
            return NOT_RANGE_OPEN;
        } else if (op == FilterOperator.NOT_RANGE_HALF_CLOSED) {
            return NOT_RANGE_HALF_CLOSED;
        } else if (op == FilterOperator.NOT_RANGE_HALF_OPEN) {
            return NOT_RANGE_HALF_OPEN;
        } else if (op == FilterOperator.NOT_RANGE_CLOSED) {
            return NOT_RANGE_CLOSED;
        } else {
            return null;
        }
    }

    public static QueryGraphRangeEnum mapFrom(RelationalOpEnum relationalOpEnum) {
        if (relationalOpEnum == RelationalOpEnum.GE) {
            return GREATER_OR_EQUAL;
        } else if (relationalOpEnum == RelationalOpEnum.GT) {
            return GREATER;
        } else if (relationalOpEnum == RelationalOpEnum.LT) {
            return LESS;
        } else if (relationalOpEnum == RelationalOpEnum.LE) {
            return LESS_OR_EQUAL;
        } else {
            throw new IllegalArgumentException("Failed to map code " + relationalOpEnum);
        }
    }

    public boolean isRange() {
        return range;
    }

    public boolean isIncludeStart() {
        if (!this.isRange()) {
            throw new UnsupportedOperationException("Cannot determine endpoint-start included for op " + this);
        }
        return this == QueryGraphRangeEnum.RANGE_HALF_OPEN || this == QueryGraphRangeEnum.RANGE_CLOSED ||
                this == QueryGraphRangeEnum.NOT_RANGE_HALF_OPEN || this == QueryGraphRangeEnum.NOT_RANGE_CLOSED;
    }

    public boolean isIncludeEnd() {
        if (!this.isRange()) {
            throw new UnsupportedOperationException("Cannot determine endpoint-end included for op " + this);
        }
        return this == QueryGraphRangeEnum.RANGE_HALF_CLOSED || this == QueryGraphRangeEnum.RANGE_CLOSED ||
                this == QueryGraphRangeEnum.NOT_RANGE_HALF_CLOSED || this == QueryGraphRangeEnum.NOT_RANGE_CLOSED;
    }

    public static QueryGraphRangeEnum getRangeOp(boolean includeStart, boolean includeEnd, boolean isInverted) {
        if (!isInverted) {
            if (includeStart) {
                if (includeEnd) {
                    return RANGE_CLOSED;
                }
                return RANGE_HALF_OPEN;
            } else {
                if (includeEnd) {
                    return RANGE_HALF_CLOSED;
                }
                return RANGE_OPEN;
            }
        } else {
            if (includeStart) {
                if (includeEnd) {
                    return NOT_RANGE_CLOSED;
                }
                return NOT_RANGE_HALF_OPEN;
            } else {
                if (includeEnd) {
                    return NOT_RANGE_HALF_CLOSED;
                }
                return NOT_RANGE_OPEN;
            }
        }
    }

    public boolean isRangeInverted() {
        return isRange() && (this == NOT_RANGE_HALF_CLOSED || this == NOT_RANGE_HALF_OPEN ||
                this == NOT_RANGE_OPEN || this == NOT_RANGE_CLOSED);
    }

    public String getStringOp() {
        return stringOp;
    }
}
