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
import java.util.Collections;
import java.util.List;

/**
 * For use with on-merge clauses, inserts into a named window if matching rows are not found.
 */
public class OnMergeMatchedInsertAction implements OnMergeMatchedAction {
    private static final long serialVersionUID = 0L;

    private List<String> columnNames = Collections.emptyList();
    private List<SelectClauseElement> selectList = Collections.emptyList();
    private Expression whereClause;
    private String optionalStreamName;

    /**
     * Ctor.
     *
     * @param columnNames        insert-into column names, or empty list if none provided
     * @param selectList         select expression list
     * @param whereClause        optional condition or null
     * @param optionalStreamName optionally a stream name for insert-into
     */
    public OnMergeMatchedInsertAction(List<String> columnNames, List<SelectClauseElement> selectList, Expression whereClause, String optionalStreamName) {
        this.columnNames = columnNames;
        this.selectList = selectList;
        this.whereClause = whereClause;
        this.optionalStreamName = optionalStreamName;
    }

    /**
     * Ctor.
     */
    public OnMergeMatchedInsertAction() {
    }

    /**
     * Returns the action condition, or null if undefined.
     *
     * @return condition
     */
    public Expression getWhereClause() {
        return whereClause;
    }

    /**
     * Sets the action condition, or null if undefined.
     *
     * @param whereClause to set, or null to remove the condition
     */
    public void setWhereClause(Expression whereClause) {
        this.whereClause = whereClause;
    }

    /**
     * Returns the insert-into column names, if provided.
     *
     * @return column names
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * Sets the insert-into column names, can be empty list.
     *
     * @param columnNames column names to set
     */
    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    /**
     * Returns the select expressions.
     *
     * @return expression list
     */
    public List<SelectClauseElement> getSelectList() {
        return selectList;
    }

    /**
     * Sets the select expressions.
     *
     * @param selectList expression list
     */
    public void setSelectList(List<SelectClauseElement> selectList) {
        this.selectList = selectList;
    }

    /**
     * Returns the insert-into stream name.
     *
     * @return stream name
     */
    public String getOptionalStreamName() {
        return optionalStreamName;
    }

    /**
     * Sets the insert-into stream name.
     *
     * @param optionalStreamName stream name to insert into
     */
    public void setOptionalStreamName(String optionalStreamName) {
        this.optionalStreamName = optionalStreamName;
    }

    @Override
    public void toEPL(StringWriter writer) {
        writer.write("insert");
        if (optionalStreamName != null) {
            writer.write(" into ");
            writer.write(optionalStreamName);
        }

        if (columnNames.size() > 0) {
            writer.write("(");
            String delimiter = "";
            for (String name : columnNames) {
                writer.write(delimiter);
                writer.write(name);
                delimiter = ", ";
            }
            writer.write(")");
        }
        writer.write(" select ");
        String delimiter = "";
        for (SelectClauseElement element : selectList) {
            writer.write(delimiter);
            element.toEPLElement(writer);
            delimiter = ", ";
        }
        if (whereClause != null) {
            writer.write(" where ");
            whereClause.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
    }
}