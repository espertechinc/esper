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

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.epl.spec.*;

public class ContextControllerConditionFactory {

    public static ContextControllerCondition getEndpoint(String contextName,
                                                         EPServicesContext servicesContext,
                                                         AgentInstanceContext agentInstanceContext,
                                                         ContextDetailCondition endpoint,
                                                         ContextControllerConditionCallback callback,
                                                         ContextInternalFilterAddendum filterAddendum,
                                                         boolean isStartEndpoint,
                                                         int nestingLevel,
                                                         int pathId,
                                                         int subpathId) {
        if (endpoint instanceof ContextDetailConditionCrontab) {
            ContextDetailConditionCrontab crontab = (ContextDetailConditionCrontab) endpoint;
            long scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
            return new ContextControllerConditionCrontab(agentInstanceContext.getStatementContext(), scheduleSlot, crontab, callback, filterAddendum);
        } else if (endpoint instanceof ContextDetailConditionFilter) {
            ContextDetailConditionFilter filter = (ContextDetailConditionFilter) endpoint;
            return new ContextControllerConditionFilter(servicesContext, agentInstanceContext, filter, callback, filterAddendum);
        } else if (endpoint instanceof ContextDetailConditionPattern) {
            ContextStatePathKey key = new ContextStatePathKey(nestingLevel, pathId, subpathId);
            ContextDetailConditionPattern pattern = (ContextDetailConditionPattern) endpoint;
            return new ContextControllerConditionPattern(servicesContext, agentInstanceContext, pattern, callback, filterAddendum, isStartEndpoint, key);
        } else if (endpoint instanceof ContextDetailConditionTimePeriod) {
            ContextDetailConditionTimePeriod timePeriod = (ContextDetailConditionTimePeriod) endpoint;
            long scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
            return new ContextControllerConditionTimePeriod(contextName, agentInstanceContext, scheduleSlot, timePeriod, callback, filterAddendum);
        } else if (endpoint instanceof ContextDetailConditionImmediate) {
            return new ContextControllerConditionImmediate();
        } else if (endpoint instanceof ContextDetailConditionNever) {
            return new ContextControllerConditionNever();
        }
        throw new IllegalStateException("Unrecognized context range endpoint " + endpoint.getClass());
    }
}
