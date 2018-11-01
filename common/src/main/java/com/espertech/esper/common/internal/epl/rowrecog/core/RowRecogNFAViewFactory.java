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
package com.espertech.esper.common.internal.epl.rowrecog.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeMatchRecognize;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAState;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateBase;
import com.espertech.esper.common.internal.epl.rowrecog.nfa.RowRecogNFAStateEndEval;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.View;
import com.espertech.esper.common.internal.view.core.ViewFactory;
import com.espertech.esper.common.internal.view.core.ViewFactoryContext;

/**
 * View factory for match-recognize view.
 */
public class RowRecogNFAViewFactory implements ViewFactory {

    private RowRecogDesc desc;

    private boolean trackMaxStates;
    private RowRecogNFAState[] startStates;
    private RowRecogNFAState[] allStates;
    protected DataInputOutputSerde<Object> partitionKeySerde;
    protected int scheduleCallbackId;

    public void setDesc(RowRecogDesc desc) {
        this.desc = desc;
    }

    public void setScheduleCallbackId(int scheduleCallbackId) {
        this.scheduleCallbackId = scheduleCallbackId;
    }

    public RowRecogDesc getDesc() {
        return desc;
    }

    public void setEventType(EventType eventType) {
        // ignored
    }

    public EventType getEventType() {
        return desc.getRowEventType();
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
        ConfigurationRuntimeMatchRecognize matchRecognize = services.getRuntimeSettingsService().getConfigurationRuntime().getMatchRecognize();
        this.trackMaxStates = matchRecognize != null && matchRecognize.getMaxStates() != null;

        // build start states
        this.startStates = new RowRecogNFAState[desc.getStartStates().length];
        for (int i = 0; i < desc.getStartStates().length; i++) {
            this.startStates[i] = desc.getStatesOrdered()[desc.getStartStates()[i]];
        }

        // build all states and state links
        for (Pair<Integer, int[]> stateLink : desc.getNextStatesPerState()) {
            RowRecogNFAStateBase state = desc.getStatesOrdered()[stateLink.getFirst()];
            RowRecogNFAState[] nextStates = new RowRecogNFAState[stateLink.getSecond().length];
            state.setNextStates(nextStates);
            for (int i = 0; i < stateLink.getSecond().length; i++) {
                int nextNum = stateLink.getSecond()[i];
                RowRecogNFAState nextState;
                if (nextNum == -1) {
                    nextState = new RowRecogNFAStateEndEval();
                } else {
                    nextState = desc.getStatesOrdered()[nextNum];
                }
                nextStates[i] = nextState;
            }
        }
        this.allStates = desc.getStatesOrdered();
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {

        RowRecogNFAViewScheduler scheduler = null;
        if (desc.isHasInterval()) {
            scheduler = new RowRecogNFAViewSchedulerImpl();
        }

        RowRecogNFAView view = new RowRecogNFAView(this,
                agentInstanceViewFactoryContext.getAgentInstanceContext(),
                scheduler);

        if (scheduler != null) {
            scheduler.setScheduleCallback(agentInstanceViewFactoryContext.getAgentInstanceContext(), view);
        }

        return view;
    }

    public boolean isTrackMaxStates() {
        return trackMaxStates;
    }

    public RowRecogNFAState[] getStartStates() {
        return startStates;
    }

    public RowRecogNFAState[] getAllStates() {
        return allStates;
    }

    public DataInputOutputSerde<Object> getPartitionKeySerde() {
        return partitionKeySerde;
    }

    public int getScheduleCallbackId() {
        return scheduleCallbackId;
    }

    public String getViewName() {
        return "rowrecog";
    }
}
