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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.AdvancedIndexConfigContextPartitionQuadTree;
import com.espertech.esper.common.internal.epl.spatial.quadtree.core.QuadTreeCollector;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTree;
import com.espertech.esper.common.internal.epl.spatial.quadtree.pointregion.PointRegionQuadTreeFactory;
import com.espertech.esper.common.internal.epl.spatial.quadtree.prqdfilterindex.*;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecLookupableAdvancedIndex;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.type.XYPoint;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Set;
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
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterReverseIndex(this, null);
        }

        double x = ((Number) advancedIndex.getX().get(theEvent)).doubleValue();
        double y = ((Number) advancedIndex.getY().get(theEvent)).doubleValue();
        double width = ((Number) advancedIndex.getWidth().get(theEvent)).doubleValue();
        double height = ((Number) advancedIndex.getHeight().get(theEvent)).doubleValue();
        PointRegionQuadTreeFilterIndexCollect.collectRange(quadTree, x, y, width, height, theEvent, matches, COLLECTOR);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterReverseIndex(null);
        }
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

    public void getTraverseStatement(EventTypeIndexTraverse traverse, Set<Integer> statementIds, ArrayDeque<FilterItem> evaluatorStack) {
        evaluatorStack.push(new FilterItem(advancedIndex.getExpression(), FilterOperator.ADVANCED_INDEX));
        PointRegionQuadTreeFilterIndexTraverse.traverse(quadTree, object -> {
            if (object instanceof FilterHandleSetNode) {
                ((FilterHandleSetNode) object).getTraverseStatement(traverse, statementIds, evaluatorStack);
                return;
            }
            if (object instanceof FilterHandle) {
                traverse.add(evaluatorStack, (FilterHandle) object);
            }
        });
        evaluatorStack.pop();
    }
}
