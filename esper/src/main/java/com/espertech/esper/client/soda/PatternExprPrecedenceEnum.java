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
 * Pattern precendences.
 */
public enum PatternExprPrecedenceEnum {

    /**
     * Precedence.
     */
    MAXIMIM(Integer.MAX_VALUE),

    /**
     * Precedence.
     */
    ATOM(7),
    /**
     * Precedence.
     */
    GUARD(6),
    /**
     * Precedence.
     */
    EVERY_NOT(5),
    /**
     * Precedence.
     */
    MATCH_UNTIL(4),
    /**
     * Precedence.
     */
    AND(3),
    /**
     * Precedence.
     */
    OR(2),
    /**
     * Precedence.
     */
    FOLLOWED_BY(1),

    /**
     * Precedence.
     */
    MINIMUM(Integer.MIN_VALUE);

    private final int level;

    private PatternExprPrecedenceEnum(int level) {
        this.level = level;
    }

    /**
     * Returns precedence.
     *
     * @return precedence
     */
    public int getLevel() {
        return level;
    }
}
