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

/**
 * Context condition that start/initiated or ends/terminates context partitions based on a filter expression.
 */
public class ContextDescriptorConditionFilter implements ContextDescriptorCondition {

    private static final long serialVersionUID = 7022506366665896834L;
    private Filter filter;
    private String optionalAsName;

    /**
     * Ctor.
     */
    public ContextDescriptorConditionFilter() {
    }

    /**
     * Ctor.
     *
     * @param filter         event filter
     * @param optionalAsName tag name of the filtered events
     */
    public ContextDescriptorConditionFilter(Filter filter, String optionalAsName) {
        this.filter = filter;
        this.optionalAsName = optionalAsName;
    }

    /**
     * Returns the event stream filter.
     *
     * @return filter
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Returns the tag name assigned, if any.
     *
     * @return tag name
     */
    public String getOptionalAsName() {
        return optionalAsName;
    }

    /**
     * Sets the event stream filter.
     *
     * @param filter filter to set
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * Returns the tag name assigned, if any.
     *
     * @param optionalAsName tag name
     */
    public void setOptionalAsName(String optionalAsName) {
        this.optionalAsName = optionalAsName;
    }

    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        filter.toEPL(writer, formatter);
        if (optionalAsName != null) {
            writer.append(" as ");
            writer.append(optionalAsName);
        }
    }
}
