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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.faf.StmtClassForgeableQueryMethodProvider;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.StatementLifecycleSvcUtil;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessorForge;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessorForgeFactory;
import com.espertech.esper.common.internal.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.subselect.*;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalHelperPlan;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyUtil;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperValidate;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.annotation.AnnotationUtil.makeAnnotations;

/**
 * Starts and provides the stop method for EPL statements.
 */
public abstract class FAFQueryMethodIUDBaseForge implements FAFQueryMethodForge {

    protected final FireAndForgetProcessorForge processor;
    protected final ExprNode whereClause;
    protected final QueryGraphForge queryGraph;
    protected final Annotation[] annotations;
    protected boolean hasTableAccess;
    protected final Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccessForges;
    private final Map<ExprSubselectNode, SubSelectFactoryForge> subselectForges;
    private final List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

    protected abstract void initExec(String aliasName, StatementSpecCompiled spec, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException;

    protected abstract Class typeOfMethod();

    protected abstract void makeInlineSpecificSetter(CodegenExpressionRef queryMethod, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public FAFQueryMethodIUDBaseForge(StatementSpecCompiled spec, Compilable compilable, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        this.annotations = spec.getAnnotations();
        this.hasTableAccess = spec.getRaw().getIntoTableSpec() != null ||
            (spec.getTableAccessNodes() != null && spec.getTableAccessNodes().size() > 0);
        if (spec.getRaw().getInsertIntoDesc() != null && services.getTableCompileTimeResolver().resolve(spec.getRaw().getInsertIntoDesc().getEventTypeName()) != null) {
            hasTableAccess = true;
        }
        if (spec.getRaw().getFireAndForgetSpec() instanceof FireAndForgetSpecUpdate ||
            spec.getRaw().getFireAndForgetSpec() instanceof FireAndForgetSpecDelete) {
            hasTableAccess |= spec.getStreamSpecs()[0] instanceof TableQueryStreamSpec;
        }
        hasTableAccess |= StatementLifecycleSvcUtil.isSubqueryWithTable(spec.getSubselectNodes(), services.getTableCompileTimeResolver());

        // validate general FAF criteria
        FAFQueryMethodHelper.validateFAFQuery(spec);

        // obtain processor
        StreamSpecCompiled streamSpec = spec.getStreamSpecs()[0];
        processor = FireAndForgetProcessorForgeFactory.validateResolveProcessor(streamSpec);

        // obtain name and type
        String processorName = processor.getNamedWindowOrTableName();
        EventType eventType = processor.getEventTypeRSPInputEvents();

        // determine alias
        String aliasName = processorName;
        if (streamSpec.getOptionalStreamName() != null) {
            aliasName = streamSpec.getOptionalStreamName();
        }

        // activate subselect activations
        StatementBaseInfo base = new StatementBaseInfo(compilable, spec, null, statementRawInfo, null);
        List<NamedWindowConsumerStreamSpec> subqueryNamedWindowConsumers = new ArrayList<>();
        SubSelectActivationDesc subSelectActivationDesc = SubSelectHelperActivations.createSubSelectActivation(Collections.emptyList(), subqueryNamedWindowConsumers, base, services);
        Map<ExprSubselectNode, SubSelectActivationPlan> subselectActivation = subSelectActivationDesc.getSubselects();
        additionalForgeables.addAll(subSelectActivationDesc.getAdditionalForgeables());

        // plan subselects
        String[] namesPerStream = new String[] {aliasName};
        EventType[] typesPerStream = new EventType[] {processor.getEventTypePublic()};
        String[] eventTypeNames = new String[] {typesPerStream[0].getName()};
        SubSelectHelperForgePlan subSelectForgePlan = SubSelectHelperForgePlanner.planSubSelect(base, subselectActivation, namesPerStream, typesPerStream, eventTypeNames, services);
        subselectForges = subSelectForgePlan.getSubselects();
        additionalForgeables.addAll(subSelectForgePlan.getAdditionalForgeables());

        // compile filter to optimize access to named window
        StreamTypeServiceImpl typeService = new StreamTypeServiceImpl(new EventType[]{eventType}, new String[]{aliasName}, new boolean[]{true}, true, false);
        ExcludePlanHint excludePlanHint = ExcludePlanHint.getHint(typeService.getStreamNames(), statementRawInfo, services);
        if (spec.getRaw().getWhereClause() != null) {
            queryGraph = new QueryGraphForge(1, excludePlanHint, false);
            EPLValidationUtil.validateFilterWQueryGraphSafe(queryGraph, spec.getRaw().getWhereClause(), typeService, statementRawInfo, services);
        } else {
            queryGraph = null;
        }

        // validate expressions
        whereClause = EPStatementStartMethodHelperValidate.validateNodes(spec.getRaw(), typeService, null, statementRawInfo, services);

        // get executor
        initExec(aliasName, spec, statementRawInfo, services);

        // plan table access
        tableAccessForges = ExprTableEvalHelperPlan.planTableAccess(spec.getRaw().getTableExpressions());
    }

    public final List<StmtClassForgeable> makeForgeables(String queryMethodProviderClassName, String classPostfix, CodegenPackageScope packageScope) {
        List<StmtClassForgeable> forgeables = new ArrayList<>();
        for (StmtClassForgeableFactory additional : additionalForgeables) {
            forgeables.add(additional.make(packageScope, classPostfix));
        }
        forgeables.add(new StmtClassForgeableQueryMethodProvider(queryMethodProviderClassName, packageScope, this));
        return forgeables;
    }

    public final void makeMethod(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpressionRef queryMethod = ref("qm");
        method.getBlock()
            .declareVar(typeOfMethod(), queryMethod.getRef(), newInstance(typeOfMethod()))
            .exprDotMethod(queryMethod, "setAnnotations", annotations == null ? constantNull() : localMethod(makeAnnotations(Annotation[].class, annotations, method, classScope)))
            .exprDotMethod(queryMethod, "setProcessor", processor.make(method, symbols, classScope))
            .exprDotMethod(queryMethod, "setQueryGraph", queryGraph == null ? constantNull() : queryGraph.make(method, symbols, classScope))
            .exprDotMethod(queryMethod, "setInternalEventRouteDest", exprDotMethod(symbols.getAddInitSvc(method), EPStatementInitServices.GETINTERNALEVENTROUTEDEST))
            .exprDotMethod(queryMethod, "setTableAccesses", ExprTableEvalStrategyUtil.codegenInitMap(tableAccessForges, this.getClass(), method, symbols, classScope))
            .exprDotMethod(queryMethod, "setHasTableAccess", constant(hasTableAccess))
            .exprDotMethod(queryMethod, "setSubselects", SubSelectFactoryForge.codegenInitMap(subselectForges, this.getClass(), method, symbols, classScope));
        makeInlineSpecificSetter(queryMethod, method, symbols, classScope);
        method.getBlock().methodReturn(queryMethod);
    }

}
