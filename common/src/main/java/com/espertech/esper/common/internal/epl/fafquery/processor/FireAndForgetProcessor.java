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
package com.espertech.esper.common.internal.epl.fafquery.processor;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.StatementContext;

public abstract class FireAndForgetProcessor {
    public abstract EventType getEventTypeResultSetProcessor();

    public abstract String getContextName();

    public abstract String getContextDeploymentId();

    public abstract FireAndForgetInstance getProcessorInstanceContextById(int agentInstanceId);

    public abstract FireAndForgetInstance getProcessorInstanceNoContext();

    public abstract EventType getEventTypePublic();

    public abstract StatementContext getStatementContext();
}