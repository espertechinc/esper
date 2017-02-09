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
package com.espertech.esper.core.service.resource;

import com.espertech.esper.core.context.factory.*;
import com.espertech.esper.pattern.EvalRootState;
import com.espertech.esper.view.Viewable;

public class StatementResourceHolderUtil {
    public static StatementResourceHolder populateHolder(StatementAgentInstanceFactoryResult startResult) {
        StatementResourceHolder holder = new StatementResourceHolder(startResult.getAgentInstanceContext());

        if (startResult instanceof StatementAgentInstanceFactorySelectResult) {
            StatementAgentInstanceFactorySelectResult selectResult = (StatementAgentInstanceFactorySelectResult) startResult;
            holder.setTopViewables(selectResult.getTopViews());
            holder.setEventStreamViewables(selectResult.getEventStreamViewables());
            holder.setPatternRoots(selectResult.getPatternRoots());
            holder.setAggregationService(selectResult.getOptionalAggegationService());
            holder.setSubselectStrategies(selectResult.getSubselectStrategies());
            holder.setPostLoad(selectResult.getOptionalPostLoadJoin());
        } else if (startResult instanceof StatementAgentInstanceFactoryCreateWindowResult) {
            StatementAgentInstanceFactoryCreateWindowResult createResult = (StatementAgentInstanceFactoryCreateWindowResult) startResult;
            holder.setTopViewables(new Viewable[]{createResult.getTopView()});
            holder.setPostLoad(createResult.getPostLoad());
            holder.setNamedWindowProcessorInstance(createResult.getProcessorInstance());
        } else if (startResult instanceof StatementAgentInstanceFactoryCreateTableResult) {
            StatementAgentInstanceFactoryCreateTableResult createResult = (StatementAgentInstanceFactoryCreateTableResult) startResult;
            holder.setTopViewables(new Viewable[]{createResult.getFinalView()});
            holder.setAggregationService(createResult.getOptionalAggegationService());
        } else if (startResult instanceof StatementAgentInstanceFactoryOnTriggerResult) {
            StatementAgentInstanceFactoryOnTriggerResult onTriggerResult = (StatementAgentInstanceFactoryOnTriggerResult) startResult;
            holder.setPatternRoots(new EvalRootState[]{onTriggerResult.getOptPatternRoot()});
            holder.setAggregationService(onTriggerResult.getOptionalAggegationService());
            holder.setSubselectStrategies(onTriggerResult.getSubselectStrategies());
        }
        return holder;
    }
}
