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
package com.espertech.esper.common.internal.context.aifactory.createindex;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactoryResult;
import com.espertech.esper.common.internal.context.airegistry.AIRegistryRequirements;
import com.espertech.esper.common.internal.context.util.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategyFactory;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategyRedoCallback;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;
import com.espertech.esper.common.internal.view.core.Viewable;
import com.espertech.esper.common.internal.view.core.ViewableDefaultImpl;

public class StatementAgentInstanceFactoryCreateIndex implements StatementAgentInstanceFactory {

    private EventType eventType;
    private String indexName;
    private String indexModuleName;
    private QueryPlanIndexItem explicitIndexDesc;
    private IndexMultiKey indexMultiKey;
    private NamedWindow namedWindow;
    private Table table;

    private Viewable viewable;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
        viewable = new ViewableDefaultImpl(eventType);
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public void setIndexModuleName(String indexModuleName) {
        this.indexModuleName = indexModuleName;
    }

    public void setExplicitIndexDesc(QueryPlanIndexItem explicitIndexDesc) {
        this.explicitIndexDesc = explicitIndexDesc;
    }

    public void setIndexMultiKey(IndexMultiKey indexMultiKey) {
        this.indexMultiKey = indexMultiKey;
    }

    public void setNamedWindow(NamedWindow namedWindow) {
        this.namedWindow = namedWindow;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public EventType getStatementEventType() {
        return eventType;
    }

    public void statementCreate(StatementContext statementContext) {
        if (table != null && indexMultiKey.isUnique()) {
            for (TableUpdateStrategyRedoCallback callback : table.getUpdateStrategyCallbacks()) {
                if (callback.isMerge()) {
                    TableUpdateStrategyFactory.validateNewUniqueIndex(callback.getTableUpdatedProperties(), indexMultiKey.getHashIndexedProps());
                }
            }
        }

        try {
            if (namedWindow != null) {
                namedWindow.validateAddIndex(statementContext.getDeploymentId(), statementContext.getStatementName(), indexName, indexModuleName, explicitIndexDesc, indexMultiKey);
            } else {
                table.validateAddIndex(statementContext.getDeploymentId(), statementContext.getStatementName(), indexName, indexModuleName, explicitIndexDesc, indexMultiKey);
            }
        } catch (ExprValidationException ex) {
            throw new EPException(ex.getMessage(), ex);
        }
    }

    public void statementDestroy(StatementContext statementContext) {
        if (namedWindow != null) {
            namedWindow.removeIndexReferencesStmtMayRemoveIndex(indexMultiKey, statementContext.getDeploymentId(), statementContext.getStatementName());
        } else {
            table.removeIndexReferencesStmtMayRemoveIndex(indexMultiKey, statementContext.getDeploymentId(), statementContext.getStatementName());
        }
    }

    public StatementAgentInstanceFactoryResult newContext(AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {
        AgentInstanceStopCallback stopCallback;

        if (namedWindow != null) {
            // handle named window index
            final NamedWindowInstance processorInstance = namedWindow.getNamedWindowInstance(agentInstanceContext);

            if (processorInstance.getRootViewInstance().isVirtualDataWindow()) {
                final VirtualDWView virtualDWView = processorInstance.getRootViewInstance().getVirtualDataWindow();
                virtualDWView.handleStartIndex(indexName, explicitIndexDesc);
                stopCallback = new AgentInstanceStopCallback() {
                    public void stop(AgentInstanceStopServices services) {
                        virtualDWView.handleStopIndex(indexName, explicitIndexDesc);
                    }
                };
            } else {
                try {
                    processorInstance.getRootViewInstance().addExplicitIndex(indexName, indexModuleName, explicitIndexDesc, isRecoveringResilient);
                } catch (ExprValidationException e) {
                    throw new EPException("Failed to create index: " + e.getMessage(), e);
                }

                stopCallback = new AgentInstanceStopCallback() {
                    public void stop(AgentInstanceStopServices services) {
                        NamedWindowInstance instance = namedWindow.getNamedWindowInstance(services.getAgentInstanceContext());
                        if (instance != null) {
                            instance.removeExplicitIndex(indexName);
                        }
                    }
                };
            }
        } else {
            // handle table access
            try {
                TableInstance instance = table.getTableInstance(agentInstanceContext.getAgentInstanceId());
                instance.addExplicitIndex(indexName, indexModuleName, explicitIndexDesc, isRecoveringResilient);
            } catch (ExprValidationException ex) {
                throw new EPException("Failed to create index: " + ex.getMessage(), ex);
            }

            stopCallback = new AgentInstanceStopCallback() {
                public void stop(AgentInstanceStopServices services) {
                    TableInstance instance = table.getTableInstance(services.getAgentInstanceContext().getAgentInstanceId());
                    if (instance != null) {
                        instance.removeExplicitIndex(indexName);
                    }
                }
            };
        }

        return new StatementAgentInstanceFactoryCreateIndexResult(viewable, stopCallback, agentInstanceContext);
    }

    public AIRegistryRequirements getRegistryRequirements() {
        return AIRegistryRequirements.noRequirements();
    }

    public StatementAgentInstanceLock obtainAgentInstanceLock(StatementContext statementContext, int agentInstanceId) {
        return AgentInstanceUtil.newLock(statementContext);
    }
}
