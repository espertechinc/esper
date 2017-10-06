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
package com.espertech.esper.epl.core.resultset.rowpergrouprollup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupDesc;
import com.espertech.esper.epl.agg.service.common.AggregationGroupByRollupLevel;
import com.espertech.esper.epl.core.orderby.OrderByProcessor;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.epl.core.resultset.grouped.ResultSetProcessorGroupedOutputFirstHelper;
import com.espertech.esper.epl.view.OutputConditionPolledFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResultSetProcessorRowPerGroupRollupUtil {

    final static String METHOD_MAKEGROUPREPSPERLEVELBUF = "makeGroupRepsPerLevelBuf";
    final static String METHOD_MAKERSTREAMSORTEDARRAYBUF = "makeRStreamSortedArrayBuf";
    final static String METHOD_GETOLDEVENTSSORTKEYS = "getOldEventsSortKeys";

    public static EventsAndSortKeysPair getOldEventsSortKeys(int oldEventCount, EventArrayAndSortKeyArray rstreamEventSortArrayBuf, OrderByProcessor orderByProcessor, AggregationGroupByRollupDesc rollupDesc) {
        EventBean[] oldEventsArr = new EventBean[oldEventCount];
        Object[] oldEventsSortKeys = null;
        if (orderByProcessor != null) {
            oldEventsSortKeys = new Object[oldEventCount];
        }
        int countEvents = 0;
        int countSortKeys = 0;
        for (AggregationGroupByRollupLevel level : rollupDesc.getLevels()) {
            List<EventBean> events = rstreamEventSortArrayBuf.getEventsPerLevel()[level.getLevelNumber()];
            for (EventBean event : events) {
                oldEventsArr[countEvents++] = event;
            }
            if (orderByProcessor != null) {
                List<Object> sortKeys = rstreamEventSortArrayBuf.getSortKeyPerLevel()[level.getLevelNumber()];
                for (Object sortKey : sortKeys) {
                    oldEventsSortKeys[countSortKeys++] = sortKey;
                }
            }
        }
        return new EventsAndSortKeysPair(oldEventsArr, oldEventsSortKeys);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param length    num-levels
     * @param isSorting sorting flag
     * @return buffer
     */
    public static EventArrayAndSortKeyArray makeRStreamSortedArrayBuf(int length, boolean isSorting) {
        List<EventBean>[] eventsPerLevel = (List<EventBean>[]) new List[length];
        List<Object>[] sortKeyPerLevel = null;
        if (isSorting) {
            sortKeyPerLevel = (List<Object>[]) new List[length];
        }
        for (int i = 0; i < length; i++) {
            eventsPerLevel[i] = new ArrayList<>();
            if (isSorting) {
                sortKeyPerLevel[i] = new ArrayList<>();
            }
        }
        return new EventArrayAndSortKeyArray(eventsPerLevel, sortKeyPerLevel);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param levelCount num-levels
     * @return buffer
     */
    public static Map<Object, EventBean[]>[] makeGroupRepsPerLevelBuf(int levelCount) {
        Map<Object, EventBean[]>[] groupRepsPerLevelBuf = (LinkedHashMap<Object, EventBean[]>[]) new LinkedHashMap[levelCount];
        for (int i = 0; i < levelCount; i++) {
            groupRepsPerLevelBuf[i] = new LinkedHashMap<>();
        }
        return groupRepsPerLevelBuf;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param resultSetProcessorHelperFactory helper factory
     * @param agentInstanceContext            context
     * @param groupKeyTypes                   types
     * @param groupByRollupDesc               rollup into
     * @param outputConditionPolledFactory    condition factory
     * @return helpers
     */
    public static ResultSetProcessorGroupedOutputFirstHelper[] initializeOutputFirstHelpers(ResultSetProcessorHelperFactory resultSetProcessorHelperFactory, AgentInstanceContext agentInstanceContext, Class[] groupKeyTypes, AggregationGroupByRollupDesc groupByRollupDesc, OutputConditionPolledFactory outputConditionPolledFactory) {
        ResultSetProcessorGroupedOutputFirstHelper[] outputFirstHelpers = new ResultSetProcessorGroupedOutputFirstHelper[groupByRollupDesc.getLevels().length];
        for (int i = 0; i < groupByRollupDesc.getLevels().length; i++) {
            outputFirstHelpers[i] = resultSetProcessorHelperFactory.makeRSGroupedOutputFirst(agentInstanceContext, groupKeyTypes, outputConditionPolledFactory, groupByRollupDesc, i);
        }
        return outputFirstHelpers;
    }
}
