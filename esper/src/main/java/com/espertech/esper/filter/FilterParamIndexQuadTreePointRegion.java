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
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.spatial.quadtree.core.QuadTreeCollector;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTree;
import com.espertech.esper.spatial.quadtree.pointregion.PointRegionQuadTreeFactory;
import com.espertech.esper.type.XYPoint;
import com.espertech.esper.spatial.quadtree.prqdfilterindex.*;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;

public class FilterParamIndexQuadTreePointRegion extends FilterParamIndexLookupableBase {
    private final ReadWriteLock readWriteLock;
    private final PointRegionQuadTree<Object> quadTree;
    private final FilterSpecLookupableAdvancedIndex advancedIndex;

    private final static QuadTreeCollector<EventEvaluator, Collection<FilterHandle>> COLLECTOR = new QuadTreeCollector<EventEvaluator, Collection<FilterHandle>>() {
        public void collectInto(EventBean event, EventEvaluator eventEvaluator, Collection<FilterHandle> c) {
            eventEvaluator.matchEvent(event, c);
        }
    };

    public FilterParamIndexQuadTreePointRegion(ReadWriteLock readWriteLock, ExprFilterSpecLookupable lookupable) {
        super(FilterOperator.ADVANCED_INDEX, lookupable);
        this.readWriteLock = readWriteLock;
        this.advancedIndex = (FilterSpecLookupableAdvancedIndex) lookupable;
        AdvancedIndexConfigContextPartitionQuadTree quadTreeConfig = advancedIndex.getQuadTreeConfig();
        quadTree = PointRegionQuadTreeFactory.make(quadTreeConfig.getX(), quadTreeConfig.getY(), quadTreeConfig.getWidth(), quadTreeConfig.getHeight());
    }

    public void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
        double x = ((Number) advancedIndex.getX().get(theEvent)).doubleValue();
        double y = ((Number) advancedIndex.getY().get(theEvent)).doubleValue();
        double width = ((Number) advancedIndex.getWidth().get(theEvent)).doubleValue();
        double height = ((Number) advancedIndex.getHeight().get(theEvent)).doubleValue();
        PointRegionQuadTreeFilterIndexCollect.collectRange(quadTree, x, y, width, height, theEvent, matches, COLLECTOR);
    }

    public EventEvaluator get(Object filterConstant) {
        XYPoint point = (XYPoint) filterConstant;
        return PointRegionQuadTreeFilterIndexGet.get(point.getX(), point.getY(), quadTree);
    }

    public void put(Object filterConstant, EventEvaluator evaluator) {
        XYPoint point = (XYPoint) filterConstant;
        PointRegionQuadTreeFilterIndexSet.set(point.getX(), point.getY(), evaluator, quadTree);
    }

    public void remove(Object filterConstant) {
        XYPoint point = (XYPoint) filterConstant;
        PointRegionQuadTreeFilterIndexDelete.delete(point.getX(), point.getY(), quadTree);
    }

    public int sizeExpensive() {
        return PointRegionQuadTreeFilterIndexCount.count(quadTree);
    }

    public boolean isEmpty() {
        return PointRegionQuadTreeFilterIndexEmpty.isEmpty(quadTree);
    }

    public ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }
}
