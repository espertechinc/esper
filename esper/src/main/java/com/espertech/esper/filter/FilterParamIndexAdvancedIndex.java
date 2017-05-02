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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.index.quadtree.AdvancedIndexConfigContextPartitionQuadTree;
import com.espertech.esper.spatial.quadtree.core.QuadTree;
import com.espertech.esper.spatial.quadtree.core.QuadTreeFactory;
import com.espertech.esper.spatial.quadtree.core.XYPoint;
import com.espertech.esper.spatial.quadtree.filterindex.*;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;

public class FilterParamIndexAdvancedIndex extends FilterParamIndexLookupableBase {
    private final ReadWriteLock readWriteLock;
    private final QuadTree<Object> quadTree;
    private final FilterSpecLookupableAdvancedIndex advancedIndex;

    private final static QuadTreeCollector<EventEvaluator, Collection<FilterHandle>> COLLECTOR = new QuadTreeCollector<EventEvaluator, Collection<FilterHandle>>() {
        public void collectInto(EventBean event, EventEvaluator eventEvaluator, Collection<FilterHandle> c) {
            eventEvaluator.matchEvent(event, c);
        }
    };

    public FilterParamIndexAdvancedIndex(ReadWriteLock readWriteLock, FilterSpecLookupable lookupable) {
        super(FilterOperator.ADVANCED_INDEX, lookupable);
        this.readWriteLock = readWriteLock;
        this.advancedIndex = (FilterSpecLookupableAdvancedIndex) lookupable;
        AdvancedIndexConfigContextPartitionQuadTree quadTreeConfig = advancedIndex.getQuadTreeConfig();
        quadTree = QuadTreeFactory.make(quadTreeConfig.getX(), quadTreeConfig.getY(), quadTreeConfig.getWidth(), quadTreeConfig.getHeight());
    }

    public void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
        double x = ((Number) advancedIndex.getX().get(theEvent)).doubleValue();
        double y = ((Number) advancedIndex.getY().get(theEvent)).doubleValue();
        double width = ((Number) advancedIndex.getWidth().get(theEvent)).doubleValue();
        double height = ((Number) advancedIndex.getHeight().get(theEvent)).doubleValue();
        QuadTreeFilterIndexCollect.collectRange(quadTree, x, y, width, height, theEvent, matches, COLLECTOR);
    }

    public EventEvaluator get(Object filterConstant) {
        XYPoint point = (XYPoint) filterConstant;
        return QuadTreeFilterIndexGet.get(point.getX(), point.getY(), quadTree);
    }

    public void put(Object filterConstant, EventEvaluator evaluator) {
        XYPoint point = (XYPoint) filterConstant;
        QuadTreeFilterIndexSet.set(point.getX(), point.getY(), evaluator, quadTree);
    }

    public void remove(Object filterConstant) {
        XYPoint point = (XYPoint) filterConstant;
        QuadTreeFilterIndexDelete.delete(point.getX(), point.getY(), quadTree);
    }

    public int sizeExpensive() {
        return QuadTreeFilterIndexCount.count(quadTree);
    }

    public boolean isEmpty() {
        return QuadTreeFilterIndexEmpty.isEmpty(quadTree);
    }

    public ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }
}
