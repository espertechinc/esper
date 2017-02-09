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
 * Context dimension information for keyed segmented context.
 */
public class ContextDescriptorKeyedSegmented implements ContextDescriptor {

    private static final long serialVersionUID = -7627632179534547604L;
    private List<ContextDescriptorKeyedSegmentedItem> items;

    /**
     * Ctor.
     */
    public ContextDescriptorKeyedSegmented() {
        items = new ArrayList<ContextDescriptorKeyedSegmentedItem>();
    }

    /**
     * Ctor.
     *
     * @param items key set descriptions
     */
    public ContextDescriptorKeyedSegmented(List<ContextDescriptorKeyedSegmentedItem> items) {
        this.items = items;
    }

    /**
     * Returns the key set descriptions
     *
     * @return list
     */
    public List<ContextDescriptorKeyedSegmentedItem> getItems() {
        return items;
    }

    /**
     * Sets the key set descriptions
     *
     * @param items list
     */
    public void setItems(List<ContextDescriptorKeyedSegmentedItem> items) {
        this.items = items;
    }

    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        writer.append("partition by ");
        String delimiter = "";
        for (ContextDescriptorKeyedSegmentedItem item : items) {
            writer.append(delimiter);
            item.toEPL(writer, formatter);
            delimiter = ", ";
        }
    }
}
