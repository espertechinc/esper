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

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * An item in a for-clause for controlling delivery of result events to listeners and subscribers.
 */
public class ForClauseItem implements Serializable {
    private static final long serialVersionUID = -8476488858601535928L;

    private ForClauseKeyword keyword;
    private List<Expression> expressions;

    /**
     * Creates a for-clause with no expressions.
     *
     * @param keyword keyword to use
     * @return for-clause
     */
    public static ForClauseItem create(ForClauseKeyword keyword) {
        return new ForClauseItem(keyword);
    }

    /**
     * Ctor.
     * <p>
     * Must set a keyword and optionally add expressions.
     */
    public ForClauseItem() {
        expressions = new ArrayList<Expression>();
    }

    /**
     * Ctor.
     *
     * @param keyword the delivery keyword
     */
    public ForClauseItem(ForClauseKeyword keyword) {
        this();
        setKeyword(keyword);
    }

    /**
     * Returns the for-clause keyword.
     *
     * @return keyword
     */
    public ForClauseKeyword getKeyword() {
        return keyword;
    }

    /**
     * Sets the for-clause keyword.
     *
     * @param keyword to set
     */
    public void setKeyword(ForClauseKeyword keyword) {
        this.keyword = keyword;
    }

    /**
     * Returns for-clause expressions.
     *
     * @return expressions
     */
    public List<Expression> getExpressions() {
        return expressions;
    }

    /**
     * Sets for-clause expressions.
     *
     * @param expressions expressions to set
     */
    public void setExpressions(List<Expression> expressions) {
        this.expressions = expressions;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        if (keyword == null) {
            return;
        }
        writer.write("for ");
        writer.write(keyword.getName());
        if (expressions.size() == 0) {
            return;
        }

        writer.write("(");
        String delimiter = "";
        for (Expression child : expressions) {
            writer.write(delimiter);
            child.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            delimiter = ", ";
        }
        writer.write(")");
    }
}