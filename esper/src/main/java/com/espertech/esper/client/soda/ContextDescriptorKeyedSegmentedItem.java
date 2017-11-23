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
 * Context detail for a key-filter pair for the keyed segmented context.
 */
public class ContextDescriptorKeyedSegmentedItem implements ContextDescriptor {

    private static final long serialVersionUID = -5135918405149193009L;
    private List<String> propertyNames;
    private Filter filter;
    private String streamName;

    /**
     * Ctor.
     */
    public ContextDescriptorKeyedSegmentedItem() {
    }

    /**
     * Ctor.
     *
     * @param propertyNames list of property names
     * @param filter        event type name and optional filter predicates
     */
    public ContextDescriptorKeyedSegmentedItem(List<String> propertyNames, Filter filter) {
        this.propertyNames = propertyNames;
        this.filter = filter;
    }

    /**
     * Ctor.
     *
     * @param propertyNames list of property names
     * @param filter        event type name and optional filter predicates
     * @param streamName    alias name
     */
    public ContextDescriptorKeyedSegmentedItem(List<String> propertyNames, Filter filter, String streamName) {
        this.propertyNames = propertyNames;
        this.filter = filter;
        this.streamName = streamName;
    }

    /**
     * Returns the filter.
     *
     * @return filter
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Sets the filter.
     *
     * @param filter filter
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * Returns the property names.
     *
     * @return list
     */
    public List<String> getPropertyNames() {
        return propertyNames;
    }

    /**
     * Sets the property names.
     *
     * @param propertyNames list
     */
    public void setPropertyNames(List<String> propertyNames) {
        this.propertyNames = propertyNames;
    }

    /**
     * Returns the stream name.
     * @return stream name
     */
    public String getStreamName() {
        return streamName;
    }

    /**
     * Sets the stream name.
     * @param streamName stream name
     */
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        String delimiter = "";
        for (String prop : propertyNames) {
            writer.append(delimiter);
            writer.append(prop);
            delimiter = " and ";
        }
        writer.append(" from ");
        filter.toEPL(writer, formatter);
        if (streamName != null) {
            writer.append(" as ").append(streamName);
        }
    }
}
