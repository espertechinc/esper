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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseElementCompiled;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseExprCompiledSpec;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.OnTriggerActivatorDesc;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorDesc;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetSpec;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.subselect.SubSelectActivationPlan;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryForge;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperForgePlan;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperForgePlanner;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalHelperPlan;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperValidate;
import com.espertech.esper.common.internal.util.UuidGenerator;

import java.util.*;

public class OnTriggerPlanValidator {
    public static final String INITIAL_VALUE_STREAM_NAME = "initial";

    public static OnTriggerPlanValidationResult validateOnTriggerPlan(EventType namedWindowOrTableType,
                                                                      OnTriggerWindowDesc onTriggerDesc,
                                                                      StreamSpecCompiled streamSpec,
                                                                      OnTriggerActivatorDesc activatorResult,
                                                                      Map<ExprSubselectNode, SubSelectActivationPlan> subselectActivation,
                                                                      StatementBaseInfo base,
                                                                      StatementCompileTimeServices services) throws ExprValidationException {
        String zeroStreamAliasName = onTriggerDesc.getOptionalAsName();
        if (zeroStreamAliasName == null) {
            zeroStreamAliasName = "stream_0";
        }
        String streamName = streamSpec.getOptionalStreamName();
        if (streamName == null) {
            streamName = "stream_1";
        }
        String namedWindowTypeName = onTriggerDesc.getWindowName();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        // Materialize sub-select views
        // 0 - named window stream
        // 1 - arriving stream
        // 2 - initial value before update
        String[] subselectStreamNames = new String[]{zeroStreamAliasName, streamSpec.getOptionalStreamName()};
        EventType[] subselectEventTypes = new EventType[]{namedWindowOrTableType, activatorResult.getActivatorResultEventType()};
        String[] subselectEventTypeNames = new String[]{namedWindowTypeName, activatorResult.getTriggerEventTypeName()};
        SubSelectHelperForgePlan subselectForgePlan = SubSelectHelperForgePlanner.planSubSelect(base, subselectActivation, subselectStreamNames, subselectEventTypes, subselectEventTypeNames, services);
        Map<ExprSubselectNode, SubSelectFactoryForge> subselectForges = subselectForgePlan.getSubselects();
        additionalForgeables.addAll(subselectForgePlan.getAdditionalForgeables());

        StreamTypeServiceImpl typeService = new StreamTypeServiceImpl(new EventType[]{namedWindowOrTableType, activatorResult.getActivatorResultEventType()}, new String[]{zeroStreamAliasName, streamName}, new boolean[]{false, true}, true, false);

        // allow "initial" as a prefix to properties
        StreamTypeServiceImpl assignmentTypeService;
        if (zeroStreamAliasName.equals(INITIAL_VALUE_STREAM_NAME) || streamName.equals(INITIAL_VALUE_STREAM_NAME)) {
            assignmentTypeService = typeService;
        } else {
            assignmentTypeService = new StreamTypeServiceImpl(new EventType[]{namedWindowOrTableType, activatorResult.getActivatorResultEventType(), namedWindowOrTableType}, new String[]{zeroStreamAliasName, streamName, INITIAL_VALUE_STREAM_NAME}, new boolean[]{false, true, true}, false, false);
            assignmentTypeService.setStreamZeroUnambigous(true);
        }

        if (onTriggerDesc instanceof OnTriggerWindowUpdateDesc) {
            OnTriggerWindowUpdateDesc updateDesc = (OnTriggerWindowUpdateDesc) onTriggerDesc;
            ExprValidationContext validationContext = new ExprValidationContextBuilder(assignmentTypeService, base.getStatementRawInfo(), services)
                    .withAllowBindingConsumption(true).build();
            for (OnTriggerSetAssignment assignment : updateDesc.getAssignments()) {
                ExprNode validated = ExprNodeUtilityValidate.getValidatedAssignment(assignment, validationContext);
                assignment.setExpression(validated);
                EPStatementStartMethodHelperValidate.validateNoAggregations(validated, "Aggregation functions may not be used within an on-update-clause");
            }
        }
        if (onTriggerDesc instanceof OnTriggerMergeDesc) {
            OnTriggerMergeDesc mergeDesc = (OnTriggerMergeDesc) onTriggerDesc;
            validateMergeDesc(mergeDesc, namedWindowOrTableType, zeroStreamAliasName, activatorResult.getActivatorResultEventType(), streamName, base.getStatementRawInfo(), services);
        }

        // validate join expression
        ExprNode validatedJoin = validateJoinNamedWindow(ExprNodeOrigin.WHERE, base.getStatementSpec().getRaw().getWhereClause(),
                namedWindowOrTableType, zeroStreamAliasName, namedWindowTypeName,
                activatorResult.getActivatorResultEventType(), streamName, activatorResult.getTriggerEventTypeName(),
                null, base.getStatementRawInfo(), services);

        // validate filter, output rate limiting
        EPStatementStartMethodHelperValidate.validateNodes(base.getStatementSpec().getRaw(), typeService, null, base.getStatementRawInfo(), services);

        // Construct a processor for results; for use in on-select to process selection results
        // Use a wildcard select if the select-clause is empty, such as for on-delete.
        // For on-select the select clause is not empty.
        if (base.getStatementSpec().getSelectClauseCompiled().getSelectExprList().length == 0) {
            base.getStatementSpec().getSelectClauseCompiled().setSelectExprList(new SelectClauseElementWildcard());
        }

        ResultSetProcessorDesc resultSetProcessorPrototype = ResultSetProcessorFactoryFactory.getProcessorPrototype(new ResultSetSpec(base.getStatementSpec()),
                typeService, null, new boolean[0], true, base.getContextPropertyRegistry(), false, true, base.getStatementRawInfo(), services);
        additionalForgeables.addAll(resultSetProcessorPrototype.getAdditionalForgeables());

        // plan table access
        Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccessForges = ExprTableEvalHelperPlan.planTableAccess(base.getStatementSpec().getTableAccessNodes());

        return new OnTriggerPlanValidationResult(subselectForges, tableAccessForges, resultSetProcessorPrototype, validatedJoin, zeroStreamAliasName, additionalForgeables);
    }

    protected static ExprNode validateJoinNamedWindow(ExprNodeOrigin exprNodeOrigin,
                                                      ExprNode deleteJoinExpr,
                                                      EventType namedWindowType,
                                                      String namedWindowStreamName,
                                                      String namedWindowName,
                                                      EventType filteredType,
                                                      String filterStreamName,
                                                      String filteredTypeName,
                                                      String optionalTableName,
                                                      StatementRawInfo statementRawInfo,
                                                      StatementCompileTimeServices compileTimeServices
    ) throws ExprValidationException {
        if (deleteJoinExpr == null) {
            return null;
        }

        LinkedHashMap<String, Pair<EventType, String>> namesAndTypes = new LinkedHashMap<String, Pair<EventType, String>>();
        namesAndTypes.put(namedWindowStreamName, new Pair<EventType, String>(namedWindowType, namedWindowName));
        namesAndTypes.put(filterStreamName, new Pair<EventType, String>(filteredType, filteredTypeName));
        StreamTypeService typeService = new StreamTypeServiceImpl(namesAndTypes, false, false);

        ExprValidationContext validationContext = new ExprValidationContextBuilder(typeService, statementRawInfo, compileTimeServices)
                .withAllowBindingConsumption(true).build();
        return ExprNodeUtilityValidate.getValidatedSubtree(exprNodeOrigin, deleteJoinExpr, validationContext);
    }

    private static void validateMergeDesc(OnTriggerMergeDesc mergeDesc, EventType namedWindowType, String namedWindowName, EventType triggerStreamType, String triggerStreamName, StatementRawInfo statementRawInfo, StatementCompileTimeServices services)
            throws ExprValidationException {
        String exprNodeErrorMessage = "Aggregation functions may not be used within an merge-clause";

        EventTypeMetadata dummyTypeNoPropertiesMeta = new EventTypeMetadata("merge_named_window_insert", statementRawInfo.getModuleName(), EventTypeTypeClass.STREAM, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        EventType dummyTypeNoProperties = BaseNestableEventUtil.makeMapTypeCompileTime(dummyTypeNoPropertiesMeta, Collections.<String, Object>emptyMap(), null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        StreamTypeService insertOnlyTypeSvc = new StreamTypeServiceImpl(new EventType[]{dummyTypeNoProperties, triggerStreamType},
                new String[]{UuidGenerator.generate(), triggerStreamName}, new boolean[]{true, true}, true, false);
        StreamTypeServiceImpl twoStreamTypeSvc = new StreamTypeServiceImpl(new EventType[]{namedWindowType, triggerStreamType},
                new String[]{namedWindowName, triggerStreamName}, new boolean[]{true, true}, true, false);

        for (OnTriggerMergeMatched matchedItem : mergeDesc.getItems()) {

            // we may provide an additional stream "initial" for the prior value, unless already defined
            StreamTypeServiceImpl assignmentStreamTypeSvc;
            if (namedWindowName.equals(INITIAL_VALUE_STREAM_NAME) || triggerStreamName.equals(INITIAL_VALUE_STREAM_NAME)) {
                assignmentStreamTypeSvc = twoStreamTypeSvc;
            } else {
                assignmentStreamTypeSvc = new StreamTypeServiceImpl(new EventType[]{namedWindowType, triggerStreamType, namedWindowType},
                        new String[]{namedWindowName, triggerStreamName, INITIAL_VALUE_STREAM_NAME}, new boolean[]{true, true, true}, false, false);
                assignmentStreamTypeSvc.setStreamZeroUnambigous(true);
            }

            if (matchedItem.getOptionalMatchCond() != null) {
                StreamTypeService matchValidStreams = matchedItem.isMatchedUnmatched() ? twoStreamTypeSvc : insertOnlyTypeSvc;
                matchedItem.setOptionalMatchCond(EPStatementStartMethodHelperValidate.validateExprNoAgg(ExprNodeOrigin.MERGEMATCHCOND, matchedItem.getOptionalMatchCond(), matchValidStreams, exprNodeErrorMessage, true, false, statementRawInfo, services));
                if (!matchedItem.isMatchedUnmatched()) {
                    EPStatementStartMethodHelperValidate.validateSubqueryExcludeOuterStream(matchedItem.getOptionalMatchCond());
                }
            }

            for (OnTriggerMergeAction item : matchedItem.getActions()) {
                if (item instanceof OnTriggerMergeActionDelete) {
                    OnTriggerMergeActionDelete delete = (OnTriggerMergeActionDelete) item;
                    if (delete.getOptionalWhereClause() != null) {
                        delete.setOptionalWhereClause(EPStatementStartMethodHelperValidate.validateExprNoAgg(ExprNodeOrigin.MERGEMATCHWHERE, delete.getOptionalWhereClause(), twoStreamTypeSvc, exprNodeErrorMessage, true, false, statementRawInfo, services));
                    }
                } else if (item instanceof OnTriggerMergeActionUpdate) {
                    OnTriggerMergeActionUpdate update = (OnTriggerMergeActionUpdate) item;
                    if (update.getOptionalWhereClause() != null) {
                        update.setOptionalWhereClause(EPStatementStartMethodHelperValidate.validateExprNoAgg(ExprNodeOrigin.MERGEMATCHWHERE, update.getOptionalWhereClause(), twoStreamTypeSvc, exprNodeErrorMessage, true, false, statementRawInfo, services));
                    }
                    for (OnTriggerSetAssignment assignment : update.getAssignments()) {
                        assignment.setExpression(EPStatementStartMethodHelperValidate.validateExprNoAgg(ExprNodeOrigin.UPDATEASSIGN, assignment.getExpression(), assignmentStreamTypeSvc, exprNodeErrorMessage, true, true, statementRawInfo, services));
                    }
                } else if (item instanceof OnTriggerMergeActionInsert) {
                    OnTriggerMergeActionInsert insert = (OnTriggerMergeActionInsert) item;

                    StreamTypeService insertTypeSvc = getInsertStreamService(insert.getOptionalStreamName(), namedWindowName, insertOnlyTypeSvc, twoStreamTypeSvc);

                    if (insert.getOptionalWhereClause() != null) {
                        insert.setOptionalWhereClause(EPStatementStartMethodHelperValidate.validateExprNoAgg(ExprNodeOrigin.MERGEMATCHWHERE, insert.getOptionalWhereClause(), insertTypeSvc, exprNodeErrorMessage, true, false, statementRawInfo, services));
                    }

                    List<SelectClauseElementCompiled> compiledSelect = validateInsertSelect(insert.getSelectClause(), insertTypeSvc, insert.getColumns(), statementRawInfo, services);
                    insert.setSelectClauseCompiled(compiledSelect);
                } else {
                    throw new IllegalArgumentException("Unrecognized merge item '" + item.getClass().getName() + "'");
                }
            }
        }

        if (mergeDesc.getOptionalInsertNoMatch() != null) {
            StreamTypeService insertTypeSvc = getInsertStreamService(mergeDesc.getOptionalInsertNoMatch().getOptionalStreamName(), namedWindowName, insertOnlyTypeSvc, twoStreamTypeSvc);
            List<SelectClauseElementCompiled> compiledSelect = validateInsertSelect(mergeDesc.getOptionalInsertNoMatch().getSelectClause(), insertTypeSvc, mergeDesc.getOptionalInsertNoMatch().getColumns(), statementRawInfo, services);
            mergeDesc.getOptionalInsertNoMatch().setSelectClauseCompiled(compiledSelect);
        }
    }

    private static StreamTypeService getInsertStreamService(String optionalStreamName, String namedWindowName, StreamTypeService insertOnlyTypeSvc, StreamTypeServiceImpl twoStreamTypeSvc) {
        if (optionalStreamName == null || optionalStreamName.toLowerCase(Locale.ENGLISH).equals(namedWindowName.toLowerCase(Locale.ENGLISH))) {
            // if no name was provided in "insert into NAME" or the name is the named window we use the empty type in the first column
            return insertOnlyTypeSvc;
        }
        return twoStreamTypeSvc;
    }

    private static List<SelectClauseElementCompiled> validateInsertSelect(List<SelectClauseElementRaw> selectClause, StreamTypeService insertTypeSvc, List<String> insertColumns, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        int colIndex = 0;
        List<SelectClauseElementCompiled> compiledSelect = new ArrayList<SelectClauseElementCompiled>();
        for (SelectClauseElementRaw raw : selectClause) {
            if (raw instanceof SelectClauseStreamRawSpec) {
                SelectClauseStreamRawSpec rawStreamSpec = (SelectClauseStreamRawSpec) raw;
                Integer foundStreamNum = null;
                for (int s = 0; s < insertTypeSvc.getStreamNames().length; s++) {
                    if (rawStreamSpec.getStreamName().equals(insertTypeSvc.getStreamNames()[s])) {
                        foundStreamNum = s;
                        break;
                    }
                }
                if (foundStreamNum == null) {
                    throw new ExprValidationException("Stream by name '" + rawStreamSpec.getStreamName() + "' was not found");
                }
                SelectClauseStreamCompiledSpec streamSelectSpec = new SelectClauseStreamCompiledSpec(rawStreamSpec.getStreamName(), rawStreamSpec.getOptionalAsName());
                streamSelectSpec.setStreamNumber(foundStreamNum);
                compiledSelect.add(streamSelectSpec);
            } else if (raw instanceof SelectClauseExprRawSpec) {
                SelectClauseExprRawSpec exprSpec = (SelectClauseExprRawSpec) raw;
                ExprValidationContext validationContext = new ExprValidationContextBuilder(insertTypeSvc, statementRawInfo, services)
                        .withAllowBindingConsumption(true).build();
                ExprNode exprCompiled = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.SELECT, exprSpec.getSelectExpression(), validationContext);
                String resultName = exprSpec.getOptionalAsName();
                if (resultName == null) {
                    if (insertColumns.size() > colIndex) {
                        resultName = insertColumns.get(colIndex);
                    } else {
                        resultName = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(exprCompiled);
                    }
                }
                compiledSelect.add(new SelectClauseExprCompiledSpec(exprCompiled, resultName, exprSpec.getOptionalAsName(), exprSpec.isEvents()));
                EPStatementStartMethodHelperValidate.validateNoAggregations(exprCompiled, "Expression in a merge-selection may not utilize aggregation functions");
            } else if (raw instanceof SelectClauseElementWildcard) {
                compiledSelect.add(new SelectClauseElementWildcard());
            } else {
                throw new IllegalStateException("Unknown select clause item:" + raw);
            }
            colIndex++;
        }
        return compiledSelect;
    }
}
