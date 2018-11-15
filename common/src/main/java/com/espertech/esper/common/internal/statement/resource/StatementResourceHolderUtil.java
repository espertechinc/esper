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
package com.espertech.esper.common.internal.statement.resource;

import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.aifactory.createcontext.StatementAgentInstanceFactoryCreateContextResult;
import com.espertech.esper.common.internal.context.aifactory.createtable.StatementAgentInstanceFactoryCreateTableResult;
import com.espertech.esper.common.internal.context.aifactory.createwindow.StatementAgentInstanceFactoryCreateNWResult;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.StatementAgentInstanceFactoryOnTriggerResult;
import com.espertech.esper.common.internal.context.aifactory.select.StatementAgentInstanceFactorySelectResult;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootState;
import com.espertech.esper.common.internal.view.core.Viewable;

public class StatementResourceHolderUtil {
    public static StatementResourceHolder populateHolder(AgentInstanceContext agentInstanceContext, StatementAgentInstanceFactoryResult startResult) {
        StatementResourceHolder holder = new StatementResourceHolder(agentInstanceContext, startResult.getStopCallback(), startResult.getFinalView(), startResult.getOptionalAggegationService(), startResult.getPriorStrategies(), startResult.getPreviousGetterStrategies(), startResult.getRowRecogPreviousStrategy());
        holder.setSubselectStrategies(startResult.getSubselectStrategies());
        holder.setTableAccessStrategies(startResult.getTableAccessStrategies());

        if (startResult instanceof StatementAgentInstanceFactorySelectResult) {
            StatementAgentInstanceFactorySelectResult selectResult = (StatementAgentInstanceFactorySelectResult) startResult;
            holder.setTopViewables(selectResult.getTopViews());
            holder.setEventStreamViewables(selectResult.getEventStreamViewables());
            holder.setPatternRoots(selectResult.getPatternRoots());
            holder.setAggregationService(selectResult.getOptionalAggegationService());
            holder.setJoinSetComposer(selectResult.getJoinSetComposer());
        } else if (startResult instanceof StatementAgentInstanceFactoryCreateContextResult) {
            StatementAgentInstanceFactoryCreateContextResult createResult = (StatementAgentInstanceFactoryCreateContextResult) startResult;
            holder.setContextManagerRealization(createResult.getContextManagerRealization());
        } else if (startResult instanceof StatementAgentInstanceFactoryCreateNWResult) {
            StatementAgentInstanceFactoryCreateNWResult createResult = (StatementAgentInstanceFactoryCreateNWResult) startResult;
            holder.setTopViewables(new Viewable[]{createResult.getTopView()});
            holder.setNamedWindowInstance(createResult.getNamedWindowInstance());
        } else if (startResult instanceof StatementAgentInstanceFactoryCreateTableResult) {
            StatementAgentInstanceFactoryCreateTableResult createResult = (StatementAgentInstanceFactoryCreateTableResult) startResult;
            holder.setTopViewables(new Viewable[]{createResult.getFinalView()});
            holder.setTableInstance(createResult.getTableInstance());
        } else if (startResult instanceof StatementAgentInstanceFactoryOnTriggerResult) {
            StatementAgentInstanceFactoryOnTriggerResult onResult = (StatementAgentInstanceFactoryOnTriggerResult) startResult;
            if (onResult.getOptPatternRoot() != null) {
                holder.setPatternRoots(new EvalRootState[] {onResult.getOptPatternRoot()});
            }
        }
        return holder;
    }
}
