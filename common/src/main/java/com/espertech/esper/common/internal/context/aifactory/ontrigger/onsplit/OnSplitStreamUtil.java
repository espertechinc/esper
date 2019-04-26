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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.onsplit;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.*;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableRSPFactoryProvider;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.OnTriggerActivatorDesc;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.StmtClassForgeableAIFactoryProviderOnTrigger;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.ontrigger.OnTriggerPlan;
import com.espertech.esper.common.internal.context.util.ContextPropertyRegistry;
import com.espertech.esper.common.internal.epl.contained.PropertyEvaluatorForge;
import com.espertech.esper.common.internal.epl.contained.PropertyEvaluatorForgeFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorDesc;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryProvider;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetSpec;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.subselect.SubSelectActivationPlan;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryForge;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperForgePlan;
import com.espertech.esper.common.internal.epl.subselect.SubSelectHelperForgePlanner;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalHelperPlan;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperValidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OnSplitStreamUtil {
    public static OnTriggerPlan handleSplitStream(String aiFactoryProviderClassName, CodegenPackageScope packageScope, String classPostfix, OnTriggerSplitStreamDesc desc, StreamSpecCompiled streamSpec, OnTriggerActivatorDesc activatorResult, Map<ExprSubselectNode, SubSelectActivationPlan> subselectActivation, StatementBaseInfo base, StatementCompileTimeServices services) throws ExprValidationException {
        StatementSpecRaw raw = base.getStatementSpec().getRaw();
        if (raw.getInsertIntoDesc() == null) {
            throw new ExprValidationException("Required insert-into clause is not provided, the clause is required for split-stream syntax");
        }
        if ((raw.getGroupByExpressions() != null && raw.getGroupByExpressions().size() > 0) || (raw.getHavingClause() != null) || (raw.getOrderByList().size() > 0)) {
            throw new ExprValidationException("A group-by clause, having-clause or order-by clause is not allowed for the split stream syntax");
        }

        String streamName = streamSpec.getOptionalStreamName();
        if (streamName == null) {
            streamName = "stream_0";
        }
        StreamTypeService typeServiceTrigger = new StreamTypeServiceImpl(new EventType[]{activatorResult.getActivatorResultEventType()}, new String[]{streamName}, new boolean[]{true}, false, false);

        // materialize sub-select views
        SubSelectHelperForgePlan subselectForgePlan = SubSelectHelperForgePlanner.planSubSelect(base, subselectActivation, new String[]{streamSpec.getOptionalStreamName()}, new EventType[]{activatorResult.getActivatorResultEventType()}, new String[]{activatorResult.getTriggerEventTypeName()}, services);
        Map<ExprSubselectNode, SubSelectFactoryForge> subselectForges = subselectForgePlan.getSubselects();

        // compile top-level split
        OnSplitItemForge[] items = new OnSplitItemForge[desc.getSplitStreams().size() + 1];
        items[0] = onSplitValidate(typeServiceTrigger, base.getStatementSpec(), base.getContextPropertyRegistry(), null, base.getStatementRawInfo(), services);

        // compile each additional split
        int index = 1;
        for (OnTriggerSplitStream splits : desc.getSplitStreams()) {
            StatementSpecCompiled splitSpec = new StatementSpecCompiled();

            splitSpec.getRaw().setInsertIntoDesc(splits.getInsertInto());
            splitSpec.setSelectClauseCompiled(compileSelectAllowSubselect(splits.getSelectClause()));
            splitSpec.getRaw().setWhereClause(splits.getWhereClause());

            PropertyEvaluatorForge optionalPropertyEvaluator = null;
            StreamTypeService typeServiceProperty;
            if (splits.getFromClause() != null) {
                optionalPropertyEvaluator = PropertyEvaluatorForgeFactory.makeEvaluator(splits.getFromClause().getPropertyEvalSpec(), activatorResult.getActivatorResultEventType(), streamName, base.getStatementRawInfo(), services);
                typeServiceProperty = new StreamTypeServiceImpl(new EventType[]{optionalPropertyEvaluator.getFragmentEventType()}, new String[]{splits.getFromClause().getOptionalStreamName()}, new boolean[]{true}, false, false);
            } else {
                typeServiceProperty = typeServiceTrigger;
            }

            items[index] = onSplitValidate(typeServiceProperty, splitSpec, base.getContextPropertyRegistry(), optionalPropertyEvaluator, base.getStatementRawInfo(), services);
            index++;
        }

        // handle result set processor classes
        List<StmtClassForgeable> forgeables = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            String classNameRSP = CodeGenerationIDGenerator.generateClassNameSimple(ResultSetProcessorFactoryProvider.class, classPostfix + "_" + i);
            forgeables.add(new StmtClassForgeableRSPFactoryProvider(classNameRSP, items[i].getResultSetProcessorDesc(), packageScope, base.getStatementRawInfo()));
            items[i].setResultSetProcessorClassName(classNameRSP);
        }

        // plan table access
        Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccessForges = ExprTableEvalHelperPlan.planTableAccess(base.getStatementSpec().getTableAccessNodes());

        // build forge
        StatementAgentInstanceFactoryOnTriggerSplitStreamForge splitStreamForge = new StatementAgentInstanceFactoryOnTriggerSplitStreamForge(activatorResult.getActivator(),
            activatorResult.getActivatorResultEventType(), subselectForges, tableAccessForges, items, desc.isFirst());
        StmtClassForgeableAIFactoryProviderOnTrigger triggerForge = new StmtClassForgeableAIFactoryProviderOnTrigger(aiFactoryProviderClassName, packageScope, splitStreamForge);

        return new OnTriggerPlan(triggerForge, forgeables, new SelectSubscriberDescriptor(), subselectForgePlan.getAdditionalForgeables());
    }

    private static OnSplitItemForge onSplitValidate(StreamTypeService typeServiceTrigger, StatementSpecCompiled statementSpecCompiled, ContextPropertyRegistry contextPropertyRegistry, PropertyEvaluatorForge optionalPropertyEval, StatementRawInfo rawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        String insertIntoName = statementSpecCompiled.getRaw().getInsertIntoDesc().getEventTypeName();
        boolean isNamedWindowInsert = services.getNamedWindowCompileTimeResolver().resolve(insertIntoName) != null;
        TableMetaData table = services.getTableCompileTimeResolver().resolve(insertIntoName);
        EPStatementStartMethodHelperValidate.validateNodes(statementSpecCompiled.getRaw(), typeServiceTrigger, null, rawInfo, services);
        ResultSetSpec spec = new ResultSetSpec(statementSpecCompiled);
        ResultSetProcessorDesc factoryDescs = ResultSetProcessorFactoryFactory.getProcessorPrototype(spec, typeServiceTrigger,
            null, new boolean[0], false, contextPropertyRegistry, false, true, rawInfo, services);
        return new OnSplitItemForge(statementSpecCompiled.getRaw().getWhereClause(), isNamedWindowInsert, table, factoryDescs, optionalPropertyEval);
    }

    /**
     * Compile a select clause allowing subselects.
     *
     * @param spec to compile
     * @return select clause compiled
     * @throws ExprValidationException when validation fails
     */
    private static SelectClauseSpecCompiled compileSelectAllowSubselect(SelectClauseSpecRaw spec) throws ExprValidationException {
        // Look for expressions with sub-selects in select expression list and filter expression
        // Recursively compile the statement within the statement.
        ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
        List<SelectClauseElementCompiled> selectElements = new ArrayList<SelectClauseElementCompiled>();
        for (SelectClauseElementRaw raw : spec.getSelectExprList()) {
            if (raw instanceof SelectClauseExprRawSpec) {
                SelectClauseExprRawSpec rawExpr = (SelectClauseExprRawSpec) raw;
                rawExpr.getSelectExpression().accept(visitor);
                selectElements.add(new SelectClauseExprCompiledSpec(rawExpr.getSelectExpression(), rawExpr.getOptionalAsName(), rawExpr.getOptionalAsName(), rawExpr.isEvents()));
            } else if (raw instanceof SelectClauseStreamRawSpec) {
                SelectClauseStreamRawSpec rawExpr = (SelectClauseStreamRawSpec) raw;
                selectElements.add(new SelectClauseStreamCompiledSpec(rawExpr.getStreamName(), rawExpr.getOptionalAsName()));
            } else if (raw instanceof SelectClauseElementWildcard) {
                SelectClauseElementWildcard wildcard = (SelectClauseElementWildcard) raw;
                selectElements.add(wildcard);
            } else {
                throw new IllegalStateException("Unexpected select clause element class : " + raw.getClass().getName());
            }
        }
        return new SelectClauseSpecCompiled(selectElements.toArray(new SelectClauseElementCompiled[selectElements.size()]), spec.isDistinct());
    }
}
