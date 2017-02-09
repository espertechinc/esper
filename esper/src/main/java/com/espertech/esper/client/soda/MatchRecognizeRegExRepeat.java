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

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Match-recognize pattern descriptor for repetition
 */
public class MatchRecognizeRegExRepeat implements Serializable {
    private static final long serialVersionUID = -4542225055062618542L;
    private Expression low;
    private Expression high;
    private Expression single;

    /**
     * Ctor.
     */
    public MatchRecognizeRegExRepeat() {
    }

    /**
     * Ctor.
     *
     * @param low    low endpoint or null
     * @param high   high endpoint or null
     * @param single exact-matches repetition, should be null if low or high is provided
     */
    public MatchRecognizeRegExRepeat(Expression low, Expression high, Expression single) {
        this.low = low;
        this.high = high;
        this.single = single;
    }

    /**
     * Returns the low endpoint.
     *
     * @return low endpoint
     */
    public Expression getLow() {
        return low;
    }

    /**
     * Sets the low endpoint.
     *
     * @param low low endpoint
     */
    public void setLow(Expression low) {
        this.low = low;
    }

    /**
     * Returns the high endpoint.
     *
     * @return high endpoint
     */
    public Expression getHigh() {
        return high;
    }

    /**
     * Sets the high endpoint.
     *
     * @param high high endpoint
     */
    public void setHigh(Expression high) {
        this.high = high;
    }

    /**
     * Returns the exact-num-matches endpoint.
     *
     * @return exact-num-matches endpoint
     */
    public Expression getSingle() {
        return single;
    }

    /**
     * Sets the exact-num-matches endpoint.
     *
     * @param single exact-num-matches endpoint
     */
    public void setSingle(Expression single) {
        this.single = single;
    }

    /**
     * Render as EPL.
     *
     * @param writer to write to
     */
    public void writeEPL(StringWriter writer) {
        writer.append("{");
        if (single != null) {
            single.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        } else {
            if (low != null) {
                low.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            }
            writer.append(",");
            if (high != null) {
                high.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            }
        }
        writer.append("}");
    }
}