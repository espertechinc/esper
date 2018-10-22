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
package com.espertech.esper.common.internal.view.previous;

import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.view.access.RandomAccessByIndexGetter;
import com.espertech.esper.common.internal.view.access.RelativeAccessByEventNIndexGetter;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;

public class ViewServicePreviousFactoryImpl implements ViewServicePreviousFactory {

    public final static ViewServicePreviousFactoryImpl INSTANCE = new ViewServicePreviousFactoryImpl();

    private ViewServicePreviousFactoryImpl() {
    }

    public ViewUpdatedCollection getOptPreviousExprRandomAccess(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        IStreamRandomAccess randomAccess = null;
        if (agentInstanceViewFactoryContext.getPreviousNodeGetter() != null) {
            RandomAccessByIndexGetter getter = (RandomAccessByIndexGetter) agentInstanceViewFactoryContext.getPreviousNodeGetter();
            randomAccess = new IStreamRandomAccess(getter);
            getter.updated(randomAccess);
        }
        return randomAccess;
    }

    public ViewUpdatedCollection getOptPreviousExprRelativeAccess(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        IStreamRelativeAccess relativeAccessByEvent = null;

        if (agentInstanceViewFactoryContext.getPreviousNodeGetter() != null) {
            RelativeAccessByEventNIndexGetter getter = (RelativeAccessByEventNIndexGetter) agentInstanceViewFactoryContext.getPreviousNodeGetter();
            IStreamRelativeAccess.IStreamRelativeAccessUpdateObserver observer = (IStreamRelativeAccess.IStreamRelativeAccessUpdateObserver) getter;
            relativeAccessByEvent = new IStreamRelativeAccess(observer);
            observer.updated(relativeAccessByEvent, null);
        }

        return relativeAccessByEvent;
    }

    public IStreamSortRankRandomAccess getOptPreviousExprSortedRankedAccess(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        IStreamSortRankRandomAccess rankedRandomAccess = null;

        if (agentInstanceViewFactoryContext.getPreviousNodeGetter() != null) {
            RandomAccessByIndexGetter getter = (RandomAccessByIndexGetter) agentInstanceViewFactoryContext.getPreviousNodeGetter();
            rankedRandomAccess = new IStreamSortRankRandomAccessImpl(getter);
            getter.updated(rankedRandomAccess);
        }

        return rankedRandomAccess;
    }
}
