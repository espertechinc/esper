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
package com.espertech.esper.epl.index.quadtree;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.table.EventTableOrganization;
import com.espertech.esper.spatial.quadtree.core.BoundingBox;
import com.espertech.esper.spatial.quadtree.mxcif.MXCIFQuadTree;
import com.espertech.esper.spatial.quadtree.mxcifrowindex.MXCIFQuadTreeRowIndexAdd;
import com.espertech.esper.spatial.quadtree.mxcifrowindex.MXCIFQuadTreeRowIndexQuery;
import com.espertech.esper.spatial.quadtree.mxcifrowindex.MXCIFQuadTreeRowIndexRemove;

import java.util.Collection;
import java.util.Iterator;

import static com.espertech.esper.epl.index.quadtree.AdvancedIndexQuadTreeConstants.*;
import static com.espertech.esper.epl.index.service.AdvancedIndexEvaluationHelper.evalDoubleColumn;
import static com.espertech.esper.epl.index.service.AdvancedIndexEvaluationHelper.invalidColumnValue;

public class EventTableQuadTreeMXCIFImpl implements EventTableQuadTree {

    private final EventTableOrganization organization;
    private final EventBean[] eventsPerStream = new EventBean[1];
    private final AdvancedIndexConfigStatementMXCIFQuadtree config;
    private final MXCIFQuadTree<Object> quadTree;

    public EventTableQuadTreeMXCIFImpl(EventTableOrganization organization, AdvancedIndexConfigStatementMXCIFQuadtree config, MXCIFQuadTree<Object> quadTree) {
        this.organization = organization;
        this.config = config;
        this.quadTree = quadTree;
    }

    public Collection<EventBean> queryRange(double x, double y, double width, double height) {
        return (Collection<EventBean>) (Collection) MXCIFQuadTreeRowIndexQuery.queryRange(quadTree, x, y, width, height);
    }

    public void addRemove(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext exprEvaluatorContext) {
        remove(oldData, exprEvaluatorContext);
        add(newData, exprEvaluatorContext);
    }

    public void add(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        for (EventBean added : events) {
            add(added, exprEvaluatorContext);
        }
    }

    public void remove(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        for (EventBean removed : events) {
            remove(removed, exprEvaluatorContext);
        }
    }

    public void add(EventBean event, ExprEvaluatorContext exprEvaluatorContext) {
        eventsPerStream[0] = event;
        double x = evalDoubleColumn(config.getxEval(), organization.getIndexName(), COL_X, eventsPerStream, true, exprEvaluatorContext);
        double y = evalDoubleColumn(config.getyEval(), organization.getIndexName(), COL_Y, eventsPerStream, true, exprEvaluatorContext);
        double width = evalDoubleColumn(config.getWidthEval(), organization.getIndexName(), COL_WIDTH, eventsPerStream, true, exprEvaluatorContext);
        double height = evalDoubleColumn(config.getHeightEval(), organization.getIndexName(), COL_HEIGHT, eventsPerStream, true, exprEvaluatorContext);
        boolean added = MXCIFQuadTreeRowIndexAdd.add(x, y, width, height, event, quadTree, organization.isUnique(), organization.getIndexName());
        if (!added) {
            throw invalidColumnValue(organization.getIndexName(), "(x,y,width,height)", "(" + x + "," + y + "," + width + "," + height + ")", "a value intersecting index bounding box (range-end-inclusive) " + quadTree.getRoot().getBb());
        }
    }

    public void remove(EventBean event, ExprEvaluatorContext exprEvaluatorContext) {
        eventsPerStream[0] = event;
        double x = evalDoubleColumn(config.getxEval(), organization.getIndexName(), COL_X, eventsPerStream, false, exprEvaluatorContext);
        double y = evalDoubleColumn(config.getyEval(), organization.getIndexName(), COL_Y, eventsPerStream, false, exprEvaluatorContext);
        double width = evalDoubleColumn(config.getWidthEval(), organization.getIndexName(), COL_WIDTH, eventsPerStream, false, exprEvaluatorContext);
        double height = evalDoubleColumn(config.getHeightEval(), organization.getIndexName(), COL_HEIGHT, eventsPerStream, false, exprEvaluatorContext);
        MXCIFQuadTreeRowIndexRemove.remove(x, y, width, height, event, quadTree);
    }

    public Iterator<EventBean> iterator() {
        BoundingBox bb = quadTree.getRoot().getBb();
        Collection<EventBean> events = queryRange(bb.getMinX(), bb.getMinY(), bb.getMaxX() - bb.getMinX(), bb.getMaxY() - bb.getMinY());
        return events.iterator();
    }

    public boolean isEmpty() {
        return false; // assumed non-empty
    }

    public void clear() {
        quadTree.clear();
    }

    public void destroy() {
    }

    public String toQueryPlan() {
        return this.getClass().toString();
    }

    public Class getProviderClass() {
        return this.getClass();
    }

    public Integer getNumberOfEvents() {
        return null;
    }

    public int getNumKeys() {
        return -1;
    }

    public Object getIndex() {
        return quadTree;
    }

    public EventTableOrganization getOrganization() {
        return organization;
    }
}
