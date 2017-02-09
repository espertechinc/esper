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
 * Category-segmented context.
 */
public class ContextDescriptorCategory implements ContextDescriptor {

    private static final long serialVersionUID = 4054195679515606559L;
    private List<ContextDescriptorCategoryItem> items;
    private Filter filter;

    /**
     * Ctor.
     */
    public ContextDescriptorCategory() {
        items = new ArrayList<ContextDescriptorCategoryItem>();
    }

    /**
     * Ctor.
     *
     * @param items  categories
     * @param filter event type and predicate
     */
    public ContextDescriptorCategory(List<ContextDescriptorCategoryItem> items, Filter filter) {
        this.items = items;
        this.filter = filter;
    }

    /**
     * Returns categories.
     *
     * @return categories
     */
    public List<ContextDescriptorCategoryItem> getItems() {
        return items;
    }

    /**
     * Sets categories
     *
     * @param items categories to set
     */
    public void setItems(List<ContextDescriptorCategoryItem> items) {
        this.items = items;
    }

    /**
     * Returns type name and predicate expressions (filter)
     *
     * @return filter
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Sets type name and predicate expressions (filter)
     *
     * @param filter filter
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public void toEPL(StringWriter writer, EPStatementFormatter formatter) {
        String delimiter = "";
        for (ContextDescriptorCategoryItem item : items) {
            writer.append(delimiter);
            item.toEPL(writer, formatter);
            delimiter = ", ";
        }
        writer.append(" from ");
        filter.toEPL(writer, formatter);
    }
}
