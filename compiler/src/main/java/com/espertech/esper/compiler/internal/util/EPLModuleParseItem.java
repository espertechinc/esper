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
package com.espertech.esper.compiler.internal.util;

/**
 * Item parsing an EPL module file.
 */
public class EPLModuleParseItem {
    private final String expression;
    private final int lineNum;
    private final int startChar;
    private final int endChar;
    private final int lineNumEnd;
    private final int lineNumContent;
    private final int lineNumContentEnd;

    /**
     * Ctor.
     *
     * @param expression EPL
     * @param lineNum    line number starting the EPL including comments
     * @param startChar  start character number total file
     * @param endChar    end character number
     * @param lineNumEnd line number ending the EPL including comments
     * @param lineNumContent line number starting the EPL excluding comments
     * @param lineNumContentEnd line number ending the EPL excluding comments
     */
    public EPLModuleParseItem(String expression, int lineNum, int startChar, int endChar, int lineNumEnd, int lineNumContent, int lineNumContentEnd) {
        this.expression = expression;
        this.lineNum = lineNum;
        this.startChar = startChar;
        this.endChar = endChar;
        this.lineNumEnd = lineNumEnd;
        this.lineNumContent = lineNumContent;
        this.lineNumContentEnd = lineNumContentEnd;
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

    /**
     * Returns line number where the expression ends.
     *
     * @return line number end
     */
    public int getLineNumEnd() {
        return lineNumEnd;
    }

    public int getLineNumContent() {
        return lineNumContent;
    }

    public int getLineNumContentEnd() {
        return lineNumContentEnd;
    }
}
