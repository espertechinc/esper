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
 * Context condition that start/initiated or ends/terminates context partitions based on a pattern.
 */
public class ContextDescriptorConditionPattern implements ContextDescriptorCondition {

    private static final long serialVersionUID = -481920039587982117L;
    private PatternExpr pattern;
    private boolean inclusive;  // statements declaring the context are inclusive of the events matching the pattern
    private boolean now;        // statements declaring the context initiate now and matching the pattern

    /**
     * Ctor.
     */
    public ContextDescriptorConditionPattern() {
    }

    /**
     * Ctor.
     *
     * @param pattern   pattern expression
     * @param inclusive if the events of the pattern should be included in the contextual statements
     * @param now       indicator whether "now"
     */
    public ContextDescriptorConditionPattern(PatternExpr pattern, boolean inclusive, boolean now) {
        this.pattern = pattern;
        this.inclusive = inclusive;
        this.now = now;
    }

    /**
     * Returns the pattern expression.
     *
     * @return pattern
     */
    public PatternExpr getPattern() {
        return pattern;
    }

    /**
     * Sets the pattern expression.
     *
     * @param pattern to set
     */
    public void setPattern(PatternExpr pattern) {
        this.pattern = pattern;
    }

    /**
     * Return the inclusive flag, meaning events that constitute the pattern match should be considered for context-associated statements.
     *
     * @return inclusive flag
     */
    public boolean isInclusive() {
        return inclusive;
    }

    /**
     * Set the inclusive flag, meaning events that constitute the pattern match should be considered for context-associated statements.
     *
     * @param inclusive inclusive flag
     */
    public void setInclusive(boolean inclusive) {
        this.inclusive = inclusive;
    }

    /**
     * Returns "now" indicator
     *
     * @return "now" indicator
     */
    public boolean isNow() {
        return now;
    }

    /**
     * Sets "now" indicator
     *
     * @param now "now" indicator
     */
    public void setNow(boolean now) {
        this.now = now;
    }

    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        if (now) {
            writer.write("@now and");
        }
        writer.write("pattern [");
        if (pattern != null) {
            pattern.toEPL(writer, PatternExprPrecedenceEnum.MINIMUM, formatter);
        }
        writer.write("]");
        if (inclusive) {
            writer.write("@Inclusive");
        }
    }
}
