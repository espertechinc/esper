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
package com.espertech.esper.client.soda;

import java.io.StringWriter;

/**
 * Between checks that a given value is in a range between a low endpoint and a high endpoint.
 * <p>
 * Closed and open ranges (endpoint included or excluded) are supported by this class, as is not-between.
 */
public class BetweenExpression extends ExpressionBase {
    private boolean isLowEndpointIncluded;
    private boolean isHighEndpointIncluded;
    private boolean isNotBetween;
    private static final long serialVersionUID = 4626033892510751123L;

    /**
     * Ctor.
     */
    public BetweenExpression() {
    }

    /**
     * Ctor, creates a between range check.
     *
     * @param datapoint provides the datapoint
     * @param lower     provides lower boundary
     * @param higher    provides upper boundary
     */
    public BetweenExpression(Expression datapoint, Expression lower, Expression higher) {
        this(datapoint, lower, higher, true, true, false);
    }

    /**
     * Ctor - for use to create an expression tree, without child expression.
     * <p>
     * Use add methods to add child expressions to acts upon.
     *
     * @param lowEndpointIncluded  true if the low endpoint is included, false if not
     * @param highEndpointIncluded true if the high endpoint is included, false if not
     * @param notBetween           true for not-between, false for between
     */
    public BetweenExpression(boolean lowEndpointIncluded, boolean highEndpointIncluded, boolean notBetween) {
        isLowEndpointIncluded = lowEndpointIncluded;
        isHighEndpointIncluded = highEndpointIncluded;
        isNotBetween = notBetween;
    }

    /**
     * Ctor.
     *
     * @param datapoint            provides the datapoint
     * @param lower                provides lower boundary
     * @param higher               provides upper boundary
     * @param lowEndpointIncluded  true if the low endpoint is included, false if not
     * @param highEndpointIncluded true if the high endpoint is included, false if not
     * @param notBetween           true for not-between, false for between
     */
    public BetweenExpression(Expression datapoint, Expression lower, Expression higher, boolean lowEndpointIncluded, boolean highEndpointIncluded, boolean notBetween) {
        this.getChildren().add(datapoint);
        this.getChildren().add(lower);
        this.getChildren().add(higher);

        isLowEndpointIncluded = lowEndpointIncluded;
        isHighEndpointIncluded = highEndpointIncluded;
        isNotBetween = notBetween;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.RELATIONAL_BETWEEN_IN;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (isLowEndpointIncluded && isHighEndpointIncluded) {
            this.getChildren().get(0).toEPL(writer, getPrecedence());
            writer.write(" between ");
            this.getChildren().get(1).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.write(" and ");
            this.getChildren().get(2).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        } else {
            this.getChildren().get(0).toEPL(writer, getPrecedence());
            writer.write(" in ");
            if (isLowEndpointIncluded) {
                writer.write('[');
            } else {
                writer.write('(');
            }
            this.getChildren().get(1).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.write(':');
            this.getChildren().get(2).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            if (isHighEndpointIncluded) {
                writer.write(']');
            } else {
                writer.write(')');
            }
        }
    }

    /**
     * True if the low endpoint is included.
     *
     * @return true for inclusive range.
     */
    public boolean isLowEndpointIncluded() {
        return isLowEndpointIncluded;
    }

    /**
     * Set to true to indicate that the low endpoint is included (the default).
     *
     * @param lowEndpointIncluded true for inclusive
     */
    public void setLowEndpointIncluded(boolean lowEndpointIncluded) {
        isLowEndpointIncluded = lowEndpointIncluded;
    }

    /**
     * True if the high endpoint is included.
     *
     * @return true for inclusive range.
     */
    public boolean isHighEndpointIncluded() {
        return isHighEndpointIncluded;
    }

    /**
     * Set to true to indicate that the high endpoint is included (the default).
     *
     * @param highEndpointIncluded true for inclusive
     */
    public void setHighEndpointIncluded(boolean highEndpointIncluded) {
        isHighEndpointIncluded = highEndpointIncluded;
    }

    /**
     * Returns true for not-between, or false for between range.
     *
     * @return false is the default range check, true checks if the value is outside of the range
     */
    public boolean isNotBetween() {
        return isNotBetween;
    }

    /**
     * Set to true for not-between, or false for between range.
     *
     * @param notBetween false is the default range check, true checks if the value is outside of the range
     */
    public void setNotBetween(boolean notBetween) {
        isNotBetween = notBetween;
    }
}
