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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.context.ContextPartitionIdentifier;
import com.espertech.esper.client.context.ContextPartitionSelector;

import java.util.Map;

public interface ContextController {
    public int getPathId();

    public void activate(EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, ContextControllerState states, ContextInternalFilterAddendum filterAddendum, Integer importPathId);

    public ContextControllerFactory getFactory();

    public void deactivate();

    public void visitSelectedPartitions(ContextPartitionSelector contextPartitionSelector, ContextPartitionVisitor visitor);

    public void importContextPartitions(ContextControllerState state, int pathIdToUse, ContextInternalFilterAddendum filterAddendum, AgentInstanceSelector agentInstanceSelector);

    public void deletePath(ContextPartitionIdentifier identifier);
}
