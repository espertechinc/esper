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
package com.espertech.esper.common.internal.context.aifactory.update;

import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Collections;
import java.util.Map;

public class StatementAgentInstanceFactoryUpdateResult extends StatementAgentInstanceFactoryResult {
    public StatementAgentInstanceFactoryUpdateResult(Viewable finalView, AgentInstanceStopCallback stopCallback, AgentInstanceContext agentInstanceContext, Map<Integer, SubSelectFactoryResult> subselectActivations) {
        super(finalView, stopCallback, agentInstanceContext, null, subselectActivations, null, null, null, null, Collections.emptyList());
    }
}
