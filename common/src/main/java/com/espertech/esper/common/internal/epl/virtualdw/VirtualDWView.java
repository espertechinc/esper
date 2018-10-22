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
package com.espertech.esper.common.internal.epl.virtualdw;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindow;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.exec.util.RangeIndexLookupValue;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupPlan;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;

import java.lang.annotation.Annotation;
import java.util.Collection;

public interface VirtualDWView {
    VirtualDataWindow getVirtualDataWindow();

    EventType getEventType();

    SubordTableLookupStrategy getSubordinateLookupStrategy(SubordTableLookupStrategyFactoryVDW subordTableFactory, AgentInstanceContext agentInstanceContext);

    JoinExecTableLookupStrategy getJoinLookupStrategy(TableLookupPlan tableLookupPlan, AgentInstanceContext agentInstanceContext, EventTable[] eventTables, int lookupStream);

    Collection<EventBean> getFireAndForgetData(EventTable eventTable, Object[] keyValues, RangeIndexLookupValue[] rangeValues, Annotation[] annotations);

    void handleStopIndex(String indexName, QueryPlanIndexItem explicitIndexDesc);

    void handleStartIndex(String indexName, QueryPlanIndexItem explicitIndexDesc);

    void handleDestroy(int agentInstanceId);

    void destroy();
}
