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
 * Hash-segmented context.
 */
public class ContextDescriptorHashSegmented implements ContextDescriptor {

    private static final long serialVersionUID = 5131255287304974685L;
    private List<ContextDescriptorHashSegmentedItem> items;
    private int granularity;
    private boolean preallocate;

    /**
     * Ctor.
     */
    public ContextDescriptorHashSegmented() {
        items = new ArrayList<ContextDescriptorHashSegmentedItem>();
    }

    /**
     * Ctor.
     *
     * @param items       list of hash code functions and event types to apply to
     * @param granularity a number between 1 and Integer.MAX for parallelism
     * @param preallocate true to allocate each context partition at time of statement creation
     */
    public ContextDescriptorHashSegmented(List<ContextDescriptorHashSegmentedItem> items, int granularity, boolean preallocate) {
        this.items = items;
        this.granularity = granularity;
        this.preallocate = preallocate;
    }

    /**
     * Returns hash items.
     *
     * @return hash items
     */
    public List<ContextDescriptorHashSegmentedItem> getItems() {
        return items;
    }

    /**
     * Returns the granularity.
     *
     * @return granularity
     */
    public int getGranularity() {
        return granularity;
    }

    /**
     * Sets the granularity.
     *
     * @param granularity granularity to set
     */
    public void setGranularity(int granularity) {
        this.granularity = granularity;
    }

    /**
     * Returns flag indicating whether to allocate context partitions upon statement creation, or only when actually referred to
     *
     * @return preallocation flag
     */
    public boolean isPreallocate() {
        return preallocate;
    }

    /**
     * Set the flag indicating whether to allocate context partitions upon statement creation, or only when actually referred to
     *
     * @param preallocate preallocation flag
     */
    public void setPreallocate(boolean preallocate) {
        this.preallocate = preallocate;
    }

    /**
     * Sets categories
     *
     * @param items categories to set
     */
    public void setItems(List<ContextDescriptorHashSegmentedItem> items) {
        this.items = items;
    }

    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.append("coalesce ");
        String delimiter = "";
        for (ContextDescriptorHashSegmentedItem item : items) {
            writer.append(delimiter);
            item.toEPL(writer, formatter);
            delimiter = ", ";
        }
        writer.append(" granularity ");
        writer.append(Integer.toString(granularity));
        if (preallocate) {
            writer.append(" preallocate");
        }
    }
}
