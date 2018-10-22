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

import java.util.List;

/**
 * Strategy of indexing that simply builds an unindexed table of poll results.
 * <p>
 * For use when caching is disabled or when no proper index could be build because no where-clause or on-clause exists or
 * these clauses don't yield indexable columns on analysis.
 */
public class PollResultIndexingStrategyNoIndex implements PollResultIndexingStrategy {

    public final static PollResultIndexingStrategyNoIndex INSTANCE = new PollResultIndexingStrategyNoIndex();

    private PollResultIndexingStrategyNoIndex() {
    }

    public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, AgentInstanceContext agentInstanceContext) {
        return new EventTable[]{new UnindexedEventTableList(pollResult, -1)};
    }
}
