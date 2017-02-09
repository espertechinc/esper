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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.core.context.util.AgentInstanceContext;

public class EventTableFactoryTableIdentAgentInstanceSubq extends EventTableFactoryTableIdentAgentInstance {
    private final int subqueryNumber;

    public EventTableFactoryTableIdentAgentInstanceSubq(AgentInstanceContext agentInstanceContext, int subqueryNumber) {
        super(agentInstanceContext);
        this.subqueryNumber = subqueryNumber;
    }

    public int getSubqueryNumber() {
        return subqueryNumber;
    }
}
