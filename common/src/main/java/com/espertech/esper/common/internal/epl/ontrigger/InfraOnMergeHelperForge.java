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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseElementCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorFactory;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectProcessorArgs;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategyFactory;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperForge;
import com.espertech.esper.common.internal.epl.updatehelper.EventBeanUpdateHelperForgeFactory;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.util.UuidGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for handles for updates/inserts/deletes/select
 */
public class InfraOnMergeHelperForge {
    private InfraOnMergeActionInsForge insertUnmatched;
    private List<InfraOnMergeMatchForge> matched;
    private List<InfraOnMergeMatchForge> unmatched;
    private boolean requiresTableWriteLock;

    public InfraOnMergeHelperForge(OnTriggerMergeDesc onTriggerDesc,
                                   EventType triggeringEventType,
                                   String triggeringStreamName,
                                   String infraName,
                                   EventTypeSPI infraEventType,
                                   StatementRawInfo statementRawInfo,
                                   StatementCompileTimeServices services,
                                   TableMetaData table)
            throws ExprValidationException {
        matched = new ArrayList<>();
        unmatched = new ArrayList<>();

        int count = 1;
        boolean hasDeleteAction = false;
        boolean hasInsertIntoTableAction = false;
        boolean hasUpdateAction = false;

        for (OnTriggerMergeMatched matchedItem : onTriggerDesc.getItems()) {
            List<InfraOnMergeActionForge> actions = new ArrayList<>();
            for (OnTriggerMergeAction item : matchedItem.getActions()) {
                try {
                    if (item instanceof OnTriggerMergeActionInsert) {
                        OnTriggerMergeActionInsert insertDesc = (OnTriggerMergeActionInsert) item;
                        InfraOnMergeActionInsForge forge = setupInsert(infraName, infraEventType, insertDesc, triggeringEventType, triggeringStreamName, statementRawInfo, services, table != null);
                        actions.add(forge);
                        hasInsertIntoTableAction = forge.getInsertIntoTable() != null;
                    } else if (item instanceof OnTriggerMergeActionUpdate) {
                        OnTriggerMergeActionUpdate updateDesc = (OnTriggerMergeActionUpdate) item;
                        EventBeanUpdateHelperForge updateHelper = EventBeanUpdateHelperForgeFactory.make(infraName, infraEventType, updateDesc.getAssignments(), onTriggerDesc.getOptionalAsName(), triggeringEventType, true, statementRawInfo.getStatementName(), services.getEventTypeAvroHandler());
                        ExprNode filterEval = updateDesc.getOptionalWhereClause();
                        if (table != null) {
                            TableUpdateStrategyFactory.validateTableUpdateOnMerge(table, updateHelper.getUpdateItemsPropertyNames());
                        }
                        InfraOnMergeActionUpdForge forge = new InfraOnMergeActionUpdForge(filterEval, updateHelper, table);
                        actions.add(forge);
                        hasUpdateAction = true;
                    } else if (item instanceof OnTriggerMergeActionDelete) {
                        OnTriggerMergeActionDelete deleteDesc = (OnTriggerMergeActionDelete) item;
                        ExprNode filterEval = deleteDesc.getOptionalWhereClause();
                        actions.add(new InfraOnMergeActionDelForge(filterEval));
                        hasDeleteAction = true;
                    } else {
                        throw new IllegalArgumentException("Invalid type of merge item '" + item.getClass() + "'");
                    }
                    count++;
                } catch (ExprValidationException | EPException ex) {
                    boolean isNot = item instanceof OnTriggerMergeActionInsert;
                    String message = "Validation failed in when-" + (isNot ? "not-" : "") + "matched (clause " + count + "): " + ex.getMessage();
                    throw new ExprValidationException(message, ex);
                }
            }

            if (matchedItem.isMatchedUnmatched()) {
                matched.add(new InfraOnMergeMatchForge(matchedItem.getOptionalMatchCond(), actions));
            } else {
                unmatched.add(new InfraOnMergeMatchForge(matchedItem.getOptionalMatchCond(), actions));
            }
        }

        if (onTriggerDesc.getOptionalInsertNoMatch() != null) {
            insertUnmatched = setupInsert(infraName, infraEventType, onTriggerDesc.getOptionalInsertNoMatch(), triggeringEventType, triggeringStreamName, statementRawInfo, services, table != null);
        }

        this.requiresTableWriteLock = hasDeleteAction || hasInsertIntoTableAction || hasUpdateAction;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(InfraOnMergeHelper.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(InfraOnMergeActionIns.class, "insertUnmatched", insertUnmatched == null ? constantNull() : insertUnmatched.make(method, symbols, classScope))
                .declareVar(List.class, "matched", makeList(matched, method, symbols, classScope))
                .declareVar(List.class, "unmatched", makeList(unmatched, method, symbols, classScope))
                .methodReturn(newInstance(InfraOnMergeHelper.class, ref("insertUnmatched"), ref("matched"), ref("unmatched"), constant(requiresTableWriteLock)));
        return localMethod(method);
    }

    private CodegenExpression makeList(List<InfraOnMergeMatchForge> items, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(List.class, this.getClass(), classScope);
        method.getBlock().declareVar(List.class, InfraOnMergeMatch.class, "list", newInstance(ArrayList.class, constant(items.size())));
        for (InfraOnMergeMatchForge item : items) {
            method.getBlock().exprDotMethod(ref("list"), "add", item.make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref("list"));
        return localMethod(method);
    }

    private InfraOnMergeActionInsForge setupInsert(String infraName, EventType infraType, OnTriggerMergeActionInsert desc, EventType triggeringEventType, String triggeringStreamName, StatementRawInfo statementRawInfo, StatementCompileTimeServices services, boolean isTable)
            throws ExprValidationException {

        // Compile insert-into info
        String streamName = desc.getOptionalStreamName() != null ? desc.getOptionalStreamName() : infraName;
        InsertIntoDesc insertIntoDesc = InsertIntoDesc.fromColumns(streamName, desc.getColumns());

        // rewrite any wildcards to use "stream.wildcard"
        if (triggeringStreamName == null) {
            triggeringStreamName = UuidGenerator.generate();
        }
        List<SelectClauseElementCompiled> selectNoWildcard = compileSelectNoWildcard(triggeringStreamName, desc.getSelectClauseCompiled());

        // Set up event types for select-clause evaluation: The first type does not contain anything as its the named-window or table row which is not present for insert
        EventTypeMetadata eventTypeMetadata = new EventTypeMetadata("merge_infra_insert", statementRawInfo.getModuleName(), EventTypeTypeClass.STREAM, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        EventType dummyTypeNoProperties = BaseNestableEventUtil.makeMapTypeCompileTime(eventTypeMetadata, Collections.<String, Object>emptyMap(), null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        EventType[] eventTypes = new EventType[]{dummyTypeNoProperties, triggeringEventType};
        String[] streamNames = new String[]{UuidGenerator.generate(), triggeringStreamName};
        StreamTypeService streamTypeService = new StreamTypeServiceImpl(eventTypes, streamNames, new boolean[eventTypes.length], false, false);

        // Get select expr processor
        SelectClauseElementCompiled[] selectClause = selectNoWildcard.toArray(new SelectClauseElementCompiled[selectNoWildcard.size()]);
        SelectProcessorArgs args = new SelectProcessorArgs(selectClause, null, false, null, null, streamTypeService,
                null, false, statementRawInfo.getAnnotations(), statementRawInfo, services);
        if (isTable && streamName.equals(infraName)) {
            args.setOptionalInsertIntoEventType(infraType);
        }

        SelectExprProcessorForge insertHelperForge = SelectExprProcessorFactory.getProcessor(args, insertIntoDesc, false).getForge();
        ExprNode filterEval = desc.getOptionalWhereClause();

        boolean route = !streamName.equals(infraName);
        boolean audit = AuditEnum.INSERT.getAudit(statementRawInfo.getAnnotations()) != null;

        TableMetaData insertIntoTable = services.getTableCompileTimeResolver().resolve(insertIntoDesc.getEventTypeName());
        return new InfraOnMergeActionInsForge(filterEval, insertHelperForge, insertIntoTable, audit, route);
    }

    public static List<SelectClauseElementCompiled> compileSelectNoWildcard(String triggeringStreamName, List<SelectClauseElementCompiled> selectClause) {
        List<SelectClauseElementCompiled> selectNoWildcard = new ArrayList<>();
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
}