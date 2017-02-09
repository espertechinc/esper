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

/**
 * Enum for match recognize pattern atom types.
 */
public enum MatchRecogizePatternElementType {
    /**
     * For single multiplicity.
     */
    SINGLE(""),

    /**
     * For greedy '*' multiplicity.
     */
    ZERO_TO_MANY("*"),

    /**
     * For greedy '+' multiplicity.
     */
    ONE_TO_MANY("+"),

    /**
     * For greedy '?' multiplicity.
     */
    ONE_OPTIONAL("?"),

    /**
     * For reluctant '*' multiplicity.
     */
    ZERO_TO_MANY_RELUCTANT("*?"),

    /**
     * For reluctant '+' multiplicity.
     */
    ONE_TO_MANY_RELUCTANT("+?"),

    /**
     * For reluctant '?' multiplicity.
     */
    ONE_OPTIONAL_RELUCTANT("??");

    private String text;

    MatchRecogizePatternElementType(String text) {
        this.text = text;
    }

    /**
     * Returns the multiplicity text.
     *
     * @return text
     */
    public String getText() {
        return text;
    }
}
