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
package com.espertech.esper.common.internal.view.union;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.intersect.IntersectViewFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for union-views.
 */
public class UnionViewFactory implements ViewFactory, DataWindowViewFactory {
    protected EventType eventType;
    protected ViewFactory[] unioned;
    protected boolean hasAsymetric;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
        for (ViewFactory factory : unioned) {
            factory.init(viewFactoryContext, services);
        }
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        List<View> views = new ArrayList<>();
        for (ViewFactory viewFactory : unioned) {
            views.add(viewFactory.makeView(agentInstanceViewFactoryContext));
        }
        if (hasAsymetric) {
            return new UnionAsymetricView(agentInstanceViewFactoryContext, this, views);
        }
        return new UnionView(agentInstanceViewFactoryContext, this, views);
    }

    public EventType getEventType() {
        return eventType;
    }

    public ViewFactory[] getUnioned() {
        return unioned;
    }

    public void setUnioned(ViewFactory[] unioned) {
        this.unioned = unioned;
    }

    public boolean isHasAsymetric() {
        return hasAsymetric;
    }

    public void setHasAsymetric(boolean hasAsymetric) {
        this.hasAsymetric = hasAsymetric;
    }

    public String getViewName() {
        return IntersectViewFactory.getViewNameUnionIntersect(false, unioned);
    }
}
