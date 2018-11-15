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
package com.espertech.esper.common.internal.view.intersect;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.view.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for union-views.
 */
public class IntersectViewFactory implements ViewFactory, DataWindowViewFactory {
    protected EventType eventType;
    protected ViewFactory[] intersecteds;
    protected int batchViewIndex;
    protected boolean hasAsymetric;
    protected ThreadLocal<IntersectBatchViewLocalState> batchViewLocalState;
    protected ThreadLocal<IntersectDefaultViewLocalState> defaultViewLocalState;
    protected ThreadLocal<IntersectAsymetricViewLocalState> asymetricViewLocalState;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
        for (ViewFactory grouped : intersecteds) {
            grouped.init(viewFactoryContext, services);
        }

        if (batchViewIndex != -1) {
            batchViewLocalState = new ThreadLocal<IntersectBatchViewLocalState>() {
                protected synchronized IntersectBatchViewLocalState initialValue() {
                    return new IntersectBatchViewLocalState(new EventBean[intersecteds.length][], new EventBean[intersecteds.length][]);
                }
            };
        } else if (hasAsymetric) {
            asymetricViewLocalState = new ThreadLocal<IntersectAsymetricViewLocalState>() {
                protected synchronized IntersectAsymetricViewLocalState initialValue() {
                    return new IntersectAsymetricViewLocalState(new EventBean[intersecteds.length][]);
                }
            };
        } else {
            defaultViewLocalState = new ThreadLocal<IntersectDefaultViewLocalState>() {
                protected synchronized IntersectDefaultViewLocalState initialValue() {
                    return new IntersectDefaultViewLocalState(new EventBean[intersecteds.length][]);
                }
            };
        }
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        List<View> views = new ArrayList<>();
        for (ViewFactory viewFactory : intersecteds) {
            agentInstanceViewFactoryContext.setRemoveStream(true);
            views.add(viewFactory.makeView(agentInstanceViewFactoryContext));
        }
        if (batchViewIndex != -1) {
            return new IntersectBatchView(agentInstanceViewFactoryContext, this, views);
        } else if (hasAsymetric) {
            return new IntersectAsymetricView(agentInstanceViewFactoryContext, this, views);
        }
        return new IntersectDefaultView(agentInstanceViewFactoryContext, this, views);
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getBatchViewIndex() {
        return batchViewIndex;
    }

    public boolean isHasAsymetric() {
        return hasAsymetric;
    }

    public IntersectBatchViewLocalState getBatchViewLocalStatePerThread() {
        return batchViewLocalState.get();
    }

    public IntersectDefaultViewLocalState getDefaultViewLocalStatePerThread() {
        return defaultViewLocalState.get();
    }

    public IntersectAsymetricViewLocalState getAsymetricViewLocalStatePerThread() {
        return asymetricViewLocalState.get();
    }

    public void setBatchViewIndex(int batchViewIndex) {
        this.batchViewIndex = batchViewIndex;
    }

    public void setHasAsymetric(boolean hasAsymetric) {
        this.hasAsymetric = hasAsymetric;
    }

    public void setIntersecteds(ViewFactory[] intersecteds) {
        this.intersecteds = intersecteds;
    }

    public ViewFactory[] getIntersecteds() {
        return intersecteds;
    }

    public String getViewName() {
        return getViewNameUnionIntersect(true, intersecteds);
    }

    public static String getViewNameUnionIntersect(boolean intersect, ViewFactory[] factories) {
        StringBuilder buf = new StringBuilder();
        buf.append(intersect ? "Intersection" : "Union");

        if (factories == null) {
            return buf.toString();
        }
        buf.append(" of ");
        String delimiter = "";
        for (ViewFactory factory : factories) {
            buf.append(delimiter);
            buf.append(factory.getViewName());
            delimiter = ",";
        }

        return buf.toString();
    }
}
