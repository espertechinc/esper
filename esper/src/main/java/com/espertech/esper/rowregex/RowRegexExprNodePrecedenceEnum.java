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
 * Precendence levels for expressions.
 */
public enum RowRegexExprNodePrecedenceEnum {

    /**
     * Precedence.
     */
    UNARY(4),
    /**
     * Precedence.
     */
    GROUPING(3),
    /**
     * Precedence.
     */
    CONCATENATION(2),
    /**
     * Precedence.
     */
    ALTERNATION(1),

    /**
     * Precedence.
     */
    MINIMUM(Integer.MIN_VALUE);

    private final int level;

    private RowRegexExprNodePrecedenceEnum(int level) {
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
