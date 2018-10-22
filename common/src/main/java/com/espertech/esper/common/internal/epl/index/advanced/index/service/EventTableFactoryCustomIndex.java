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
package com.espertech.esper.common.internal.epl.index.advanced.index.service;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactory;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganizationType;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexConfigContextPartition;

public class EventTableFactoryCustomIndex implements EventTableFactory {
    protected final EventType eventType;
    protected final EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc;
    protected final EventTableOrganization organization;

    public EventTableFactoryCustomIndex(String indexName, int indexedStreamNum, EventType eventType, boolean unique, EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc) {
        this.eventType = eventType;
        this.advancedIndexProvisionDesc = advancedIndexProvisionDesc;
        this.organization = new EventTableOrganization(indexName, unique, false, indexedStreamNum, advancedIndexProvisionDesc.getIndexExpressionTexts(), EventTableOrganizationType.APPLICATION);
    }

    public Class getEventTableClass() {
        return EventTable.class;
    }

    public EventTable[] makeEventTables(AgentInstanceContext agentInstanceContext, Integer subqueryNumber) {
        AdvancedIndexConfigContextPartition configCP = advancedIndexProvisionDesc.getFactory().configureContextPartition(agentInstanceContext, eventType, advancedIndexProvisionDesc, organization);
        EventTable eventTable = advancedIndexProvisionDesc.getFactory().make(advancedIndexProvisionDesc.getConfigStatement(), configCP, organization);
        return new EventTable[]{eventTable};
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " streamNum=" + organization.getStreamNum() +
                " indexName=" + organization.getIndexName();
    }
}
