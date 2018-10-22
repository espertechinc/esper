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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultRuntime;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewable;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableIndexService;
import com.espertech.esper.common.internal.epl.index.base.EventTableUtil;
import com.espertech.esper.common.internal.epl.join.queryplan.*;
import com.espertech.esper.common.internal.epl.join.strategy.ExecNode;
import com.espertech.esper.common.internal.epl.join.strategy.ExecNodeQueryStrategy;
import com.espertech.esper.common.internal.epl.join.strategy.QueryStrategy;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowInstance;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWQueryPlanUtil;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;
import com.espertech.esper.common.internal.view.core.DerivedValueView;
import com.espertech.esper.common.internal.view.core.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;

public class JoinSetComposerPrototypeGeneral extends JoinSetComposerPrototypeBase {

    private static final Logger log = LoggerFactory.getLogger(JoinSetComposerPrototypeGeneral.class);

    private StreamJoinAnalysisResultRuntime streamJoinAnalysisResult;
    private String[] streamNames;
    private QueryPlan queryPlan;
    private boolean joinRemoveStream;
    private EventTableIndexService eventTableIndexService;
    private boolean hasHistorical;

    public void setStreamJoinAnalysisResult(StreamJoinAnalysisResultRuntime streamJoinAnalysisResult) {
        this.streamJoinAnalysisResult = streamJoinAnalysisResult;
    }

    public void setStreamNames(String[] streamNames) {
        this.streamNames = streamNames;
    }

    public void setQueryPlan(QueryPlan queryPlan) {
        this.queryPlan = queryPlan;
    }

    public void setJoinRemoveStream(boolean joinRemoveStream) {
        this.joinRemoveStream = joinRemoveStream;
    }

    public void setEventTableIndexService(EventTableIndexService eventTableIndexService) {
        this.eventTableIndexService = eventTableIndexService;
    }

    public void setHasHistorical(boolean hasHistorical) {
        this.hasHistorical = hasHistorical;
    }

    public JoinSetComposerDesc create(Viewable[] streamViews, boolean isFireAndForget, AgentInstanceContext agentInstanceContext, boolean isRecoveringResilient) {

        // Build indexes
        QueryPlanIndex[] indexSpecs = queryPlan.getIndexSpecs();
        Map<TableLookupIndexReqKey, EventTable>[] indexesPerStream = new HashMap[indexSpecs.length];
        Lock[] tableSecondaryIndexLocks = new Lock[indexSpecs.length];
        boolean hasTable = false;
        for (int streamNo = 0; streamNo < indexSpecs.length; streamNo++) {
            if (indexSpecs[streamNo] == null) {
                continue;
            }

            Map<TableLookupIndexReqKey, QueryPlanIndexItem> items = indexSpecs[streamNo].getItems();
            indexesPerStream[streamNo] = new LinkedHashMap<TableLookupIndexReqKey, EventTable>();

            if (streamJoinAnalysisResult.getTables()[streamNo] != null) {
                // build for tables
                Table table = streamJoinAnalysisResult.getTables()[streamNo];
                TableInstance state = table.getTableInstance(agentInstanceContext.getAgentInstanceId());
                for (String indexName : state.getIndexRepository().getExplicitIndexNames()) { // add secondary indexes
                    EventTable index = state.getIndex(indexName);
                    indexesPerStream[streamNo].put(new TableLookupIndexReqKey(indexName, null, table.getName()), index);
                }
                EventTable index = state.getIndex(table.getName()); // add primary index
                indexesPerStream[streamNo].put(new TableLookupIndexReqKey(table.getName(), null, table.getName()), index);
                hasTable = true;
                tableSecondaryIndexLocks[streamNo] = agentInstanceContext.getStatementContext().getStatementInformationals().isWritesToTables() ?
                        state.getTableLevelRWLock().writeLock() : state.getTableLevelRWLock().readLock();
            } else {
                // build tables for implicit indexes
                for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem> entry : items.entrySet()) {
                    EventTable index;

                    VirtualDWView virtualDWView = getNamedWindowVirtualDataWindow(streamNo, streamJoinAnalysisResult, agentInstanceContext);
                    if (virtualDWView != null) {
                        index = VirtualDWQueryPlanUtil.getJoinIndexTable(items.get(entry.getKey()));
                    } else {
                        index = EventTableUtil.buildIndex(agentInstanceContext, streamNo, items.get(entry.getKey()), streamTypes[streamNo], false, entry.getValue().isUnique(), null, null, isFireAndForget);
                    }
                    indexesPerStream[streamNo].put(entry.getKey(), index);
                }
            }
        }

        // obtain any external views
        VirtualDWView[] externalViews = new VirtualDWView[indexSpecs.length];
        for (int i = 0; i < externalViews.length; i++) {
            externalViews[i] = getNamedWindowVirtualDataWindow(i, streamJoinAnalysisResult, agentInstanceContext);
        }

        // Build strategies
        QueryPlanNode[] queryExecSpecs = queryPlan.getExecNodeSpecs();
        QueryStrategy[] queryStrategies = new QueryStrategy[queryExecSpecs.length];
        for (int i = 0; i < queryExecSpecs.length; i++) {
            QueryPlanNode planNode = queryExecSpecs[i];
            if (planNode == null) {
                log.debug(".makeComposer No execution node for stream " + i + " '" + streamNames[i] + "'");
                continue;
            }

            ExecNode executionNode = planNode.makeExec(agentInstanceContext, indexesPerStream, streamTypes, streamViews, externalViews, tableSecondaryIndexLocks);

            if (log.isDebugEnabled()) {
                log.debug(".makeComposer Execution nodes for stream " + i + " '" + streamNames[i] +
                        "' : \n" + ExecNode.print(executionNode));
            }

            queryStrategies[i] = new ExecNodeQueryStrategy(i, streamTypes.length, executionNode);
        }

        // Remove indexes that are from tables as these are only available to query strategies
        if (hasTable) {
            indexesPerStream = removeTableIndexes(indexesPerStream, streamJoinAnalysisResult.getTables());
        }

        // If this is not unidirectional and not a self-join (excluding self-outer-join)
        JoinSetComposerDesc joinSetComposerDesc;
        if (JoinSetComposerUtil.isNonUnidirectionalNonSelf(isOuterJoins, streamJoinAnalysisResult.isUnidirectional(), streamJoinAnalysisResult.isPureSelfJoin())) {
            JoinSetComposer composer;
            if (hasHistorical) {
                composer = new JoinSetComposerHistoricalImpl(eventTableIndexService.allowInitIndex(isRecoveringResilient), indexesPerStream, queryStrategies, streamViews, agentInstanceContext);
            } else {
                if (isFireAndForget) {
                    composer = new JoinSetComposerFAFImpl(indexesPerStream, queryStrategies, streamJoinAnalysisResult.isPureSelfJoin(), agentInstanceContext, joinRemoveStream, isOuterJoins);
                } else {
                    composer = new JoinSetComposerImpl(eventTableIndexService.allowInitIndex(isRecoveringResilient), indexesPerStream, queryStrategies, streamJoinAnalysisResult.isPureSelfJoin(), agentInstanceContext, joinRemoveStream);
                }
            }

            // rewrite the filter expression for all-inner joins in case "on"-clause outer join syntax was used to include those expressions
            joinSetComposerDesc = new JoinSetComposerDesc(composer, postJoinFilterEvaluator);
        } else {
            if (streamJoinAnalysisResult.isUnidirectionalAll()) {
                JoinSetComposer composer = new JoinSetComposerAllUnidirectionalOuter(queryStrategies);
                joinSetComposerDesc = new JoinSetComposerDesc(composer, postJoinFilterEvaluator);
            } else {
                QueryStrategy driver;
                int unidirectionalStream;
                if (streamJoinAnalysisResult.isUnidirectional()) {
                    unidirectionalStream = streamJoinAnalysisResult.getUnidirectionalStreamNumberFirst();
                    driver = queryStrategies[unidirectionalStream];
                } else {
                    unidirectionalStream = 0;
                    driver = queryStrategies[0];
                }

                JoinSetComposer composer = new JoinSetComposerStreamToWinImpl(!isRecoveringResilient, indexesPerStream, streamJoinAnalysisResult.isPureSelfJoin(),
                        unidirectionalStream, driver, streamJoinAnalysisResult.getUnidirectionalNonDriving());
                joinSetComposerDesc = new JoinSetComposerDesc(composer, postJoinFilterEvaluator);
            }
        }

        // init if the join-set-composer allows it
        if (joinSetComposerDesc.getJoinSetComposer().allowsInit()) {

            // compile prior events per stream to preload any indexes
            EventBean[][] eventsPerStream = new EventBean[streamNames.length][];
            ArrayList<EventBean> events = new ArrayList<EventBean>();
            for (int i = 0; i < eventsPerStream.length; i++) {
                // For named windows and tables, we don't need to preload indexes from the iterators as this is always done already
                if (streamJoinAnalysisResult.getNamedWindows()[i] != null || streamJoinAnalysisResult.getTables()[i] != null) {
                    continue;
                }

                Iterator<EventBean> it = null;
                if (!(streamViews[i] instanceof HistoricalEventViewable) && !(streamViews[i] instanceof DerivedValueView)) {
                    try {
                        it = streamViews[i].iterator();
                    } catch (UnsupportedOperationException ex) {
                        // Joins do not support the iterator
                    }
                }

                if (it != null) {
                    for (; it.hasNext(); ) {
                        events.add(it.next());
                    }
                    eventsPerStream[i] = events.toArray(new EventBean[events.size()]);
                    events.clear();
                } else {
                    eventsPerStream[i] = new EventBean[0];
                }
            }

            // init
            joinSetComposerDesc.getJoinSetComposer().init(eventsPerStream, agentInstanceContext);
        }

        return joinSetComposerDesc;
    }

    private VirtualDWView getNamedWindowVirtualDataWindow(int streamNo, StreamJoinAnalysisResultRuntime streamJoinAnalysisResult, AgentInstanceContext agentInstanceContext) {
        NamedWindow namedWindow = streamJoinAnalysisResult.getNamedWindows()[streamNo];
        if (namedWindow == null) {
            return null;
        }
        if (!namedWindow.getRootView().isVirtualDataWindow()) {
            return null;
        }
        NamedWindowInstance instance = namedWindow.getNamedWindowInstance(agentInstanceContext);
        return instance.getRootViewInstance().getVirtualDataWindow();
    }

    private Map<TableLookupIndexReqKey, EventTable>[] removeTableIndexes(Map<TableLookupIndexReqKey, EventTable>[] indexesPerStream, Table[] tablesPerStream) {
        Map<TableLookupIndexReqKey, EventTable>[] result = new Map[indexesPerStream.length];
        for (int i = 0; i < indexesPerStream.length; i++) {
            if (tablesPerStream[i] == null) {
                result[i] = indexesPerStream[i];
                continue;
            }
            result[i] = Collections.emptyMap();
        }
        return result;
    }
}
