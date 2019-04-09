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
package com.espertech.esper.common.internal.epl.lookup;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropHashKeyForge;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropRangeKeyForge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordCompositeTableLookupStrategyFactoryForge implements SubordTableLookupStrategyFactoryForge {
    private final boolean isNWOnTrigger;
    private final int numStreams;
    private final List<SubordPropHashKeyForge> hashKeys;
    private final Class[] hashTypes;
    private final MultiKeyClassRef hashMultikeyClasses;
    private final List<SubordPropRangeKeyForge> rangeProps;
    private final Class[] coercionRangeTypes;

    public SubordCompositeTableLookupStrategyFactoryForge(boolean isNWOnTrigger, int numStreams, List<SubordPropHashKeyForge> keyExpr, Class[] coercionKeyTypes, MultiKeyClassRef hashMultikeyClasses, List<SubordPropRangeKeyForge> rangeProps, Class[] coercionRangeTypes) {
        this.isNWOnTrigger = isNWOnTrigger;
        this.numStreams = numStreams;
        this.hashKeys = keyExpr;
        this.hashTypes = coercionKeyTypes;
        this.hashMultikeyClasses = hashMultikeyClasses;
        this.rangeProps = rangeProps;
        this.coercionRangeTypes = coercionRangeTypes;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(SubordCompositeTableLookupStrategyFactory.class, this.getClass(), classScope);

        List<String> expressions = new ArrayList<>();
        CodegenExpression hashEval = constantNull();
        if (hashKeys != null && !hashKeys.isEmpty()) {
            ExprForge[] forges = new ExprForge[hashKeys.size()];
            for (int i = 0; i < hashKeys.size(); i++) {
                forges[i] = hashKeys.get(i).getHashKey().getKeyExpr().getForge();
            }
            expressions.addAll(Arrays.asList(ExprNodeUtilityPrint.toExpressionStringsMinPrecedence(forges)));
            hashEval = MultiKeyCodegen.codegenExprEvaluatorMayMultikey(forges, hashTypes, hashMultikeyClasses, method, classScope);
        }

        method.getBlock().declareVar(QueryGraphValueEntryRange[].class, "rangeEvals", newArrayByLength(QueryGraphValueEntryRange.class, constant(rangeProps.size())));
        for (int i = 0; i < rangeProps.size(); i++) {
            CodegenExpression rangeEval = rangeProps.get(i).getRangeInfo().make(coercionRangeTypes[i], parent, symbols, classScope);
            method.getBlock().assignArrayElement(ref("rangeEvals"), constant(i), rangeEval);
        }

        method.getBlock().methodReturn(newInstance(SubordCompositeTableLookupStrategyFactory.class,
            constant(isNWOnTrigger), constant(numStreams), constant(expressions.toArray(new String[0])),
            hashEval, ref("rangeEvals")));
        return localMethod(method);
    }
}
