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
package com.espertech.esper.type;

/**
 * Enum for the type of outer join.
 */
public enum OuterJoinType {
    /**
     * Left outer join.
     */
    LEFT("left"),

    /**
     * Right outer join.
     */
    RIGHT("right"),

    /**
     * Full outer join.
     */
    FULL("full"),

    /**
     * Inner join.
     */
    INNER("inner");

    private String text;

    OuterJoinType(String text) {
        this.text = text;
    }

    /**
     * Returns the operator as an expression text.
     *
     * @return text of operator
     */
    public String getText() {
        return text;
    }
}
