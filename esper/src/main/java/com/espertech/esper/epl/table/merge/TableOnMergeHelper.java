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
package com.espertech.esper.epl.table.merge;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.InternalEventRouter;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.select.*;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.named.NamedWindowOnMergeHelper;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.upd.TableUpdateStrategy;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelperFactory;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.util.UuidGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory for handles for updates/inserts/deletes/select
 */
public class TableOnMergeHelper {
    private List<TableOnMergeMatch> matched;
    private List<TableOnMergeMatch> unmatched;
    private TableOnMergeActionIns insertUnmatched;
    private boolean requiresWriteLock;

    public TableOnMergeHelper(StatementContext statementContext,
                              OnTriggerMergeDesc onTriggerDesc,
                              EventType triggeringEventType,
                              String triggeringStreamName,
                              InternalEventRouter internalEventRouter,
                              TableMetadata tableMetadata)
            throws ExprValidationException {
        matched = new ArrayList<TableOnMergeMatch>();
        unmatched = new ArrayList<TableOnMergeMatch>();

        int count = 1;
        boolean hasDeleteAction = false;
        boolean hasInsertIntoTableAction = false;
        boolean hasUpdateAction = false;
        for (OnTriggerMergeMatched matchedItem : onTriggerDesc.getItems()) {
            List<TableOnMergeAction> actions = new ArrayList<TableOnMergeAction>();
            for (OnTriggerMergeAction item : matchedItem.getActions()) {
                try {
                    if (item instanceof OnTriggerMergeActionInsert) {
                        OnTriggerMergeActionInsert insertDesc = (OnTriggerMergeActionInsert) item;
                        TableOnMergeActionIns action = setupInsert(tableMetadata, internalEventRouter, count, insertDesc, triggeringEventType, triggeringStreamName, statementContext);
                        actions.add(action);
                        hasInsertIntoTableAction = action.isInsertIntoBinding();
                    } else if (item instanceof OnTriggerMergeActionUpdate) {
                        OnTriggerMergeActionUpdate updateDesc = (OnTriggerMergeActionUpdate) item;
                        EventBeanUpdateHelper updateHelper = EventBeanUpdateHelperFactory.make(tableMetadata.getTableName(), tableMetadata.getInternalEventType(), updateDesc.getAssignments(), onTriggerDesc.getOptionalAsName(), triggeringEventType, false, statementContext.getStatementName(), statementContext.getEngineURI(), statementContext.getEventAdapterService(), false);
                        ExprEvaluator filterEval = updateDesc.getOptionalWhereClause() == null ? null : ExprNodeCompiler.allocateEvaluator(updateDesc.getOptionalWhereClause().getForge(), statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());
                        TableUpdateStrategy tableUpdateStrategy = statementContext.getTableService().getTableUpdateStrategy(tableMetadata, updateHelper, true);
                        TableOnMergeActionUpd upd = new TableOnMergeActionUpd(filterEval, tableUpdateStrategy);
                        actions.add(upd);
                        statementContext.getTableService().addTableUpdateStrategyReceiver(tableMetadata, statementContext.getStatementName(), upd, updateHelper, true);
                        hasUpdateAction = true;
                    } else if (item instanceof OnTriggerMergeActionDelete) {
                        OnTriggerMergeActionDelete deleteDesc = (OnTriggerMergeActionDelete) item;
                        ExprEvaluator filterEval = deleteDesc.getOptionalWhereClause() == null ? null : ExprNodeCompiler.allocateEvaluator(deleteDesc.getOptionalWhereClause().getForge(), statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());
                        actions.add(new TableOnMergeActionDel(filterEval));
                        hasDeleteAction = true;
                    } else {
                        throw new IllegalArgumentException("Invalid type of merge item '" + item.getClass() + "'");
                    }
                    count++;
                } catch (ExprValidationException ex) {
                    boolean isNot = item instanceof OnTriggerMergeActionInsert;
                    String message = "Validation failed in when-" + (isNot ? "not-" : "") + "matched (clause " + count + "): " + ex.getMessage();
                    throw new ExprValidationException(message, ex);
                }
            }

            if (matchedItem.isMatchedUnmatched()) {
                matched.add(new TableOnMergeMatch(matchedItem.getOptionalMatchCond(), actions, statementContext.getEngineImportService(), statementContext.getStatementName()));
            } else {
                unmatched.add(new TableOnMergeMatch(matchedItem.getOptionalMatchCond(), actions, statementContext.getEngineImportService(), statementContext.getStatementName()));
            }
        }

        if (onTriggerDesc.getOptionalInsertNoMatch() != null) {
            insertUnmatched = setupInsert(tableMetadata, internalEventRouter, count, onTriggerDesc.getOptionalInsertNoMatch(), triggeringEventType, triggeringStreamName, statementContext);
        }

        // since updates may change future secondary keys
        requiresWriteLock = hasDeleteAction || hasInsertIntoTableAction || hasUpdateAction;
    }

    private TableOnMergeActionIns setupInsert(TableMetadata tableMetadata, InternalEventRouter internalEventRouter, int selectClauseNumber, OnTriggerMergeActionInsert desc, EventType triggeringEventType, String triggeringStreamName, StatementContext statementContext)
            throws ExprValidationException {

        // Compile insert-into info
        String streamName = desc.getOptionalStreamName() != null ? desc.getOptionalStreamName() : tableMetadata.getTableName();
        InsertIntoDesc insertIntoDesc = InsertIntoDesc.fromColumns(streamName, desc.getColumns());
        EventType insertIntoTargetType = streamName.equals(tableMetadata.getTableName()) ? tableMetadata.getInternalEventType() : null;

        // rewrite any wildcards to use "stream.wildcard"
        if (triggeringStreamName == null) {
            triggeringStreamName = UuidGenerator.generate();
        }
        List<SelectClauseElementCompiled> selectNoWildcard = NamedWindowOnMergeHelper.compileSelectNoWildcard(triggeringStreamName, desc.getSelectClauseCompiled());

        // Set up event types for select-clause evaluation: The first type does not contain anything as its the named window row which is not present for insert
        EventType dummyTypeNoProperties = new MapEventType(EventTypeMetadata.createAnonymous("merge_named_window_insert", EventTypeMetadata.ApplicationType.MAP), "merge_named_window_insert", 0, null, Collections.<String, Object>emptyMap(), null, null, null);
        EventType[] eventTypes = new EventType[]{dummyTypeNoProperties, triggeringEventType};
        String[] streamNames = new String[]{UuidGenerator.generate(), triggeringStreamName};
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(eventTypes, streamNames, new boolean[1], statementContext.getEngineURI(), false, false);

        // Get select expr processor
        SelectExprEventTypeRegistry selectExprEventTypeRegistry = new SelectExprEventTypeRegistry(statementContext.getStatementName(), statementContext.getStatementEventTypeRef());
        ExprEvaluatorContextStatement exprEvaluatorContext = new ExprEvaluatorContextStatement(statementContext, false);
        SelectExprProcessorForge insertHelperForge = SelectExprProcessorFactory.getProcessor(Collections.singleton(selectClauseNumber),
                selectNoWildcard.toArray(new SelectClauseElementCompiled[selectNoWildcard.size()]), false, insertIntoDesc, insertIntoTargetType, null, streamTypeService,
                statementContext.getEventAdapterService(), statementContext.getStatementResultService(), statementContext.getValueAddEventService(), selectExprEventTypeRegistry,
                statementContext.getEngineImportService(), exprEvaluatorContext, statementContext.getVariableService(), statementContext.getTableService(), statementContext.getTimeProvider(), statementContext.getEngineURI(), statementContext.getStatementId(), statementContext.getStatementName(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), statementContext.getConfigSnapshot(), null, statementContext.getNamedWindowMgmtService(), null, null,
                statementContext.getStatementExtensionServicesContext());
        SelectExprProcessor insertHelper = SelectExprProcessorCompiler.allocateSelectExprEvaluator(statementContext.getEventAdapterService(), insertHelperForge, statementContext.getEngineImportService(), TableOnMergeHelper.class, false, statementContext.getStatementName());
        ExprEvaluator filterEval = desc.getOptionalWhereClause() == null ? null : ExprNodeCompiler.allocateEvaluator(desc.getOptionalWhereClause().getForge(), statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());

        InternalEventRouter routerToUser = streamName.equals(tableMetadata.getTableName()) ? null : internalEventRouter;
        boolean audit = AuditEnum.INSERT.getAudit(statementContext.getAnnotations()) != null;
        return new TableOnMergeActionIns(filterEval, insertHelper, routerToUser, statementContext.getEpStatementHandle(), statementContext.getInternalEventEngineRouteDest(), audit, tableMetadata.getRowFactory());
    }

    public List<TableOnMergeMatch> getMatched() {
        return matched;
    }

    public List<TableOnMergeMatch> getUnmatched() {
        return unmatched;
    }

    public TableOnMergeActionIns getInsertUnmatched() {
        return insertUnmatched;
    }

    public boolean isRequiresWriteLock() {
        return requiresWriteLock;
    }
}