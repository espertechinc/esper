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
package com.espertech.esper.epl.index.service;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.table.*;
import com.espertech.esper.epl.lookup.AdvancedIndexConfigContextPartition;

public class EventTableFactoryCustomIndex implements EventTableFactory {
    protected final EventType eventType;
    protected final EventAdvancedIndexProvisionDesc advancedIndexProvisionDesc;
    protected final EventTableOrganization organization;

    public EventTableFactoryCustomIndex(String indexName, int indexedStreamNum, EventType eventType, boolean unique, EventAdvancedIndexProvisionDesc advancedIndexProvisionDesc) {
        this.eventType = eventType;
        this.advancedIndexProvisionDesc = advancedIndexProvisionDesc;
        String[] expressions = ExprNodeUtilityCore.toExpressionStringMinPrecedenceAsArray(advancedIndexProvisionDesc.getIndexDesc().getIndexedExpressions());
        this.organization = new EventTableOrganization(indexName, unique, false, indexedStreamNum, expressions, EventTableOrganizationType.APPLICATION);
    }

    public Class getEventTableClass() {
        return EventTable.class;
    }

    public EventTable[] makeEventTables(EventTableFactoryTableIdent tableIdent, ExprEvaluatorContext exprEvaluatorContext) {
        AdvancedIndexConfigContextPartition configCP = advancedIndexProvisionDesc.getFactory().configureContextPartition(eventType, advancedIndexProvisionDesc.getIndexDesc(), advancedIndexProvisionDesc.getParameters(), exprEvaluatorContext, organization, advancedIndexProvisionDesc.getConfigStatement());
        EventTable eventTable = advancedIndexProvisionDesc.getFactory().make(advancedIndexProvisionDesc.getConfigStatement(), configCP, organization);
        return new EventTable[]{eventTable};
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " streamNum=" + organization.getStreamNum() +
                " indexName=" + organization.getIndexName();
    }
}
