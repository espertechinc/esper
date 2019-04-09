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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScopeNames;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationClassNames;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryCompiler;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryMakeResult;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceForgeDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.index.base.EventTableFactoryFactoryForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowDeployTimeResolver;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateDesc;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.core.ViewFactoryForgeUtil;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubSelectStrategyFactoryLocalViewPreloadedForge implements SubSelectStrategyFactoryForge {
    private final List<ViewFactoryForge> viewForges;
    private final ViewResourceDelegateDesc viewResourceDelegateDesc;
    private final Pair<EventTableFactoryFactoryForge, SubordTableLookupStrategyFactoryForge> lookupStrategy;
    private final ExprNode filterExprNode;
    private final boolean correlatedSubquery;
    private final AggregationServiceForgeDesc aggregationServiceForgeDesc;
    private final int subqueryNumber;
    private final ExprNode[] groupKeys;
    private final NamedWindowMetaData namedWindow;
    private final ExprNode namedWindowFilterExpr;
    private final QueryGraphForge namedWindowFilterQueryGraph;
    private final MultiKeyClassRef groupByMultiKeyClasses;

    public SubSelectStrategyFactoryLocalViewPreloadedForge(List<ViewFactoryForge> viewForges, ViewResourceDelegateDesc viewResourceDelegateDesc, Pair<EventTableFactoryFactoryForge, SubordTableLookupStrategyFactoryForge> lookupStrategy, ExprNode filterExprNode, boolean correlatedSubquery, AggregationServiceForgeDesc aggregationServiceForgeDesc, int subqueryNumber, ExprNode[] groupKeys, NamedWindowMetaData namedWindow, ExprNode namedWindowFilterExpr, QueryGraphForge namedWindowFilterQueryGraph, MultiKeyClassRef groupByMultiKeyClasses) {
        this.viewForges = viewForges;
        this.viewResourceDelegateDesc = viewResourceDelegateDesc;
        this.lookupStrategy = lookupStrategy;
        this.filterExprNode = filterExprNode;
        this.correlatedSubquery = correlatedSubquery;
        this.aggregationServiceForgeDesc = aggregationServiceForgeDesc;
        this.subqueryNumber = subqueryNumber;
        this.groupKeys = groupKeys;
        this.namedWindow = namedWindow;
        this.namedWindowFilterExpr = namedWindowFilterExpr;
        this.namedWindowFilterQueryGraph = namedWindowFilterQueryGraph;
        this.groupByMultiKeyClasses = groupByMultiKeyClasses;
    }

    public List<ViewFactoryForge> getViewForges() {
        return viewForges;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(SubSelectStrategyFactoryLocalViewPreloaded.class, this.getClass(), classScope);

        CodegenExpression groupKeyEval = MultiKeyCodegen.codegenExprEvaluatorMayMultikey(groupKeys, null, groupByMultiKeyClasses, method, classScope);

        method.getBlock()
            .declareVar(SubSelectStrategyFactoryLocalViewPreloaded.class, "factory", newInstance(SubSelectStrategyFactoryLocalViewPreloaded.class))
            .exprDotMethod(ref("factory"), "setSubqueryNumber", constant(subqueryNumber))
            .exprDotMethod(ref("factory"), "setViewFactories", ViewFactoryForgeUtil.codegenForgesWInit(viewForges, 0, subqueryNumber, method, symbols, classScope))
            .exprDotMethod(ref("factory"), "setViewResourceDelegate", viewResourceDelegateDesc.toExpression())
            .exprDotMethod(ref("factory"), "setEventTableFactoryFactory", lookupStrategy.getFirst().make(method, symbols, classScope))
            .exprDotMethod(ref("factory"), "setLookupStrategyFactory", lookupStrategy.getSecond().make(method, symbols, classScope))
            .exprDotMethod(ref("factory"), "setAggregationServiceFactory", makeAggregationService(subqueryNumber, aggregationServiceForgeDesc, classScope, method, symbols))
            .exprDotMethod(ref("factory"), "setCorrelatedSubquery", constant(correlatedSubquery))
            .exprDotMethod(ref("factory"), "setGroupKeyEval", groupKeyEval)
            .exprDotMethod(ref("factory"), "setFilterExprEval", filterExprNode == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluatorNoCoerce(filterExprNode.getForge(), method, this.getClass(), classScope));
        if (namedWindow != null) {
            method.getBlock().exprDotMethod(ref("factory"), "setNamedWindow", NamedWindowDeployTimeResolver.makeResolveNamedWindow(namedWindow, symbols.getAddInitSvc(method)));
            if (namedWindowFilterExpr != null) {
                method.getBlock()
                    .exprDotMethod(ref("factory"), "setNamedWindowFilterQueryGraph", namedWindowFilterQueryGraph.make(method, symbols, classScope))
                    .exprDotMethod(ref("factory"), "setNamedWindowFilterExpr", ExprNodeUtilityCodegen.codegenEvaluator(namedWindowFilterExpr.getForge(), method, this.getClass(), classScope));
            }
        }
        method.getBlock().methodReturn(ref("factory"));
        return localMethod(method);
    }

    public boolean hasAggregation() {
        return aggregationServiceForgeDesc != null;
    }

    public boolean hasPrior() {
        return viewResourceDelegateDesc.getPriorRequests() != null && !viewResourceDelegateDesc.getPriorRequests().isEmpty();
    }

    public boolean hasPrevious() {
        return viewResourceDelegateDesc.isHasPrevious();
    }

    protected static CodegenExpression makeAggregationService(int subqueryNumber, AggregationServiceForgeDesc aggregationServiceForgeDesc, CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbol symbols) {
        if (aggregationServiceForgeDesc == null) {
            return constantNull();
        }

        AggregationClassNames aggregationClassNames = new AggregationClassNames(CodegenPackageScopeNames.classPostfixAggregationForSubquery(subqueryNumber));
        AggregationServiceFactoryMakeResult aggResult = AggregationServiceFactoryCompiler.makeInnerClassesAndInit(false, aggregationServiceForgeDesc.getAggregationServiceFactoryForge(), parent, classScope, classScope.getOutermostClassName(), aggregationClassNames);
        classScope.addInnerClasses(aggResult.getInnerClasses());
        return localMethod(aggResult.getInitMethod(), symbols.getAddInitSvc(parent));
    }
}
