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
package com.espertech.esper.rowregex;

/**
 * Enum for NFA types.
 */
public enum RegexNFATypeEnum {
    /**
     * For single multiplicity.
     */
    SINGLE(false, false, null, ""),

    /**
     * For greedy '*' multiplicity.
     */
    ZERO_TO_MANY(true, true, true, "*"),

    /**
     * For greedy '+' multiplicity.
     */
    ONE_TO_MANY(true, false, true, "+"),

    /**
     * For greedy '?' multiplicity.
     */
    ONE_OPTIONAL(false, true, true, "?"),

    /**
     * For reluctant '*' multiplicity.
     */
    ZERO_TO_MANY_RELUCTANT(true, true, false, "*?"),

    /**
     * For reluctant '+' multiplicity.
     */
    ONE_TO_MANY_RELUCTANT(true, false, false, "+?"),

    /**
     * For reluctant '?' multiplicity.
     */
    ONE_OPTIONAL_RELUCTANT(false, true, false, "??");

    private boolean multipleMatches;
    private boolean optional;
    private Boolean greedy;
    private String text;

    private RegexNFATypeEnum(boolean multipleMatches, boolean optional, Boolean greedy, String text) {
        this.multipleMatches = multipleMatches;
        this.optional = optional;
        this.greedy = greedy;
        this.text = text;
    }

    /**
     * Returns indicator if single or multiple matches.
     *
     * @return indicator
     */
    public boolean isMultipleMatches() {
        return multipleMatches;
    }

    /**
     * Returns indicator if optional matches.
     *
     * @return indicator
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Returns indicator if greedy or reluctant.
     *
     * @return indicator
     */
    public Boolean isGreedy() {
        return greedy;
    }

    /**
     * Inspect code and return enum for code.
     *
     * @param code              to inspect
     * @param reluctantQuestion null for greedy or questionmark for reluctant
     * @return enum
     */
    public static RegexNFATypeEnum fromString(String code, String reluctantQuestion) {
        boolean reluctant = false;
        if (reluctantQuestion != null) {
            if (!reluctantQuestion.equals("?")) {
                throw new IllegalArgumentException("Invalid code for pattern type: " + code + " reluctant '" + reluctantQuestion + "'");
            }
            reluctant = true;
        }

        if (code == null) {
            return SINGLE;
        }
        if (code.equals("*")) {
            return reluctant ? ZERO_TO_MANY_RELUCTANT : ZERO_TO_MANY;
        }
        if (code.equals("+")) {
            return reluctant ? ONE_TO_MANY_RELUCTANT : ONE_TO_MANY;
        }
        if (code.equals("?")) {
            return reluctant ? ONE_OPTIONAL_RELUCTANT : ONE_OPTIONAL;
        }
        throw new IllegalArgumentException("Invalid code for pattern type: " + code);
    }

    /**
     * Return postfix.
     *
     * @return postfix
     */
    public String getOptionalPostfix() {
        return text;
    }
}
