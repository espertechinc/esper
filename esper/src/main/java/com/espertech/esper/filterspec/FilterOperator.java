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
package com.espertech.esper.filterspec;

/**
 * Defines the different operator types available for event filters.
 * <p>
 * Mathematical notation for defining ranges of floating point numbers is used as defined below:
 * <p>[a,b]  a closed range from value a to value b with the end-points a and b included in the range
 * <p>(a,b)  an open range from value a to value b with the end-points a and b not included in the range
 * <p>[a,b)  a half-open range from value a to value b with the end-point a included and end-point b not included
 * in the range
 * <p>(a,b]  a half-open range from value a to value b with the end-point a not included and end-point b included in the range
 */
public enum FilterOperator {
    /**
     * Exact matches (=).
     */
    EQUAL("="),

    /**
     * Exact not matche (!=).
     */
    NOT_EQUAL("!="),

    /**
     * Exact matches allowing null (is).
     */
    IS("is"),

    /**
     * Exact not matches allowing null (is not).
     */
    IS_NOT("is not"),

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
    NOT_RANGE_HALF_CLOSED("-(,]"),

    /**
     * List of values using the 'in' operator
     */
    IN_LIST_OF_VALUES("in"),

    /**
     * Not-in list of values using the 'not in' operator
     */
    NOT_IN_LIST_OF_VALUES("!in"),

    /**
     * Advanced-index
     */
    ADVANCED_INDEX("ai"),

    /**
     * Boolean expression filter operator
     */
    BOOLEAN_EXPRESSION("boolean_expr");

    private String textualOp;

    private FilterOperator(String textualOp) {
        this.textualOp = textualOp;
    }

    /**
     * Returns true for all range operators, false if not a range operator.
     *
     * @return true for ranges, false for anyting else
     */
    public boolean isRangeOperator() {
        if ((this == FilterOperator.RANGE_CLOSED) ||
                (this == FilterOperator.RANGE_OPEN) ||
                (this == FilterOperator.RANGE_HALF_OPEN) ||
                (this == FilterOperator.RANGE_HALF_CLOSED)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true for inverted range operators, false if not an inverted range operator.
     *
     * @return true for inverted ranges, false for anyting else
     */
    public boolean isInvertedRangeOperator() {
        if ((this == FilterOperator.NOT_RANGE_CLOSED) ||
                (this == FilterOperator.NOT_RANGE_OPEN) ||
                (this == FilterOperator.NOT_RANGE_HALF_OPEN) ||
                (this == FilterOperator.NOT_RANGE_HALF_CLOSED)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true for relational comparison operators which excludes the = equals operator, else returns false.
     *
     * @return true for lesser or greater -type operators, false for anyting else
     */
    public boolean isComparisonOperator() {
        if ((this == FilterOperator.LESS) ||
                (this == FilterOperator.LESS_OR_EQUAL) ||
                (this == FilterOperator.GREATER) ||
                (this == FilterOperator.GREATER_OR_EQUAL)) {
            return true;
        }
        return false;
    }

    /**
     * Parse the range operator from booleans describing whether the start or end values are exclusive.
     *
     * @param isInclusiveFirst true if low endpoint is inclusive, false if not
     * @param isInclusiveLast  true if high endpoint is inclusive, false if not
     * @param isNot            is true if this is an inverted range, or false if a regular range
     * @return FilterOperator for the combination inclusive or exclusive
     */
    public static FilterOperator parseRangeOperator(boolean isInclusiveFirst, boolean isInclusiveLast, boolean isNot) {
        if (isInclusiveFirst && isInclusiveLast) {
            if (isNot) {
                return FilterOperator.NOT_RANGE_CLOSED;
            } else {
                return FilterOperator.RANGE_CLOSED;
            }
        }
        if (isInclusiveFirst && !isInclusiveLast) {
            if (isNot) {
                return FilterOperator.NOT_RANGE_HALF_OPEN;
            } else {
                return FilterOperator.RANGE_HALF_OPEN;
            }
        }
        if (isInclusiveLast) {
            if (isNot) {
                return FilterOperator.NOT_RANGE_HALF_CLOSED;
            } else {
                return FilterOperator.RANGE_HALF_CLOSED;
            }
        }
        if (isNot) {
            return FilterOperator.NOT_RANGE_OPEN;
        } else {
            return FilterOperator.RANGE_OPEN;
        }
    }

    public String getTextualOp() {
        return textualOp;
    }

    public FilterOperator reversedRelationalOp() {
        if (this == LESS) {
            return GREATER;
        } else if (this == LESS_OR_EQUAL) {
            return GREATER_OR_EQUAL;
        } else if (this == GREATER) {
            return LESS;
        } else if (this == GREATER_OR_EQUAL) {
            return LESS_OR_EQUAL;
        }
        throw new IllegalArgumentException("Not a relational operator: " + this);
    }
}
