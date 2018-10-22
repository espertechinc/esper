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
package com.espertech.esper.common.internal.epl.join.base;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalDataQueryStrategy;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewable;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategy;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategyNoIndex;
import com.espertech.esper.common.internal.epl.historical.lookupstrategy.HistoricalIndexLookupStrategy;
import com.espertech.esper.common.internal.epl.historical.lookupstrategy.HistoricalIndexLookupStrategyNoIndex;
import com.espertech.esper.common.internal.epl.join.strategy.QueryStrategy;
import com.espertech.esper.common.internal.view.core.Viewable;

public class JoinSetComposerPrototypeHistorical2Stream extends JoinSetComposerPrototypeBase {

    private int polledNum;
    private int streamNum;
    private ExprEvaluator outerJoinEqualsEval;
    private HistoricalIndexLookupStrategy lookupStrategy;
    private PollResultIndexingStrategy indexingStrategy;
    private boolean isAllHistoricalNoSubordinate;
    private boolean[] outerJoinPerStream;

    public JoinSetComposerDesc create(Viewable[] streamViews, boolean isFireAndForget, AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        QueryStrategy[] queryStrategies = new QueryStrategy[streamTypes.length];

        HistoricalEventViewable viewable = (HistoricalEventViewable) streamViews[polledNum];
        queryStrategies[streamNum] = new HistoricalDataQueryStrategy(streamNum, polledNum, viewable, outerJoinPerStream[streamNum], outerJoinEqualsEval,
                lookupStrategy, indexingStrategy);

        // for strictly historical joins, create a query strategy for the non-subordinate historical view
        if (isAllHistoricalNoSubordinate) {
            viewable = (HistoricalEventViewable) streamViews[streamNum];
            queryStrategies[polledNum] = new HistoricalDataQueryStrategy(polledNum, streamNum, viewable, outerJoinPerStream[polledNum], outerJoinEqualsEval,
                    HistoricalIndexLookupStrategyNoIndex.INSTANCE, PollResultIndexingStrategyNoIndex.INSTANCE);
        }

        boolean allowIndexInit = agentInstanceContext.getEventTableIndexService().allowInitIndex(isRecoveringResilient);
        JoinSetComposer composer = new JoinSetComposerHistoricalImpl(allowIndexInit, null, queryStrategies, streamViews, agentInstanceContext);
        return new JoinSetComposerDesc(composer, postJoinFilterEvaluator);
    }

    public void setPolledNum(int polledNum) {
        this.polledNum = polledNum;
    }

    public void setStreamNum(int streamNum) {
        this.streamNum = streamNum;
    }

    public void setOuterJoinEqualsEval(ExprEvaluator outerJoinEqualsEval) {
        this.outerJoinEqualsEval = outerJoinEqualsEval;
    }

    public void setLookupStrategy(HistoricalIndexLookupStrategy lookupStrategy) {
        this.lookupStrategy = lookupStrategy;
    }

    public void setIndexingStrategy(PollResultIndexingStrategy indexingStrategy) {
        this.indexingStrategy = indexingStrategy;
    }

    public void setAllHistoricalNoSubordinate(boolean allHistoricalNoSubordinate) {
        isAllHistoricalNoSubordinate = allHistoricalNoSubordinate;
    }

    public void setOuterJoinPerStream(boolean[] outerJoinPerStream) {
        this.outerJoinPerStream = outerJoinPerStream;
    }
}
