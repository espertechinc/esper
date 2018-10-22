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
package com.espertech.esper.common.internal.epl.index.advanced.index.quadtree;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionRuntime;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexConfigContextPartition;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexFactory;

public abstract class EventAdvancedIndexFactoryForgeQuadTreeFactory implements EventAdvancedIndexFactory {

    public AdvancedIndexConfigContextPartition configureContextPartition(AgentInstanceContext agentInstanceContext, EventType eventType, EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc, EventTableOrganization organization) {
        return AdvancedIndexFactoryProviderQuadTree.configureQuadTree(organization.getIndexName(), advancedIndexProvisionDesc.getParameterEvaluators(), agentInstanceContext);
    }
}
