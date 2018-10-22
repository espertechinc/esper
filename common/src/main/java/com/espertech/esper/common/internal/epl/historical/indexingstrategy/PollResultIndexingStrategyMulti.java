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
package com.espertech.esper.common.internal.epl.historical.indexingstrategy;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganizationType;
import com.espertech.esper.common.internal.epl.index.base.MultiIndexEventTable;

import java.util.List;

public class PollResultIndexingStrategyMulti implements PollResultIndexingStrategy {
    private int streamNum;
    private PollResultIndexingStrategy[] indexingStrategies;

    public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, AgentInstanceContext agentInstanceContext) {
        if (!isActiveCache) {
            return new EventTable[]{new UnindexedEventTableList(pollResult, streamNum)};
        }

        EventTable[] tables = new EventTable[indexingStrategies.length];
        for (int i = 0; i < indexingStrategies.length; i++) {
            tables[i] = indexingStrategies[i].index(pollResult, isActiveCache, agentInstanceContext)[0];
        }
        EventTableOrganization organization = new EventTableOrganization(null, false, false, streamNum, null, EventTableOrganizationType.MULTIINDEX);
        return new EventTable[]{new MultiIndexEventTable(tables, organization)};
    }

    public void setStreamNum(int streamNum) {
        this.streamNum = streamNum;
    }

    public void setIndexingStrategies(PollResultIndexingStrategy[] indexingStrategies) {
        this.indexingStrategies = indexingStrategies;
    }
}
