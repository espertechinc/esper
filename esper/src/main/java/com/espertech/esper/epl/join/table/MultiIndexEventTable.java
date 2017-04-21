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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.io.StringWriter;
import java.util.Iterator;

/**
 * An event table for holding multiple tables for use when multiple indexes of the same dataset must be entered into a cache
 * for use in historical data lookup.
 * <p>
 * Does not allow iteration, adding and removing events. Does allow clearing all tables and asking for
 * filled or empty tables. All tables are expected to be filled and empty at the same time,
 * reflecting multiple indexes on a single set of data.
 */
public class MultiIndexEventTable implements EventTable {
    private final EventTable[] tables;
    private final EventTableOrganization organization;

    /**
     * Ctor.
     *
     * @param tables       tables to hold
     * @param organization organization
     */
    public MultiIndexEventTable(EventTable[] tables, EventTableOrganization organization) {
        this.tables = tables;
        this.organization = organization;
    }

    /**
     * Returns all tables.
     *
     * @return tables
     */
    public EventTable[] getTables() {
        return tables;
    }

    public void addRemove(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public void add(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public void add(EventBean event, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public void remove(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public void remove(EventBean event, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public Iterator<EventBean> iterator() {
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        return tables[0].isEmpty();
    }

    public void clear() {
        for (int i = 0; i < tables.length; i++) {
            tables[i].clear();
        }
    }

    public void destroy() {
        clear();
    }

    public String toQueryPlan() {
        StringWriter buf = new StringWriter();
        String delimiter = "";
        for (EventTable table : tables) {
            buf.append(delimiter);
            buf.append(table.toQueryPlan());
            delimiter = ", ";
        }
        return this.getClass().getSimpleName() + " " + buf.toString();
    }

    public Integer getNumberOfEvents() {
        for (EventTable table : tables) {
            Integer num = table.getNumberOfEvents();
            if (num != null) {
                return num;
            }
        }
        return null;
    }

    public int getNumKeys() {
        return tables[0].getNumKeys();
    }

    public Object getIndex() {
        Object[] indexes = new Object[tables.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = tables[i].getIndex();
        }
        return indexes;
    }

    public EventTableOrganization getOrganization() {
        return organization;
    }

    public Class getProviderClass() {
        return MultiIndexEventTable.class;
    }
}
