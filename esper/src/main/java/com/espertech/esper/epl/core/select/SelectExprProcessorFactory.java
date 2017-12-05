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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.soda.ForClauseKeyword;
import com.espertech.esper.core.context.util.ContextDescriptor;
import com.espertech.esper.core.service.StatementExtensionSvcContext;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.eval.SelectExprStreamDesc;
import com.espertech.esper.epl.core.resultset.core.GroupByRollupInfo;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.dot.ExprDotNode;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.table.mgmt.TableServiceUtil;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.event.vaevent.ValueAddEventService;
import com.espertech.esper.schedule.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Factory for select expression processors.
 */
public class SelectExprProcessorFactory {
    private static final Logger log = LoggerFactory.getLogger(SelectExprProcessorFactory.class);

    public static SelectExprProcessorForge getProcessor(Collection<Integer> assignedTypeNumberStack,
                                                   SelectClauseElementCompiled[] selectionList,
                                                   boolean isUsingWildcard,
                                                   InsertIntoDesc insertIntoDesc,
                                                   EventType optionalInsertIntoEventType,
                                                   ForClauseSpec forClauseSpec,
                                                   StreamTypeService typeService,
                                                   EventAdapterService eventAdapterService,
                                                   StatementResultService statementResultService,
                                                   ValueAddEventService valueAddEventService,
                                                   SelectExprEventTypeRegistry selectExprEventTypeRegistry,
                                                   EngineImportService engineImportService,
                                                   ExprEvaluatorContext exprEvaluatorContext,
                                                   VariableService variableService,
                                                   TableService tableService,
                                                   TimeProvider timeProvider,
                                                   String engineURI,
                                                   int statementId,
                                                   String statementName,
                                                   Annotation[] annotations,
                                                   ContextDescriptor contextDescriptor,
                                                   ConfigurationInformation configuration,
                                                   SelectExprProcessorDeliveryCallback selectExprProcessorCallback,
                                                   NamedWindowMgmtService namedWindowMgmtService,
                                                   IntoTableSpec intoTableClause,
                                                   GroupByRollupInfo groupByRollupInfo,
                                                   StatementExtensionSvcContext statementExtensionSvcContext)
            throws ExprValidationException {
        if (selectExprProcessorCallback != null) {
            BindProcessorForge bindProcessorForge = new BindProcessorForge(selectionList, typeService.getEventTypes(), typeService.getStreamNames(), tableService);
            Map<String, Object> properties = new LinkedHashMap<String, Object>();
            for (int i = 0; i < bindProcessorForge.getColumnNamesAssigned().length; i++) {
                properties.put(bindProcessorForge.getColumnNamesAssigned()[i], bindProcessorForge.getExpressionTypes()[i]);
            }
            EventType eventType = eventAdapterService.createAnonymousObjectArrayType("Output_" + statementName, properties);
            return new SelectExprProcessorWDeliveryCallback(eventType, bindProcessorForge, selectExprProcessorCallback);
        }

        SelectExprProcessorForge synthetic = getProcessorInternal(assignedTypeNumberStack, selectionList, isUsingWildcard, insertIntoDesc, optionalInsertIntoEventType, typeService, eventAdapterService, valueAddEventService, selectExprEventTypeRegistry, engineImportService, statementId, statementName, annotations, configuration, namedWindowMgmtService, tableService, groupByRollupInfo);

        // Handle table as an optional service
        if (statementResultService != null) {
            // Handle for-clause delivery contract checking
            ExprNode[] groupedDeliveryExpr = null;
            boolean forDelivery = false;
            if (forClauseSpec != null) {
                for (ForClauseItemSpec item : forClauseSpec.getClauses()) {
                    if (item.getKeyword() == null) {
                        throw new ExprValidationException("Expected any of the " + Arrays.toString(ForClauseKeyword.values()).toLowerCase(Locale.ENGLISH) + " for-clause keywords after reserved keyword 'for'");
                    }
                    try {
                        ForClauseKeyword keyword = ForClauseKeyword.valueOf(item.getKeyword().toUpperCase(Locale.ENGLISH));
                        if ((keyword == ForClauseKeyword.GROUPED_DELIVERY) && (item.getExpressions().isEmpty())) {
                            throw new ExprValidationException("The for-clause with the " + ForClauseKeyword.GROUPED_DELIVERY.getName() + " keyword requires one or more grouping expressions");
                        }
                        if ((keyword == ForClauseKeyword.DISCRETE_DELIVERY) && (!item.getExpressions().isEmpty())) {
                            throw new ExprValidationException("The for-clause with the " + ForClauseKeyword.DISCRETE_DELIVERY.getName() + " keyword does not allow grouping expressions");
                        }
                        if (forDelivery) {
                            throw new ExprValidationException("The for-clause with delivery keywords may only occur once in a statement");
                        }
                    } catch (RuntimeException ex) {
                        throw new ExprValidationException("Expected any of the " + Arrays.toString(ForClauseKeyword.values()).toLowerCase(Locale.ENGLISH) + " for-clause keywords after reserved keyword 'for'");
                    }

                    StreamTypeService type = new StreamTypeServiceImpl(synthetic.getResultEventType(), null, false, engineURI);
                    groupedDeliveryExpr = new ExprNode[item.getExpressions().size()];
                    ExprValidationContext validationContext = new ExprValidationContext(type, engineImportService, statementExtensionSvcContext, null, timeProvider, variableService, tableService, exprEvaluatorContext, eventAdapterService, statementName, statementId, annotations, null, false, false, true, false, intoTableClause == null ? null : intoTableClause.getName(), false);  // no context descriptor available
                    for (int i = 0; i < item.getExpressions().size(); i++) {
                        groupedDeliveryExpr[i] = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.FORCLAUSE, item.getExpressions().get(i), validationContext);
                    }
                    forDelivery = true;
                }
            }

            BindProcessorForge bindProcessor = new BindProcessorForge(selectionList, typeService.getEventTypes(), typeService.getStreamNames(), tableService);
            ExprEvaluator[] groupedDeliveryEvals = ExprNodeUtilityRich.getEvaluatorsMayCompile(groupedDeliveryExpr, engineImportService, SelectExprProcessorFactory.class, typeService.isOnDemandStreams(), statementName);
            statementResultService.setSelectClause(bindProcessor.getExpressionTypes(), bindProcessor.getColumnNamesAssigned(), forDelivery, groupedDeliveryEvals, exprEvaluatorContext);
            return new SelectExprResultProcessor(statementResultService, synthetic, bindProcessor);
        }

        return synthetic;
    }

    private static SelectExprProcessorForge getProcessorInternal(
            Collection<Integer> assignedTypeNumberStack,
            SelectClauseElementCompiled[] selectionList,
            boolean isUsingWildcard,
            InsertIntoDesc insertIntoDesc,
            EventType optionalInsertIntoEventType,
            StreamTypeService typeService,
            EventAdapterService eventAdapterService,
            ValueAddEventService valueAddEventService,
            SelectExprEventTypeRegistry selectExprEventTypeRegistry,
            EngineImportService engineImportService,
            int statementId,
            String statementName,
            Annotation[] annotations,
            ConfigurationInformation configuration,
            NamedWindowMgmtService namedWindowMgmtService,
            TableService tableService,
            GroupByRollupInfo groupByRollupInfo)
            throws ExprValidationException {
        // Wildcard not allowed when insert into specifies column order
        if (isUsingWildcard && insertIntoDesc != null && !insertIntoDesc.getColumnNames().isEmpty()) {
            throw new ExprValidationException("Wildcard not allowed when insert-into specifies column order");
        }

        // Determine wildcard processor (select *)
        if (isWildcardsOnly(selectionList)) {
            // For joins
            if (typeService.getStreamNames().length > 1) {
                log.debug(".getProcessor Using SelectExprJoinWildcardProcessor");
                return SelectExprJoinWildcardProcessorFactory.create(assignedTypeNumberStack, statementId, statementName, typeService.getStreamNames(), typeService.getEventTypes(), eventAdapterService, insertIntoDesc, selectExprEventTypeRegistry, engineImportService, annotations, configuration, tableService, typeService.getEngineURIQualifier(), typeService.isOnDemandStreams());
            } else if (insertIntoDesc == null) {
                // Single-table selects with no insert-into
                // don't need extra processing
                log.debug(".getProcessor Using wildcard processor");
                if (typeService.hasTableTypes()) {
                    String tableName = TableServiceUtil.getTableNameFromEventType(typeService.getEventTypes()[0]);
                    return new SelectExprWildcardTableProcessor(tableName, tableService);
                }
                return new SelectExprWildcardProcessor(typeService.getEventTypes()[0]);
            }
        }

        // Verify the assigned or name used is unique
        if (insertIntoDesc == null) {
            verifyNameUniqueness(selectionList);
        }

        // Construct processor
        SelectExprBuckets buckets = getSelectExpressionBuckets(selectionList);

        SelectExprProcessorHelper factory = new SelectExprProcessorHelper(assignedTypeNumberStack, buckets.expressions, buckets.selectedStreams, insertIntoDesc, optionalInsertIntoEventType, isUsingWildcard, typeService, eventAdapterService, valueAddEventService, selectExprEventTypeRegistry, engineImportService, statementId, statementName, annotations, configuration, namedWindowMgmtService, tableService, groupByRollupInfo);
        SelectExprProcessorForge processor = factory.getForge();

        // add reference to the type obtained
        EventTypeSPI type = (EventTypeSPI) processor.getResultEventType();
        if (!typeService.isOnDemandStreams() && type.getMetadata().getTypeClass() != EventTypeMetadata.TypeClass.ANONYMOUS) {
            selectExprEventTypeRegistry.add(processor.getResultEventType());
        }
        return processor;
    }

    /**
     * Verify that each given name occurs exactly one.
     *
     * @param selectionList is the list of select items to verify names
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException thrown if a name occured more then once
     */
    protected static void verifyNameUniqueness(SelectClauseElementCompiled[] selectionList) throws ExprValidationException {
        Set<String> names = new HashSet<String>();
        for (SelectClauseElementCompiled element : selectionList) {
            if (element instanceof SelectClauseExprCompiledSpec) {
                SelectClauseExprCompiledSpec expr = (SelectClauseExprCompiledSpec) element;
                if (names.contains(expr.getAssignedName())) {
                    throw new ExprValidationException("Column name '" + expr.getAssignedName() + "' appears more then once in select clause");
                }
                names.add(expr.getAssignedName());
            } else if (element instanceof SelectClauseStreamCompiledSpec) {
                SelectClauseStreamCompiledSpec stream = (SelectClauseStreamCompiledSpec) element;
                if (stream.getOptionalName() == null) {
                    continue; // ignore no-name stream selectors
                }
                if (names.contains(stream.getOptionalName())) {
                    throw new ExprValidationException("Column name '" + stream.getOptionalName() + "' appears more then once in select clause");
                }
                names.add(stream.getOptionalName());
            }
        }
    }

    private static boolean isWildcardsOnly(SelectClauseElementCompiled[] elements) {
        for (SelectClauseElementCompiled element : elements) {
            if (!(element instanceof SelectClauseElementWildcard)) {
                return false;
            }
        }
        return true;
    }

    private static SelectExprBuckets getSelectExpressionBuckets(SelectClauseElementCompiled[] elements) {
        List<SelectClauseExprCompiledSpec> expressions = new ArrayList<SelectClauseExprCompiledSpec>();
        List<SelectExprStreamDesc> selectedStreams = new ArrayList<SelectExprStreamDesc>();

        for (SelectClauseElementCompiled element : elements) {
            if (element instanceof SelectClauseExprCompiledSpec) {
                SelectClauseExprCompiledSpec expr = (SelectClauseExprCompiledSpec) element;
                if (!isTransposingFunction(expr.getSelectExpression())) {
                    expressions.add(expr);
                } else {
                    selectedStreams.add(new SelectExprStreamDesc(expr));
                }
            } else if (element instanceof SelectClauseStreamCompiledSpec) {
                selectedStreams.add(new SelectExprStreamDesc((SelectClauseStreamCompiledSpec) element));
            }
        }
        return new SelectExprBuckets(expressions, selectedStreams);
    }

    private static boolean isTransposingFunction(ExprNode selectExpression) {
        if (!(selectExpression instanceof ExprDotNode)) {
            return false;
        }
        ExprDotNode dotNode = (ExprDotNode) selectExpression;
        if (dotNode.getChainSpec().get(0).getName().toLowerCase(Locale.ENGLISH).equals(EngineImportService.EXT_SINGLEROW_FUNCTION_TRANSPOSE)) {
            return true;
        }
        return false;
    }

    public static class SelectExprBuckets {
        private final List<SelectClauseExprCompiledSpec> expressions;
        private final List<SelectExprStreamDesc> selectedStreams;

        public SelectExprBuckets(List<SelectClauseExprCompiledSpec> expressions, List<SelectExprStreamDesc> selectedStreams) {
            this.expressions = expressions;
            this.selectedStreams = selectedStreams;
        }

        public List<SelectExprStreamDesc> getSelectedStreams() {
            return selectedStreams;
        }

        public List<SelectClauseExprCompiledSpec> getExpressions() {
            return expressions;
        }
    }
}
