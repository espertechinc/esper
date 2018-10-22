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
package com.espertech.esper.common.internal.context.aifactory.createschema;

import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.Collections;

public class StatementAgentInstanceFactoryCreateSchemaResult extends StatementAgentInstanceFactoryResult {
    public StatementAgentInstanceFactoryCreateSchemaResult(Viewable finalView, AgentInstanceStopCallback stopCallback, AgentInstanceContext agentInstanceContext) {
        super(finalView, stopCallback, agentInstanceContext, null, Collections.emptyMap(), null, null, null, null, Collections.emptyList());
    }
}
