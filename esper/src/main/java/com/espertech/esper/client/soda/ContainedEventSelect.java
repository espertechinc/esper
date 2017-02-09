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
import java.util.List;

/**
 * Represents a contained-event selection.
 */
public class ContainedEventSelect implements Serializable {
    private static final long serialVersionUID = 0L;

    private SelectClause selectClause;
    private Expression splitExpression;
    private String optionalSplitExpressionTypeName;
    private String optionalAsName;
    private Expression whereClause;

    /**
     * Ctor.
     */
    public ContainedEventSelect() {
    }

    /**
     * Ctor.
     *
     * @param splitExpression the property expression or other expression for splitting the event
     */
    public ContainedEventSelect(Expression splitExpression) {
        this.splitExpression = splitExpression;
    }

    /**
     * Returns the property alias.
     *
     * @return alias
     */
    public String getOptionalAsName() {
        return optionalAsName;
    }

    /**
     * Sets the property alias
     *
     * @param optionalAsName alias
     */
    public void setOptionalAsName(String optionalAsName) {
        this.optionalAsName = optionalAsName;
    }

    /**
     * Returns the select clause.
     *
     * @return select clause
     */
    public SelectClause getSelectClause() {
        return selectClause;
    }

    /**
     * Sets the select clause.
     *
     * @param selectClause select clause
     */
    public void setSelectClause(SelectClause selectClause) {
        this.selectClause = selectClause;
    }

    /**
     * Returns the where clause.
     *
     * @return where clause
     */
    public Expression getWhereClause() {
        return whereClause;
    }

    /**
     * Sets the where clause.
     *
     * @param whereClause where clause
     */
    public void setWhereClause(Expression whereClause) {
        this.whereClause = whereClause;
    }

    /**
     * Returns the event type name assigned to events that result by applying the split (contained event) expression.
     *
     * @return type name, or null if none assigned
     */
    public String getOptionalSplitExpressionTypeName() {
        return optionalSplitExpressionTypeName;
    }

    /**
     * Sets the event type name assigned to events that result by applying the split (contained event) expression.
     *
     * @param optionalSplitExpressionTypeName type name, or null if none assigned
     */
    public void setOptionalSplitExpressionTypeName(String optionalSplitExpressionTypeName) {
        this.optionalSplitExpressionTypeName = optionalSplitExpressionTypeName;
    }

    /**
     * Returns the expression that returns the contained events.
     *
     * @return contained event expression
     */
    public Expression getSplitExpression() {
        return splitExpression;
    }

    /**
     * Sets the expression that returns the contained events.
     *
     * @param splitExpression contained event expression
     */
    public void setSplitExpression(Expression splitExpression) {
        this.splitExpression = splitExpression;
    }

    /**
     * Returns the EPL.
     *
     * @param writer    to write to
     * @param formatter for newline-whitespace formatting
     */
    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        if (selectClause != null) {
            selectClause.toEPL(writer, formatter, false, false);
            writer.write(" from ");
        }
        splitExpression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        if (optionalSplitExpressionTypeName != null) {
            writer.write("@type(");
            writer.write(optionalSplitExpressionTypeName);
            writer.write(")");
        }
        if (optionalAsName != null) {
            writer.write(" as ");
            writer.write(optionalAsName);
        }
        if (whereClause != null) {
            writer.write(" where ");
            whereClause.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
    }

    /**
     * Render contained-event select
     *
     * @param writer    to render to
     * @param formatter to use
     * @param items     to render
     */
    public static void toEPL(StringWriter writer, EPStatementFormatter formatter, List<ContainedEventSelect> items) {
        for (ContainedEventSelect propertySelect : items) {
            writer.write('[');
            propertySelect.toEPL(writer, formatter);
            writer.write(']');
        }
    }
}
