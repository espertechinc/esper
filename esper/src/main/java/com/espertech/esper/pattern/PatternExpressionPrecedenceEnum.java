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
package com.espertech.esper.pattern;

public enum PatternExpressionPrecedenceEnum {

    /**
     * Precedence.
     */
    MINIMUM(Integer.MIN_VALUE),

    /**
     * Precedence.
     */
    FOLLOWEDBY(1),

    /**
     * Precedence.
     */
    OR(2),

    /**
     * Precedence.
     */
    AND(3),

    /**
     * Precedence.
     */
    REPEAT_UNTIL(4),

    /**
     * Precedence.
     */
    UNARY(5),

    /**
     * Precedence.
     */
    GUARD_POSTFIX(6),

    /**
     * Precedence.
     */
    ATOM(Integer.MAX_VALUE);

    private final int level;

    private PatternExpressionPrecedenceEnum(int level) {
        this.level = level;
    }

    /**
     * Level.
     *
     * @return level
     */
    public int getLevel() {
        return level;
    }
}
