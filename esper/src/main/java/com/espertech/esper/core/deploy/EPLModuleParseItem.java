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
package com.espertech.esper.core.deploy;

/**
 * Item parsing an EPL module file.
 */
public class EPLModuleParseItem {
    private String expression;
    private int lineNum;
    private int startChar;
    private int endChar;

    /**
     * Ctor.
     *
     * @param expression EPL
     * @param lineNum    line number
     * @param startChar  start character number total file
     * @param endChar    end character number
     */
    public EPLModuleParseItem(String expression, int lineNum, int startChar, int endChar) {
        this.expression = expression;
        this.lineNum = lineNum;
        this.startChar = startChar;
        this.endChar = endChar;
    }

    /**
     * Returns line number of expression.
     *
     * @return line number
     */
    public int getLineNum() {
        return lineNum;
    }

    /**
     * Returns the expression.
     *
     * @return expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Returns the position of the start character.
     *
     * @return start char position
     */
    public int getStartChar() {
        return startChar;
    }

    /**
     * Returns the position of the end character.
     *
     * @return end char position
     */
    public int getEndChar() {
        return endChar;
    }
}
