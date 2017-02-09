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
 * Filter defines the event type to be filtered for, and an optional expression that returns true if
 * the filter should consider the event, or false to reject the event.
 */
public class Filter implements Serializable {
    private static final long serialVersionUID = 0L;

    private String eventTypeName;
    private Expression filter;
    private List<ContainedEventSelect> optionalPropertySelects;

    /**
     * Ctor.
     */
    public Filter() {
    }

    /**
     * Creates a filter to the given named event type.
     *
     * @param eventTypeName is the event type name to filter for
     * @return filter
     */
    public static Filter create(String eventTypeName) {
        return new Filter(eventTypeName);
    }

    /**
     * Creates a filter to the given named event type and filter expression.
     *
     * @param eventTypeName is the event type name to filter for
     * @param filter        is the expression filtering out events
     * @return filter is the filter expression
     */
    public static Filter create(String eventTypeName, Expression filter) {
        return new Filter(eventTypeName, filter);
    }

    /**
     * Ctor.
     *
     * @param eventTypeName is the event type name
     */
    public Filter(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    /**
     * Ctor.
     *
     * @param eventTypeName is the event type name
     * @param filter        is the filter expression
     */
    public Filter(String eventTypeName, Expression filter) {
        this.eventTypeName = eventTypeName;
        this.filter = filter;
    }

    /**
     * Returns the name of the event type to filter for.
     *
     * @return event type name
     */
    public String getEventTypeName() {
        return eventTypeName;
    }

    /**
     * Sets the name of the event type to filter for.
     *
     * @param eventTypeName name of the event type to filter for
     */
    public void setEventTypeName(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    /**
     * Returns the optional filter expression that tests the event, or null if no filter expression was defined.
     *
     * @return filter expression
     */
    public Expression getFilter() {
        return filter;
    }

    /**
     * Sets the optional filter expression that tests the event, or null if no filter expression is needed.
     *
     * @param filter is the filter expression to set
     */
    public void setFilter(Expression filter) {
        this.filter = filter;
    }

    /**
     * Returns contained-event spec.
     *
     * @return spec
     */
    public List<ContainedEventSelect> getOptionalPropertySelects() {
        return optionalPropertySelects;
    }

    /**
     * Sets the contained-event selection, if any.
     *
     * @param optionalPropertySelects spec
     */
    public void setOptionalPropertySelects(List<ContainedEventSelect> optionalPropertySelects) {
        this.optionalPropertySelects = optionalPropertySelects;
    }

    /**
     * Returns a textual representation of the filter.
     *
     * @param writer    to output to
     * @param formatter for newline-whitespace formatting
     */
    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.write(eventTypeName);
        if (filter != null) {
            writer.write('(');
            filter.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
            writer.write(')');
        }
        if (optionalPropertySelects != null) {
            ContainedEventSelect.toEPL(writer, formatter, optionalPropertySelects);
        }
    }
}
