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
import java.util.List;

/**
 * Items within the split-stream syntax to contain a tuple of insert-into, select and where-clause.
 */
public class OnInsertSplitStreamItem implements Serializable {
    private static final long serialVersionUID = 0L;

    private InsertIntoClause insertInto;
    private SelectClause selectClause;
    private List<ContainedEventSelect> propertySelects;
    private String propertySelectsStreamName;
    private Expression whereClause;

    /**
     * Ctor.
     */
    public OnInsertSplitStreamItem() {
    }

    /**
     * Factory method for split-stream items.
     *
     * @param insertInto   the insert-into clause
     * @param selectClause the select-clause
     * @param whereClause  where-expression or null
     * @return split-stream item
     */
    public static OnInsertSplitStreamItem create(InsertIntoClause insertInto, SelectClause selectClause, Expression whereClause) {
        return new OnInsertSplitStreamItem(insertInto, selectClause, whereClause);
    }

    /**
     * Factory method for split-stream items.
     *
     * @param insertInto                the insert-into clause
     * @param selectClause              the select-clause
     * @param propertySelects           contained-event selects in the from-clause
     * @param propertySelectsStreamName stream name for contained-event selection
     * @param whereClause               where-expression or null
     * @return split-stream item
     */
    public static OnInsertSplitStreamItem create(InsertIntoClause insertInto, SelectClause selectClause, List<ContainedEventSelect> propertySelects, String propertySelectsStreamName, Expression whereClause) {
        return new OnInsertSplitStreamItem(insertInto, selectClause, propertySelects, propertySelectsStreamName, whereClause);
    }

    /**
     * Ctor.
     *
     * @param insertInto                the insert-into clause
     * @param selectClause              the select-clause
     * @param propertySelects           contained-event selections
     * @param propertySelectsStreamName contained-event selection stream name
     * @param whereClause               where-expression or null
     */
    public OnInsertSplitStreamItem(InsertIntoClause insertInto, SelectClause selectClause, List<ContainedEventSelect> propertySelects, String propertySelectsStreamName, Expression whereClause) {
        this.insertInto = insertInto;
        this.selectClause = selectClause;
        this.propertySelects = propertySelects;
        this.propertySelectsStreamName = propertySelectsStreamName;
        this.whereClause = whereClause;
    }

    /**
     * Ctor.
     *
     * @param insertInto   the insert-into clause
     * @param selectClause the select-clause
     * @param whereClause  where-expression or null
     */
    public OnInsertSplitStreamItem(InsertIntoClause insertInto, SelectClause selectClause, Expression whereClause) {
        this(insertInto, selectClause, null, null, whereClause);
    }

    /**
     * Returns the insert-into clause.
     *
     * @return insert-into clause
     */
    public InsertIntoClause getInsertInto() {
        return insertInto;
    }

    /**
     * Sets the insert-into clause.
     *
     * @param insertInto insert-into clause
     */
    public void setInsertInto(InsertIntoClause insertInto) {
        this.insertInto = insertInto;
    }

    /**
     * Returns the select-clause.
     *
     * @return select-clause
     */
    public SelectClause getSelectClause() {
        return selectClause;
    }

    /**
     * Sets the select-clause.
     *
     * @param selectClause select-clause
     */
    public void setSelectClause(SelectClause selectClause) {
        this.selectClause = selectClause;
    }

    /**
     * Returns the optional where-clause.
     *
     * @return where-clause
     */
    public Expression getWhereClause() {
        return whereClause;
    }

    /**
     * Sets the optional where-clause
     *
     * @param whereClause to set
     */
    public void setWhereClause(Expression whereClause) {
        this.whereClause = whereClause;
    }

    /**
     * Returns contained-event selection, if any.
     *
     * @return list or null
     */
    public List<ContainedEventSelect> getPropertySelects() {
        return propertySelects;
    }

    /**
     * Sets contained-event selection, if any.
     *
     * @param propertySelects list
     */
    public void setPropertySelects(List<ContainedEventSelect> propertySelects) {
        this.propertySelects = propertySelects;
    }

    /**
     * Returns the stream name assigned to contained-event selects, or null
     *
     * @return stream name
     */
    public String getPropertySelectsStreamName() {
        return propertySelectsStreamName;
    }

    /**
     * Sets the stream name assigned to contained-event selects, or null
     *
     * @param propertySelectsStreamName stream name
     */
    public void setPropertySelectsStreamName(String propertySelectsStreamName) {
        this.propertySelectsStreamName = propertySelectsStreamName;
    }
}
