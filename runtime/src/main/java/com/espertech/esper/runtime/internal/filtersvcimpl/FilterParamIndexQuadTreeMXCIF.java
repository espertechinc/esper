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
import com.espertech.esper.common.internal.epl.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.common.internal.epl.spatial.quadtree.mxcif.MXCIFQuadTreeFactory;
import com.espertech.esper.common.internal.epl.spatial.quadtree.mxciffilterindex.*;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecLookupableAdvancedIndex;
import com.espertech.esper.common.internal.filtersvc.FilterHandle;
import com.espertech.esper.common.internal.type.XYWHRectangle;
import com.espertech.esper.runtime.internal.metrics.instrumentation.InstrumentationHelper;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

public class FilterParamIndexQuadTreeMXCIF extends FilterParamIndexLookupableBase {
    private final ReadWriteLock readWriteLock;
    private final MXCIFQuadTree<Object> quadTree;
    private final FilterSpecLookupableAdvancedIndex advancedIndex;

    private final static QuadTreeCollector<EventEvaluator, Collection<FilterHandle>> COLLECTOR = new QuadTreeCollector<EventEvaluator, Collection<FilterHandle>>() {
        public void collectInto(EventBean event, EventEvaluator eventEvaluator, Collection<FilterHandle> c) {
            eventEvaluator.matchEvent(event, c);
        }
    };

    public FilterParamIndexQuadTreeMXCIF(ReadWriteLock readWriteLock, ExprFilterSpecLookupable lookupable) {
        super(FilterOperator.ADVANCED_INDEX, lookupable);
        this.readWriteLock = readWriteLock;
        this.advancedIndex = (FilterSpecLookupableAdvancedIndex) lookupable;
        AdvancedIndexConfigContextPartitionQuadTree quadTreeConfig = advancedIndex.getQuadTreeConfig();
        quadTree = MXCIFQuadTreeFactory.make(quadTreeConfig.getX(), quadTreeConfig.getY(), quadTreeConfig.getWidth(), quadTreeConfig.getHeight());
    }

    public void matchEvent(EventBean theEvent, Collection<FilterHandle> matches) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qFilterReverseIndex(this, null);
        }

        double x = ((Number) advancedIndex.getX().get(theEvent)).doubleValue();
        double y = ((Number) advancedIndex.getY().get(theEvent)).doubleValue();
        double width = ((Number) advancedIndex.getWidth().get(theEvent)).doubleValue();
        double height = ((Number) advancedIndex.getHeight().get(theEvent)).doubleValue();
        MXCIFQuadTreeFilterIndexCollect.collectRange(quadTree, x, y, width, height, theEvent, matches, COLLECTOR);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aFilterReverseIndex(null);
        }
    }

    public EventEvaluator get(Object filterConstant) {
        XYWHRectangle rect = (XYWHRectangle) filterConstant;
        return MXCIFQuadTreeFilterIndexGet.get(rect.getX(), rect.getY(), rect.getW(), rect.getH(), quadTree);
    }

    public void put(Object filterConstant, EventEvaluator evaluator) {
        XYWHRectangle rect = (XYWHRectangle) filterConstant;
        MXCIFQuadTreeFilterIndexSet.set(rect.getX(), rect.getY(), rect.getW(), rect.getH(), evaluator, quadTree);
    }

    public void remove(Object filterConstant) {
        XYWHRectangle rect = (XYWHRectangle) filterConstant;
        MXCIFQuadTreeFilterIndexDelete.delete(rect.getX(), rect.getY(), rect.getW(), rect.getH(), quadTree);
    }

    public int sizeExpensive() {
        return MXCIFQuadTreeFilterIndexCount.count(quadTree);
    }

    public boolean isEmpty() {
        return MXCIFQuadTreeFilterIndexEmpty.isEmpty(quadTree);
    }

    public ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }

    public void getTraverseStatement(EventTypeIndexTraverse traverse, Set<Integer> statementIds, ArrayDeque<FilterItem> evaluatorStack) {
        evaluatorStack.push(new FilterItem(advancedIndex.getExpression(), FilterOperator.ADVANCED_INDEX));
        MXCIFQuadTreeFilterIndexTraverse.traverse(quadTree, object -> {
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
