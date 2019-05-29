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
package com.espertech.esper.common.internal.epl.resultset.select.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.soda.ForClauseKeyword;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage1.spec.ForClauseItemSpec;
import com.espertech.esper.common.internal.compile.stage1.spec.InsertIntoDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseElementWildcard;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseElementCompiled;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseExprCompiledSpec;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNode;
import com.espertech.esper.common.internal.epl.resultset.select.eval.SelectEvalWildcardNonJoin;
import com.espertech.esper.common.internal.epl.resultset.select.eval.SelectEvalWildcardTable;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.event.core.WrapperEventType;
import com.espertech.esper.common.internal.event.variant.VariantEventType;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeUtility;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Factory for select expression processors.
 */
public class SelectExprProcessorFactory {
    private static final Logger log = LoggerFactory.getLogger(SelectExprProcessorFactory.class);

    public static SelectExprProcessorDescriptor getProcessor(SelectProcessorArgs args, InsertIntoDesc insertIntoDesc, boolean withSubscriber)
        throws ExprValidationException {

        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        SelectExprProcessorWInsertTarget synthetic = getProcessorInternal(args, insertIntoDesc);
        additionalForgeables.addAll(synthetic.getAdditionalForgeables());

        // plan serdes for variant event types
        if (synthetic.getInsertIntoTargetType() instanceof VariantEventType ||
            synthetic.getInsertIntoTargetType() instanceof WrapperEventType && (((WrapperEventType) synthetic.getInsertIntoTargetType()).getUnderlyingEventType() instanceof VariantEventType)) {
            List<StmtClassForgeableFactory> serdeForgeables = SerdeEventTypeUtility.plan(synthetic.getForge().getResultEventType(), args.getStatementRawInfo(), args.getCompileTimeServices().getSerdeEventTypeRegistry(), args.getCompileTimeServices().getSerdeResolver());
            additionalForgeables.addAll(serdeForgeables);
            for (EventType eventType : args.getTypeService().getEventTypes()) {
                serdeForgeables = SerdeEventTypeUtility.plan(eventType, args.getStatementRawInfo(), args.getCompileTimeServices().getSerdeEventTypeRegistry(), args.getCompileTimeServices().getSerdeResolver());
                additionalForgeables.addAll(serdeForgeables);
            }
        }

        if (args.isFireAndForget() || !withSubscriber) {
            return new SelectExprProcessorDescriptor(new SelectSubscriberDescriptor(), synthetic.getForge(), additionalForgeables);
        }

        // Handle for-clause delivery contract checking
        ExprNode[] groupedDeliveryExpr = null;
        MultiKeyClassRef groupedDeliveryMultiKey = null;
        boolean forDelivery = false;
        if (args.getForClauseSpec() != null) {
            for (ForClauseItemSpec item : args.getForClauseSpec().getClauses()) {
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

                StreamTypeService type = new StreamTypeServiceImpl(synthetic.getForge().getResultEventType(), null, false);
                groupedDeliveryExpr = new ExprNode[item.getExpressions().size()];
                ExprValidationContext validationContext = new ExprValidationContextBuilder(type, args.getStatementRawInfo(), args.getCompileTimeServices()).withAllowBindingConsumption(true).build();
                for (int i = 0; i < item.getExpressions().size(); i++) {
                    groupedDeliveryExpr[i] = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.FORCLAUSE, item.getExpressions().get(i), validationContext);
                }
                forDelivery = true;

                MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(groupedDeliveryExpr, false, args.getStatementRawInfo(), args.getSerdeResolver());
                groupedDeliveryMultiKey = multiKeyPlan.getClassRef();
                additionalForgeables = multiKeyPlan.getMultiKeyForgeables();
            }
            if (groupedDeliveryExpr != null && groupedDeliveryExpr.length == 0) {
                groupedDeliveryExpr = null;
            }
        }

        boolean allowSubscriber = args.getCompileTimeServices().getConfiguration().getCompiler().getByteCode().isAllowSubscriber();
        SelectSubscriberDescriptor descriptor;
        SelectExprProcessorForge forge;

        if (allowSubscriber) {
            BindProcessorForge bindProcessor = new BindProcessorForge(synthetic.getForge(), args.getSelectionList(), args.getTypeService().getEventTypes(), args.getTypeService().getStreamNames(), args.getTableCompileTimeResolver());
            descriptor = new SelectSubscriberDescriptor(bindProcessor.getExpressionTypes(), bindProcessor.getColumnNamesAssigned(), forDelivery, groupedDeliveryExpr, groupedDeliveryMultiKey);
            forge = new BindSelectExprProcessorForge(synthetic.getForge(), bindProcessor);
        } else {
            descriptor = new SelectSubscriberDescriptor();
            forge = new ListenerOnlySelectExprProcessorForge(synthetic.getForge());
        }

        return new SelectExprProcessorDescriptor(descriptor, forge, additionalForgeables);
    }

    private static SelectExprProcessorWInsertTarget getProcessorInternal(SelectProcessorArgs args, InsertIntoDesc insertIntoDesc)
        throws ExprValidationException {
        // Wildcard not allowed when insert into specifies column order
        if (args.isUsingWildcard() && insertIntoDesc != null && !insertIntoDesc.getColumnNames().isEmpty()) {
            throw new ExprValidationException("Wildcard not allowed when insert-into specifies column order");
        }

        EventType insertIntoTarget = insertIntoDesc == null ? null : args.getEventTypeCompileTimeResolver().getTypeByName(insertIntoDesc.getEventTypeName());

        // Determine wildcard processor (select *)
        if (isWildcardsOnly(args.getSelectionList())) {
            // For joins
            if (args.getTypeService().getStreamNames().length > 1 && (!(insertIntoTarget instanceof VariantEventType))) {
                log.debug(".getProcessor Using SelectExprJoinWildcardProcessor");
                SelectExprProcessorForgeWForgables pair = SelectExprJoinWildcardProcessorFactory.create(args, insertIntoDesc, eventTypeName -> eventTypeName);
                SelectExprProcessorForge forge = pair.getForge();
                return new SelectExprProcessorWInsertTarget(forge, null, pair.getAdditionalForgeables());
            } else if (insertIntoDesc == null) {
                // Single-table selects with no insert-into
                // don't need extra processing
                log.debug(".getProcessor Using wildcard processor");
                if (args.getTypeService().hasTableTypes()) {
                    TableMetaData table = args.getTableCompileTimeResolver().resolveTableFromEventType(args.getTypeService().getEventTypes()[0]);
                    if (table != null) {
                        SelectExprProcessorForge forge = new SelectEvalWildcardTable(table);
                        return new SelectExprProcessorWInsertTarget(forge, null, Collections.emptyList());
                    }
                }
                SelectExprProcessorForge forge = new SelectEvalWildcardNonJoin(args.getTypeService().getEventTypes()[0]);
                return new SelectExprProcessorWInsertTarget(forge, null, Collections.emptyList());
            }
        }

        // Verify the assigned or name used is unique
        if (insertIntoDesc == null) {
            verifyNameUniqueness(args.getSelectionList());
        }

        // Construct processor
        SelectExprBuckets buckets = getSelectExpressionBuckets(args.getSelectionList());

        SelectExprProcessorHelper factory = new SelectExprProcessorHelper(buckets.expressions, buckets.selectedStreams, args, insertIntoDesc);
        return factory.getForge();
    }

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
        if (!dotNode.getChainSpec().isEmpty() && dotNode.getChainSpec().get(0).getName().toLowerCase(Locale.ENGLISH).equals(ClasspathImportServiceCompileTime.EXT_SINGLEROW_FUNCTION_TRANSPOSE)) {
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
