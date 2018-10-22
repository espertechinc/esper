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
package com.espertech.esper.common.internal.context.aifactory.createwindow;

import com.espertech.esper.common.internal.context.activator.ViewableActivationResult;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Collections;

public class StatementAgentInstanceFactoryCreateNWResult extends StatementAgentInstanceFactoryResult {

    private final Viewable eventStreamParentViewable;
    private final Viewable topView;
    private final NamedWindowInstance namedWindowInstance;
    private final ViewableActivationResult viewableActivationResult;

    public StatementAgentInstanceFactoryCreateNWResult(Viewable finalView,
                                                       AgentInstanceStopCallback stopCallback,
                                                       AgentInstanceContext agentInstanceContext,
                                                       Viewable eventStreamParentViewable,
                                                       Viewable topView,
                                                       NamedWindowInstance namedWindowInstance,
                                                       ViewableActivationResult viewableActivationResult) {
        super(finalView, stopCallback, agentInstanceContext,
                null,
                Collections.<Integer, SubSelectFactoryResult>emptyMap(),
                null,
                null,
                null,
                Collections.emptyMap(),
                null
        );
        this.eventStreamParentViewable = eventStreamParentViewable;
        this.topView = topView;
        this.namedWindowInstance = namedWindowInstance;
        this.viewableActivationResult = viewableActivationResult;
    }

    public Viewable getEventStreamParentViewable() {
        return eventStreamParentViewable;
    }

    public Viewable getTopView() {
        return topView;
    }

    public NamedWindowInstance getNamedWindowInstance() {
        return namedWindowInstance;
    }

    public ViewableActivationResult getViewableActivationResult() {
        return viewableActivationResult;
    }
}
