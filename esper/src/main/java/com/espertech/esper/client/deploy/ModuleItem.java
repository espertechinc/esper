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
package com.espertech.esper.client.deploy;

import java.io.Serializable;

/**
 * Represents an EPL statement as part of a {@link Module}.
 * <p>
 * Character position start and end are only available for non-comment only.
 */
public class ModuleItem implements Serializable {
    private static final long serialVersionUID = 1281976895727342222L;

    private String expression;
    private boolean commentOnly;
    private int lineNumber;
    private int charPosStart;
    private int charPosEnd;

    /**
     * Ctor.
     *
     * @param expression   EPL
     * @param commentOnly  true if the statement consists only of comments or whitespace
     * @param lineNumber   line number
     * @param charPosStart character position of start of segment
     * @param charPosEnd   character position of end of segment
     */
    public ModuleItem(String expression, boolean commentOnly, int lineNumber, int charPosStart, int charPosEnd) {
        this.expression = expression;
        this.commentOnly = commentOnly;
        this.lineNumber = lineNumber;
        this.charPosStart = charPosStart;
        this.charPosEnd = charPosEnd;
    }

    /**
     * Returns the EPL.
     *
     * @return expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Sets the EPL.
     *
     * @param expression to set
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * Returns true to indicate comments-only expression.
     *
     * @return comments-only indicator
     */
    public boolean isCommentOnly() {
        return commentOnly;
    }

    /**
     * Set true to indicate comments-only expression.
     *
     * @param commentOnly comments-only indicator
     */
    public void setCommentOnly(boolean commentOnly) {
        this.commentOnly = commentOnly;
    }

    /**
     * Returns the line number of item.
     *
     * @return item line num
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Sets item line num
     *
     * @param lineNumber to set
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Returns item char position in line.
     *
     * @return char position
     */
    public int getCharPosStart() {
        return charPosStart;
    }

    /**
     * Sets item char position in line.
     *
     * @param charPosStart char position
     */
    public void setCharPosStart(int charPosStart) {
        this.charPosStart = charPosStart;
    }

    /**
     * Returns end position of character on line for the item.
     *
     * @return position
     */
    public int getCharPosEnd() {
        return charPosEnd;
    }

    /**
     * Sets the end position of character on line for the item.
     *
     * @param charPosEnd position
     */
    public void setCharPosEnd(int charPosEnd) {
        this.charPosEnd = charPosEnd;
    }
}
