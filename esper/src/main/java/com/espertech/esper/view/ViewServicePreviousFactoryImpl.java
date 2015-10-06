/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.view;

import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.view.ext.IStreamSortRankRandomAccess;
import com.espertech.esper.view.window.IStreamRandomAccess;
import com.espertech.esper.view.window.IStreamRelativeAccess;
import com.espertech.esper.view.window.RandomAccessByIndexGetter;
import com.espertech.esper.view.window.RelativeAccessByEventNIndexMap;

public class ViewServicePreviousFactoryImpl implements ViewServicePreviousFactory
{
    public IStreamRandomAccess getOptPreviousExprRandomAccess(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        IStreamRandomAccess randomAccess = null;
        if (agentInstanceViewFactoryContext.getPreviousNodeGetter() != null)
        {
            RandomAccessByIndexGetter getter = (RandomAccessByIndexGetter) agentInstanceViewFactoryContext.getPreviousNodeGetter();
            randomAccess = new IStreamRandomAccess(getter);
            getter.updated(randomAccess);
        }
        return randomAccess;
    }

    public IStreamRelativeAccess getOptPreviousExprRelativeAccess(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        IStreamRelativeAccess relativeAccessByEvent = null;

        if (agentInstanceViewFactoryContext.getPreviousNodeGetter() != null)
        {
            RelativeAccessByEventNIndexMap getter = (RelativeAccessByEventNIndexMap) agentInstanceViewFactoryContext.getPreviousNodeGetter();
            relativeAccessByEvent = new IStreamRelativeAccess(getter);
            getter.updated(relativeAccessByEvent, null);
        }

        return relativeAccessByEvent;
    }

    public IStreamSortRankRandomAccess getOptPreviousExprSortedRankedAccess(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        IStreamSortRankRandomAccess rankedRandomAccess = null;

        if (agentInstanceViewFactoryContext.getPreviousNodeGetter() != null)
        {
            RandomAccessByIndexGetter getter = (RandomAccessByIndexGetter) agentInstanceViewFactoryContext.getPreviousNodeGetter();
            rankedRandomAccess = new IStreamSortRankRandomAccess(getter);
            getter.updated(rankedRandomAccess);
        }

        return rankedRandomAccess;
    }
}
