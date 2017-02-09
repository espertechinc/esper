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
import java.util.ArrayList;
import java.util.List;

/**
 * A clause to insert into zero, one or more streams based on criteria.
 */
public class OnInsertSplitStreamClause extends OnClause {
    private static final long serialVersionUID = 0L;

    private boolean first;
    private List<OnInsertSplitStreamItem> items = new ArrayList<OnInsertSplitStreamItem>();

    /**
     * Ctor.
     */
    public OnInsertSplitStreamClause() {
    }

    /**
     * Creates a split-stream on-insert clause from an indicator whether to consider the first of all where-clauses,
     * and a list of items.
     *
     * @param isFirst true for first where-clause, false for all where-clauses fire
     * @param items   is a list of insert-into, select and optional where-clauses
     * @return split-stream on-insert clause
     */
    public static OnInsertSplitStreamClause create(boolean isFirst, List<OnInsertSplitStreamItem> items) {
        return new OnInsertSplitStreamClause(isFirst, items);
    }

    /**
     * Creates an split-stream on-insert clause considering only the first where-clause that matches.
     *
     * @return split-stream on-insert clause
     */
    public static OnInsertSplitStreamClause create() {
        return new OnInsertSplitStreamClause(true, new ArrayList<OnInsertSplitStreamItem>());
    }

    /**
     * Ctor.
     *
     * @param isFirst indicator whether only the first where-clause is to match or all where-clauses.
     * @param items   tuples of insert-into, select and where-clauses.
     */
    public OnInsertSplitStreamClause(boolean isFirst, List<OnInsertSplitStreamItem> items) {
        this.first = isFirst;
        this.items = items;
    }

    /**
     * Renders the clause in textual representation.
     *
     * @param writer    to output to
     * @param formatter for newline-whitespace formatting
     */
    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        for (OnInsertSplitStreamItem item : items) {
            item.getInsertInto().toEPL(writer, formatter, true);
            item.getSelectClause().toEPL(writer, formatter, true, false);
            if (item.getPropertySelects() != null) {
                writer.append(" from ");
                ContainedEventSelect.toEPL(writer, formatter, item.getPropertySelects());
                if (item.getPropertySelectsStreamName() != null) {
                    writer.append(" as ");
                    writer.append(item.getPropertySelectsStreamName());
                }
            }
            if (item.getWhereClause() != null) {
                writer.append(" where ");
                item.getWhereClause().toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            }
        }

        if (!first) {
            writer.append(" output all");
        }
    }

    /**
     * Returns true for firing the insert-into for only the first where-clause that matches,
     * or false for firing the insert-into for all where-clauses that match.
     *
     * @return indicator first or all
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * Returns true for firing the insert-into for only the first where-clause that matches,
     * or false for firing the insert-into for all where-clauses that match.
     *
     * @return indicator first or all
     */
    public boolean getFirst() {
        return first;
    }

    /**
     * Set to true for firing the insert-into for only the first where-clause that matches,
     * or false for firing the insert-into for all where-clauses that match.
     *
     * @param first indicator first or all
     */
    public void setFirst(boolean first) {
        this.first = first;
    }

    /**
     * Returns a list of insert-into, select and where-clauses.
     *
     * @return split-stream lines
     */
    public List<OnInsertSplitStreamItem> getItems() {
        return items;
    }

    /**
     * Sets a list of insert-into, select and where-clauses.
     *
     * @param items split-stream lines
     */
    public void setItems(List<OnInsertSplitStreamItem> items) {
        this.items = items;
    }

    /**
     * Add a insert-into, select and where-clause.
     *
     * @param item to add
     */
    public void addItem(OnInsertSplitStreamItem item) {
        items.add(item);
    }
}
