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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.InternalEventRouter;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;
import com.espertech.esper.epl.core.select.*;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelperFactory;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.util.UuidGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory for handles for updates/inserts/deletes/select
 */
public class NamedWindowOnMergeHelper {
    private NamedWindowOnMergeActionIns insertUnmatched;
    private List<NamedWindowOnMergeMatch> matched;
    private List<NamedWindowOnMergeMatch> unmatched;

    public NamedWindowOnMergeHelper(StatementContext statementContext,
                                    OnTriggerMergeDesc onTriggerDesc,
                                    EventType triggeringEventType,
                                    String triggeringStreamName,
                                    InternalEventRouter internalEventRouter,
                                    String namedWindowName,
                                    EventTypeSPI namedWindowType)
            throws ExprValidationException {
        matched = new ArrayList<>();
        unmatched = new ArrayList<>();

        int count = 1;
        for (OnTriggerMergeMatched matchedItem : onTriggerDesc.getItems()) {
            List<NamedWindowOnMergeAction> actions = new ArrayList<NamedWindowOnMergeAction>();
            for (OnTriggerMergeAction item : matchedItem.getActions()) {
                try {
                    if (item instanceof OnTriggerMergeActionInsert) {
                        OnTriggerMergeActionInsert insertDesc = (OnTriggerMergeActionInsert) item;
                        actions.add(setupInsert(namedWindowName, internalEventRouter, namedWindowType, count, insertDesc, triggeringEventType, triggeringStreamName, statementContext));
                    } else if (item instanceof OnTriggerMergeActionUpdate) {
                        OnTriggerMergeActionUpdate updateDesc = (OnTriggerMergeActionUpdate) item;
                        EventBeanUpdateHelper updateHelper = EventBeanUpdateHelperFactory.make(namedWindowName, namedWindowType, updateDesc.getAssignments(), onTriggerDesc.getOptionalAsName(), triggeringEventType, true, statementContext.getStatementName(), statementContext.getEngineURI(), statementContext.getEventAdapterService(), false);
                        ExprEvaluator filterEval = updateDesc.getOptionalWhereClause() == null ? null : ExprNodeCompiler.allocateEvaluator(updateDesc.getOptionalWhereClause().getForge(), statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());
                        actions.add(new NamedWindowOnMergeActionUpd(filterEval, updateHelper));
                    } else if (item instanceof OnTriggerMergeActionDelete) {
                        OnTriggerMergeActionDelete deleteDesc = (OnTriggerMergeActionDelete) item;
                        ExprEvaluator filterEval = deleteDesc.getOptionalWhereClause() == null ? null : ExprNodeCompiler.allocateEvaluator(deleteDesc.getOptionalWhereClause().getForge(), statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());
                        actions.add(new NamedWindowOnMergeActionDel(filterEval));
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
                matched.add(new NamedWindowOnMergeMatch(matchedItem.getOptionalMatchCond(), actions, statementContext.getEngineImportService(), statementContext.getStatementName()));
            } else {
                unmatched.add(new NamedWindowOnMergeMatch(matchedItem.getOptionalMatchCond(), actions, statementContext.getEngineImportService(), statementContext.getStatementName()));
            }
        }

        if (onTriggerDesc.getOptionalInsertNoMatch() != null) {
            insertUnmatched = setupInsert(namedWindowName, internalEventRouter, namedWindowType, count, onTriggerDesc.getOptionalInsertNoMatch(), triggeringEventType, triggeringStreamName, statementContext);
        }
    }

    private NamedWindowOnMergeActionIns setupInsert(String namedWindowName, InternalEventRouter internalEventRouter, EventTypeSPI eventTypeNamedWindow, int selectClauseNumber, OnTriggerMergeActionInsert desc, EventType triggeringEventType, String triggeringStreamName, StatementContext statementContext)
            throws ExprValidationException {

        // Compile insert-into info
        String streamName = desc.getOptionalStreamName() != null ? desc.getOptionalStreamName() : eventTypeNamedWindow.getName();
        InsertIntoDesc insertIntoDesc = InsertIntoDesc.fromColumns(streamName, desc.getColumns());

        // rewrite any wildcards to use "stream.wildcard"
        if (triggeringStreamName == null) {
            triggeringStreamName = UuidGenerator.generate();
        }
        List<SelectClauseElementCompiled> selectNoWildcard = compileSelectNoWildcard(triggeringStreamName, desc.getSelectClauseCompiled());

        // Set up event types for select-clause evaluation: The first type does not contain anything as its the named window row which is not present for insert
        EventType dummyTypeNoProperties = new MapEventType(EventTypeMetadata.createAnonymous("merge_named_window_insert", EventTypeMetadata.ApplicationType.MAP), "merge_named_window_insert", 0, null, Collections.<String, Object>emptyMap(), null, null, null);
        EventType[] eventTypes = new EventType[]{dummyTypeNoProperties, triggeringEventType};
        String[] streamNames = new String[]{UuidGenerator.generate(), triggeringStreamName};
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(eventTypes, streamNames, new boolean[1], statementContext.getEngineURI(), false, false);

        // Get select expr processor
        SelectExprEventTypeRegistry selectExprEventTypeRegistry = new SelectExprEventTypeRegistry(statementContext.getStatementName(), statementContext.getStatementEventTypeRef());
        ExprEvaluatorContextStatement exprEvaluatorContext = new ExprEvaluatorContextStatement(statementContext, false);
        SelectExprProcessorForge insertHelperForge = SelectExprProcessorFactory.getProcessor(Collections.singleton(selectClauseNumber),
                selectNoWildcard.toArray(new SelectClauseElementCompiled[selectNoWildcard.size()]), false, insertIntoDesc, null, null, streamTypeService,
                statementContext.getEventAdapterService(), statementContext.getStatementResultService(), statementContext.getValueAddEventService(), selectExprEventTypeRegistry,
                statementContext.getEngineImportService(), exprEvaluatorContext, statementContext.getVariableService(), statementContext.getTableService(), statementContext.getTimeProvider(), statementContext.getEngineURI(), statementContext.getStatementId(), statementContext.getStatementName(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), statementContext.getConfigSnapshot(), null, statementContext.getNamedWindowMgmtService(), null, null,
                statementContext.getStatementExtensionServicesContext());
        SelectExprProcessor insertHelper = SelectExprProcessorCompiler.allocateSelectExprEvaluator(statementContext.getEventAdapterService(), insertHelperForge, statementContext.getEngineImportService(), NamedWindowOnMergeHelper.class, false, statementContext.getStatementName());

        ExprEvaluator filterEval = desc.getOptionalWhereClause() == null ? null : ExprNodeCompiler.allocateEvaluator(desc.getOptionalWhereClause().getForge(), statementContext.getEngineImportService(), this.getClass(), false, statementContext.getStatementName());

        InternalEventRouter routerToUser = streamName.equals(namedWindowName) ? null : internalEventRouter;
        boolean audit = AuditEnum.INSERT.getAudit(statementContext.getAnnotations()) != null;

        String insertIntoTableName = null;
        if (statementContext.getTableService().getTableMetadata(insertIntoDesc.getEventTypeName()) != null) {
            insertIntoTableName = insertIntoDesc.getEventTypeName();
        }

        return new NamedWindowOnMergeActionIns(filterEval, insertHelper, routerToUser, insertIntoTableName, statementContext.getTableService(), statementContext.getEpStatementHandle(), statementContext.getInternalEventEngineRouteDest(), audit);
    }

    public static List<SelectClauseElementCompiled> compileSelectNoWildcard(String triggeringStreamName, List<SelectClauseElementCompiled> selectClause) {
        List<SelectClauseElementCompiled> selectNoWildcard = new ArrayList<SelectClauseElementCompiled>();
        for (SelectClauseElementCompiled element : selectClause) {
            if (!(element instanceof SelectClauseElementWildcard)) {
                selectNoWildcard.add(element);
                continue;
            }
            SelectClauseStreamCompiledSpec streamSelect = new SelectClauseStreamCompiledSpec(triggeringStreamName, null);
            streamSelect.setStreamNumber(1);
            selectNoWildcard.add(streamSelect);
        }
        return selectNoWildcard;
    }

    public List<NamedWindowOnMergeMatch> getMatched() {
        return matched;
    }

    public List<NamedWindowOnMergeMatch> getUnmatched() {
        return unmatched;
    }

    public NamedWindowOnMergeActionIns getInsertUnmatched() {
        return insertUnmatched;
    }
}