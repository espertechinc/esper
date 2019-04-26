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
package com.espertech.esper.common.internal.filterspec;


import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.AdvancedIndexConfigContextPartitionQuadTree;

public class FilterSpecLookupableAdvancedIndex extends ExprFilterSpecLookupable {
    private AdvancedIndexConfigContextPartitionQuadTree quadTreeConfig;
    private EventPropertyValueGetter x;
    private EventPropertyValueGetter y;
    private EventPropertyValueGetter width;
    private EventPropertyValueGetter height;
    private String indexType;

    public FilterSpecLookupableAdvancedIndex(String expression, EventPropertyGetter getter, Class returnType) {
        super(expression, getter, returnType, true, null);
    }

    public EventPropertyValueGetter getX() {
        return x;
    }

    public EventPropertyValueGetter getY() {
        return y;
    }

    public EventPropertyValueGetter getWidth() {
        return width;
    }

    public EventPropertyValueGetter getHeight() {
        return height;
    }

    public AdvancedIndexConfigContextPartitionQuadTree getQuadTreeConfig() {
        return quadTreeConfig;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setQuadTreeConfig(AdvancedIndexConfigContextPartitionQuadTree quadTreeConfig) {
        this.quadTreeConfig = quadTreeConfig;
    }

    public void setX(EventPropertyValueGetter x) {
        this.x = x;
    }

    public void setY(EventPropertyValueGetter y) {
        this.y = y;
    }

    public void setWidth(EventPropertyValueGetter width) {
        this.width = width;
    }

    public void setHeight(EventPropertyValueGetter height) {
        this.height = height;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }
}


