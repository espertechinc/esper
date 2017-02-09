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
package com.espertech.esper.view.internal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.view.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Factory for union-views.
 */
public class IntersectViewFactory implements ViewFactory, DataWindowViewFactory, DataWindowViewFactoryUniqueCandidate, ViewFactoryContainer {
    protected EventType parentEventType;
    protected List<ViewFactory> viewFactories;
    protected int batchViewIndex;
    protected boolean hasAsymetric;
    protected ThreadLocal<IntersectBatchViewLocalState> batchViewLocalState;
    protected ThreadLocal<IntersectDefaultViewLocalState> defaultViewLocalState;
    protected ThreadLocal<IntersectAsymetricViewLocalState> asymetricViewLocalState;

    /**
     * Ctor.
     */
    public IntersectViewFactory() {
    }

    /**
     * Sets the parent event type.
     *
     * @param parentEventType type
     */
    public void setParentEventType(EventType parentEventType) {
        this.parentEventType = parentEventType;
    }

    /**
     * Sets the view factories.
     *
     * @param viewFactories factories
     */
    public void setViewFactories(final List<ViewFactory> viewFactories) {
        this.viewFactories = viewFactories;
        int batchCount = 0;
        for (int i = 0; i < viewFactories.size(); i++) {
            ViewFactory viewFactory = viewFactories.get(i);
            hasAsymetric |= viewFactory instanceof AsymetricDataWindowViewFactory;
            if (viewFactory instanceof DataWindowBatchingViewFactory) {
                batchCount++;
                batchViewIndex = i;
            }
        }
        if (batchCount > 1) {
            throw new ViewProcessingException("Cannot combined multiple batch data windows into an intersection");
        }

        if (batchCount == 1) {
            batchViewLocalState = new ThreadLocal<IntersectBatchViewLocalState>() {
                protected synchronized IntersectBatchViewLocalState initialValue() {
                    return new IntersectBatchViewLocalState(new EventBean[viewFactories.size()][], new EventBean[viewFactories.size()][]);
                }
            };
        } else if (hasAsymetric) {
            asymetricViewLocalState = new ThreadLocal<IntersectAsymetricViewLocalState>() {
                protected synchronized IntersectAsymetricViewLocalState initialValue() {
                    return new IntersectAsymetricViewLocalState(new EventBean[viewFactories.size()][]);
                }
            };
        } else {
            defaultViewLocalState = new ThreadLocal<IntersectDefaultViewLocalState>() {
                protected synchronized IntersectDefaultViewLocalState initialValue() {
                    return new IntersectDefaultViewLocalState(new EventBean[viewFactories.size()][]);
                }
            };
        }
    }

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> viewParameters) throws ViewParameterException {
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        List<View> views = new ArrayList<View>();
        boolean hasBatch = false;
        for (ViewFactory viewFactory : viewFactories) {
            agentInstanceViewFactoryContext.setRemoveStream(true);
            views.add(viewFactory.makeView(agentInstanceViewFactoryContext));
            hasBatch |= viewFactory instanceof DataWindowBatchingViewFactory;
        }
        if (hasBatch) {
            return new IntersectBatchView(agentInstanceViewFactoryContext, this, views);
        } else if (hasAsymetric) {
            return new IntersectAsymetricView(agentInstanceViewFactoryContext, this, views);
        }
        return new IntersectDefaultView(agentInstanceViewFactoryContext, this, views);
    }

    public EventType getEventType() {
        return parentEventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        return false;
    }

    public Set<String> getUniquenessCandidatePropertyNames() {
        for (ViewFactory viewFactory : viewFactories) {
            if (viewFactory instanceof DataWindowViewFactoryUniqueCandidate) {
                DataWindowViewFactoryUniqueCandidate unique = (DataWindowViewFactoryUniqueCandidate) viewFactory;
                Set<String> props = unique.getUniquenessCandidatePropertyNames();
                if (props != null) {
                    return props;
                }
            }
        }
        return null;
    }

    public String getViewName() {
        return getViewNameUnionIntersect(true, viewFactories);
    }

    public Collection<ViewFactory> getViewFactoriesContained() {
        return viewFactories;
    }

    protected static String getViewNameUnionIntersect(boolean intersect, Collection<ViewFactory> factories) {
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

    public EventType getParentEventType() {
        return parentEventType;
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
}
