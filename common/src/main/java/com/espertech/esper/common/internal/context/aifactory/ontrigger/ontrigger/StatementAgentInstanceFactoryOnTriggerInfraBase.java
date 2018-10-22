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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.ontrigger;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryUtil;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.StatementAgentInstanceFactoryOnTriggerBase;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordWMatchExprLookupStrategy;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordinateQueryPlannerUtil;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordinateWMatchExprQueryPlan;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.epl.ontrigger.InfraOnExprBaseViewFactory;
import com.espertech.esper.common.internal.epl.ontrigger.InfraOnExprBaseViewResult;
import com.espertech.esper.common.internal.epl.output.core.OutputProcessViewSimpleWProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;
import com.espertech.esper.common.internal.view.core.View;

import java.util.List;

public abstract class StatementAgentInstanceFactoryOnTriggerInfraBase extends StatementAgentInstanceFactoryOnTriggerBase implements StatementReadyCallback {
    private NamedWindow namedWindow;
    private Table table;
    private SubordinateWMatchExprQueryPlan queryPlan;
    private InfraOnExprBaseViewFactory factory;
    private ResultSetProcessorFactoryProvider nonSelectRSPFactoryProvider;

    protected abstract InfraOnExprBaseViewFactory setupFactory(EventType infraEventType, NamedWindow namedWindow, Table table, StatementContext statementContext);

    protected abstract boolean isSelect();

    public void setNamedWindow(NamedWindow namedWindow) {
        this.namedWindow = namedWindow;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public void setQueryPlan(SubordinateWMatchExprQueryPlan queryPlan) {
        this.queryPlan = queryPlan;
    }

    public void setNonSelectRSPFactoryProvider(ResultSetProcessorFactoryProvider nonSelectRSPFactoryProvider) {
        this.nonSelectRSPFactoryProvider = nonSelectRSPFactoryProvider;
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        EventType infraEventType = namedWindow != null ? namedWindow.getRootView().getEventType() : table.getMetaData().getInternalEventType();
        factory = setupFactory(infraEventType, namedWindow, table, statementContext);

        if (queryPlan.getIndexDescs() != null) {
            EventTableIndexMetadata indexInfo = namedWindow != null ? namedWindow.getEventTableIndexMetadata() : table.getEventTableIndexMetadata();
            SubordinateQueryPlannerUtil.addIndexMetaAndRef(queryPlan.getIndexDescs(), indexInfo, statementContext.getDeploymentId(), statementContext.getStatementName());

            statementContext.addFinalizeCallback(new StatementFinalizeCallback() {
                public void statementDestroyed(StatementContext context) {
                    for (int i = 0; i < queryPlan.getIndexDescs().length; i++) {
                        if (namedWindow != null) {
                            boolean last = namedWindow.getEventTableIndexMetadata().removeIndexReference(queryPlan.getIndexDescs()[i].getIndexMultiKey(), statementContext.getDeploymentId());
                            if (last) {
                                namedWindow.getEventTableIndexMetadata().removeIndex(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                                namedWindow.removeAllInstanceIndexes(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                            }
                        } else {
                            boolean last = table.getEventTableIndexMetadata().removeIndexReference(queryPlan.getIndexDescs()[i].getIndexMultiKey(), statementContext.getDeploymentId());
                            if (last) {
                                table.getEventTableIndexMetadata().removeIndex(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                                table.removeAllInstanceIndexes(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                            }
                        }
                    }
                }
            });
        }
    }

    public InfraOnExprBaseViewResult determineOnExprView(AgentInstanceContext agentInstanceContext, List<AgentInstanceStopCallback> stopCallbacks, boolean isRecoveringResilient) {

        // get instance
        NamedWindowInstance namedWindowInstance = null;
        TableInstance tableInstance = null;
        if (namedWindow != null) {
            namedWindowInstance = namedWindow.getNamedWindowInstance(agentInstanceContext);
            if (namedWindowInstance == null) {
                throw new RuntimeException("Failed to obtain named window instance for named window '" + namedWindow.getName() + "'");
            }
        } else {
            tableInstance = table.getTableInstance(agentInstanceContext.getAgentInstanceId());
            if (tableInstance == null) {
                throw new RuntimeException("Failed to obtain table instance for table '" + table.getName() + "'");
            }
        }

        // obtain index
        EventTable[] indexes = null;
        if (queryPlan.getIndexDescs() != null) {
            if (namedWindow != null) {
                indexes = SubordinateQueryPlannerUtil.realizeTables(queryPlan.getIndexDescs(),
                        namedWindowInstance.getRootViewInstance().getEventType(),
                        namedWindowInstance.getRootViewInstance().getIndexRepository(),
                        namedWindowInstance.getRootViewInstance().getDataWindowContents(),
                        namedWindowInstance.getTailViewInstance().getAgentInstanceContext(),
                        isRecoveringResilient);

                stopCallbacks.add(new AgentInstanceStopCallback() {
                    public void stop(AgentInstanceStopServices services) {
                        for (int i = 0; i < queryPlan.getIndexDescs().length; i++) {
                            boolean last = namedWindow.getEventTableIndexMetadata().removeIndexReference(queryPlan.getIndexDescs()[i].getIndexMultiKey(), agentInstanceContext.getDeploymentId());
                            if (last) {
                                namedWindow.getEventTableIndexMetadata().removeIndex(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                                namedWindow.removeAllInstanceIndexes(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                            }
                        }
                    }
                });
            } else {
                indexes = new EventTable[queryPlan.getIndexDescs().length];
                for (int i = 0; i < indexes.length; i++) {
                    indexes[i] = tableInstance.getIndexRepository().getIndexByDesc(queryPlan.getIndexDescs()[i].getIndexMultiKey());
                }
            }
        }

        // realize lookup strategy
        Iterable<EventBean> scanIterable = namedWindow != null ? namedWindowInstance.getRootViewInstance().getDataWindowContents() : tableInstance.getIterableTableScan();
        VirtualDWView virtualDW = namedWindow != null ? namedWindowInstance.getRootViewInstance().getVirtualDataWindow() : null;
        SubordWMatchExprLookupStrategy lookupStrategy = queryPlan.getFactory().realize(indexes, agentInstanceContext, scanIterable, virtualDW);

        // realize view
        if (namedWindow != null) {
            return factory.makeNamedWindow(lookupStrategy, namedWindowInstance.getRootViewInstance(), agentInstanceContext);
        }
        return factory.makeTable(lookupStrategy, tableInstance, agentInstanceContext);
    }

    public View determineFinalOutputView(AgentInstanceContext agentInstanceContext, View onExprView) {
        if (!isSelect()) {
            Pair<ResultSetProcessor, AggregationService> pair = StatementAgentInstanceFactoryUtil.startResultSetAndAggregation(nonSelectRSPFactoryProvider, agentInstanceContext, false, null);
            OutputProcessViewSimpleWProcessor out = new OutputProcessViewSimpleWProcessor(agentInstanceContext, pair.getFirst());
            out.setParent(onExprView);
            onExprView.setChild(out);
            return out;
        } else {
            return onExprView;
        }
    }

    public NamedWindow getNamedWindow() {
        return namedWindow;
    }

    public Table getTable() {
        return table;
    }
}
