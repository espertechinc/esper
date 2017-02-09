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
package com.espertech.esper.core.context.util;

import com.espertech.esper.core.context.mgr.AgentInstance;

import java.io.Serializable;
import java.util.Comparator;

public class AgentInstanceComparator implements Comparator<AgentInstance>, Serializable {

    private static final long serialVersionUID = 8926266145763075051L;

    public final static AgentInstanceComparator INSTANCE = new AgentInstanceComparator();

    private EPStatementAgentInstanceHandleComparator innerComparator = new EPStatementAgentInstanceHandleComparator();

    public int compare(AgentInstance ai1, AgentInstance ai2) {
        EPStatementAgentInstanceHandle o1 = ai1.getAgentInstanceContext().getEpStatementAgentInstanceHandle();
        EPStatementAgentInstanceHandle o2 = ai2.getAgentInstanceContext().getEpStatementAgentInstanceHandle();
        return innerComparator.compare(o1, o2);
    }
}
