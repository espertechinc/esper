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
package com.espertech.esper.common.internal.epl.lookup;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionRuntime;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;

public interface EventAdvancedIndexFactory {
    EventAdvancedIndexFactoryForge getForge();

    AdvancedIndexConfigContextPartition configureContextPartition(AgentInstanceContext agentInstanceContext, EventType eventType, EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc, EventTableOrganization organization);

    EventTable make(EventAdvancedIndexConfigStatement configStatement, AdvancedIndexConfigContextPartition configContextPartition, EventTableOrganization organization);

    EventAdvancedIndexConfigStatementForge toConfigStatement(ExprNode[] indexedExpr);
}

