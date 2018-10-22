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
package com.espertech.esper.common.client.soda;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;

/**
 * Table column in a create-table statement.
 */
public class CreateTableColumn implements Serializable {
    private static final long serialVersionUID = 0L;

    private String columnName;
    private Expression optionalExpression;
    private String optionalTypeName;
    private List<AnnotationPart> annotations;
    private Boolean primaryKey;

    /**
     * Ctor.
     *
     * @param columnName         the table column name
     * @param optionalExpression an optional aggregation expression (exclusive of type name)
     * @param optionalTypeName   a type name (exclusive of aggregation expression)
     * @param annotations        optional annotations
     * @param primaryKey         flag indicating whether the column is a primary key
     */
    public CreateTableColumn(String columnName, Expression optionalExpression, String optionalTypeName, List<AnnotationPart> annotations, Boolean primaryKey) {
        this.columnName = columnName;
        this.optionalExpression = optionalExpression;
        this.optionalTypeName = optionalTypeName;
        this.annotations = annotations;
        this.primaryKey = primaryKey;
    }

    /**
     * Ctor.
     */
    public CreateTableColumn() {
    }

    /**
     * Returns the table column name
     *
     * @return column name
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Sets the table column name
     *
     * @param columnName column name
     */
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    /**
     * Returns optional annotations, or null if there are none
     *
     * @return annotations
     */
    public List<AnnotationPart> getAnnotations() {
        return annotations;
    }

    /**
     * Sets optional annotations, or null if there are none
     *
     * @param annotations annotations
     */
    public void setAnnotations(List<AnnotationPart> annotations) {
        this.annotations = annotations;
    }

    /**
     * Returns the aggragtion expression, if the type of the column is aggregation,
     * or null if a type name is provided instead.
     *
     * @return expression
     */
    public Expression getOptionalExpression() {
        return optionalExpression;
    }

    /**
     * Sets the aggragtion expression, if the type of the column is aggregation,
     * or null if a type name is provided instead.
     *
     * @param optionalExpression expression
     */
    public void setOptionalExpression(Expression optionalExpression) {
        this.optionalExpression = optionalExpression;
    }

    /**
     * Returns the type name, or null if the column is an aggregation and an
     * aggregation expression is provided instead.
     *
     * @return type name
     */
    public String getOptionalTypeName() {
        return optionalTypeName;
    }

    /**
     * Sets the type name, or null if the column is an aggregation and an
     * aggregation expression is provided instead.
     *
     * @param optionalTypeName type name
     */
    public void setOptionalTypeName(String optionalTypeName) {
        this.optionalTypeName = optionalTypeName;
    }

    /**
     * Returns indicator whether the column is a primary key
     *
     * @return primary key indicator
     */
    public Boolean getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Sets indicator whether the column is a primary key
     *
     * @param primaryKey primary key indicator
     */
    public void setPrimaryKey(Boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * Render create-table column
     *
     * @param writer to render to
     */
    public void toEPL(StringWriter writer) {
        writer.append(columnName);
        writer.append(" ");
        if (optionalExpression != null) {
            optionalExpression.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        } else {
            writer.append(optionalTypeName);
            if (primaryKey) {
                writer.append(" primary key");
            }
        }
        if (annotations != null && !annotations.isEmpty()) {
            writer.append(" ");
            String delimiter = "";
            for (AnnotationPart part : annotations) {
                if (part.getName() == null) {
                    continue;
                }
                writer.append(delimiter);
                delimiter = " ";
                part.toEPL(writer);
            }
        }
    }
}
