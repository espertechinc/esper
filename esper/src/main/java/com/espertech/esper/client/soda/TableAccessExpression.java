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

import java.io.StringWriter;
import java.util.List;

/**
 * Table access expression.
 */
public class TableAccessExpression extends ExpressionBase {
    private static final long serialVersionUID = 8878898170678366785L;

    private String tableName;
    private List<Expression> keyExpressions;
    private String optionalColumn;
    private Expression optionalAggregate;

    /**
     * Ctor.
     */
    public TableAccessExpression() {
    }

    /**
     * Ctor.
     *
     * @param tableName         the table name
     * @param keyExpressions    the list of key expressions for each table primary key in the same order as declared
     * @param optionalColumn    optional column name
     * @param optionalAggregate optional aggregation function
     */
    public TableAccessExpression(String tableName, List<Expression> keyExpressions, String optionalColumn, Expression optionalAggregate) {
        this.tableName = tableName;
        this.keyExpressions = keyExpressions;
        this.optionalColumn = optionalColumn;
        this.optionalAggregate = optionalAggregate;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write(tableName);
        if (keyExpressions != null && !keyExpressions.isEmpty()) {
            writer.write("[");
            ExpressionBase.toPrecedenceFreeEPL(keyExpressions, writer);
            writer.write("]");
        }
        if (optionalColumn != null) {
            writer.write(".");
            writer.write(optionalColumn);
        }
        if (optionalAggregate != null) {
            writer.write(".");
            optionalAggregate.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
    }

    /**
     * Returns the table name.
     *
     * @return table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the table name.
     *
     * @param tableName table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns the primary key expressions.
     *
     * @return primary key expressions
     */
    public List<Expression> getKeyExpressions() {
        return keyExpressions;
    }

    /**
     * Sets the primary key expressions.
     *
     * @param keyExpressions primary key expressions
     */
    public void setKeyExpressions(List<Expression> keyExpressions) {
        this.keyExpressions = keyExpressions;
    }

    /**
     * Returns the optional table column name to access.
     *
     * @return table column name or null if accessing row
     */
    public String getOptionalColumn() {
        return optionalColumn;
    }

    /**
     * Sets the optional table column name to access.
     *
     * @param optionalColumn table column name or null if accessing row
     */
    public void setOptionalColumn(String optionalColumn) {
        this.optionalColumn = optionalColumn;
    }

    /**
     * Returns the optional table column aggregation accessor to use.
     *
     * @return table column aggregation accessor
     */
    public Expression getOptionalAggregate() {
        return optionalAggregate;
    }

    /**
     * Sets the optional table column aggregation accessor to use.
     *
     * @param optionalAggregate table column aggregation accessor
     */
    public void setOptionalAggregate(Expression optionalAggregate) {
        this.optionalAggregate = optionalAggregate;
    }
}
