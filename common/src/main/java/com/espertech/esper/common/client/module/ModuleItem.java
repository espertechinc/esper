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
package com.espertech.esper.common.client.module;

import com.espertech.esper.common.client.soda.EPStatementObjectModel;

import java.io.Serializable;

/**
 * Represents an EPL statement as part of a {@link Module}.
 * <p>
 * Character position start and end are only available for non-comment only.
 */
public class ModuleItem implements Serializable {
    private static final long serialVersionUID = 1281976895727342222L;

    private String expression;
    private EPStatementObjectModel model;
    private boolean commentOnly;
    private int lineNumber;
    private int charPosStart;
    private int charPosEnd;
    private int lineNumberEnd;
    private int lineNumberContent;
    private int lineNumberContentEnd;

    /**
     * Ctor.
     *
     * @param expression   EPL
     * @param commentOnly  true if the statement consists only of comments or whitespace
     * @param lineNumber   line number
     * @param charPosStart character position of start of segment
     * @param charPosEnd   character position of end of segment
     * @param lineNumberEnd line number of the line that ends the statement
     * @param lineNumberContent line number of the line that starts the statement excluding comments, or -1 if comments-only
     * @param lineNumberContentEnd line number of the line that ends the statement excluding comments, or -1 if comments-only
     */
    public ModuleItem(String expression, boolean commentOnly, int lineNumber, int charPosStart, int charPosEnd, int lineNumberEnd, int lineNumberContent, int lineNumberContentEnd) {
        this.expression = expression;
        this.commentOnly = commentOnly;
        this.lineNumber = lineNumber;
        this.charPosStart = charPosStart;
        this.charPosEnd = charPosEnd;
        this.lineNumberEnd = lineNumberEnd;
        this.lineNumberContent = lineNumberContent;
        this.lineNumberContentEnd = lineNumberContentEnd;
    }

    /**
     * Ctor.
     *
     * @param expression expression
     */
    public ModuleItem(String expression) {
        this.expression = expression;
    }

    /**
     * Ctor.
     *
     * @param model statement object model
     */
    public ModuleItem(EPStatementObjectModel model) {
        this.model = model;
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
     * Returns the line number of item that ends the item.
     *
     * @return item line num end
     */
    public int getLineNumberEnd() {
        return lineNumberEnd;
    }

    /**
     * Sets item line num end
     *
     * @param lineNumberEnd to set
     */
    public void setLineNumberEnd(int lineNumberEnd) {
        this.lineNumberEnd = lineNumberEnd;
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

    /**
     * Returns the statement object model when provided
     *
     * @return model
     */
    public EPStatementObjectModel getModel() {
        return model;
    }

    /**
     * Returns the line number of item content excluding comments, or -1 if comments-only
     *
     * @return item line num content start
     */
    public int getLineNumberContent() {
        return lineNumberContent;
    }

    /**
     * Sets the line number of item content excluding comments, or -1 if comments-only
     *
     * @param  lineNumberContent item line num content start
     */
    public void setLineNumberContent(int lineNumberContent) {
        this.lineNumberContent = lineNumberContent;
    }

    /**
     * Returns the line number of item content end excluding comments, or -1 if comments-only
     *
     * @return item line num content end
     */
    public int getLineNumberContentEnd() {
        return lineNumberContentEnd;
    }

    /**
     * Sets the line number of item content end excluding comments, or -1 if comments-only
     *
     * @param lineNumberContentEnd item line num content end
     */
    public void setLineNumberContentEnd(int lineNumberContentEnd) {
        this.lineNumberContentEnd = lineNumberContentEnd;
    }
}
