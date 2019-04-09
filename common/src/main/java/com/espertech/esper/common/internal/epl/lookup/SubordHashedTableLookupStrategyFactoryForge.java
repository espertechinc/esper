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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.join.queryplan.CoercionDesc;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropHashKeyForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.util.IntArrayUtil;

import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordHashedTableLookupStrategyFactoryForge implements SubordTableLookupStrategyFactoryForge {
    private final boolean isNWOnTrigger;
    private final int numStreamsOuter;
    private final List<SubordPropHashKeyForge> hashKeys;
    private final CoercionDesc hashKeyCoercionTypes;
    private final boolean isStrictKeys;
    private final String[] hashStrictKeys;
    private final int[] keyStreamNumbers;
    private final EventType[] outerStreamTypesZeroIndexed;
    private final MultiKeyClassRef hashMultikeyClasses;

    public SubordHashedTableLookupStrategyFactoryForge(boolean isNWOnTrigger, int numStreamsOuter, List<SubordPropHashKeyForge> hashKeys, CoercionDesc hashKeyCoercionTypes, boolean isStrictKeys, String[] hashStrictKeys, int[] keyStreamNumbers, EventType[] outerStreamTypesZeroIndexed, MultiKeyClassRef hashMultikeyClasses) {
        this.isNWOnTrigger = isNWOnTrigger;
        this.numStreamsOuter = numStreamsOuter;
        this.hashKeys = hashKeys;
        this.hashKeyCoercionTypes = hashKeyCoercionTypes;
        this.isStrictKeys = isStrictKeys;
        this.hashStrictKeys = hashStrictKeys;
        this.keyStreamNumbers = keyStreamNumbers;
        this.outerStreamTypesZeroIndexed = outerStreamTypesZeroIndexed;
        this.hashMultikeyClasses = hashMultikeyClasses;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " expressions " + Arrays.toString(getExpressions());
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod methodNode = parent.makeChild(SubordTableLookupStrategyFactory.class, this.getClass(), classScope);
        if (isStrictKeys) {
            int[] keyStreamNums = IntArrayUtil.copy(keyStreamNumbers);
            EventType[] keyStreamTypes = outerStreamTypesZeroIndexed;
            if (isNWOnTrigger) {
                keyStreamTypes = EventTypeUtility.shiftRight(outerStreamTypesZeroIndexed);
                for (int i = 0; i < keyStreamNums.length; i++) {
                    keyStreamNums[i] = keyStreamNums[i] + 1;
                }
            }
            ExprForge[] forges = ExprNodeUtilityQuery.forgesForProperties(keyStreamTypes, hashStrictKeys, keyStreamNums);
            CodegenExpression eval = MultiKeyCodegen.codegenExprEvaluatorMayMultikey(forges, hashKeyCoercionTypes.getCoercionTypes(), hashMultikeyClasses, methodNode, classScope);
            methodNode.getBlock().methodReturn(newInstance(SubordHashedTableLookupStrategyPropFactory.class, constant(hashStrictKeys), constant(keyStreamNums), eval));
            return localMethod(methodNode);
        } else {
            ExprForge[] forges = new ExprForge[hashKeys.size()];
            for (int i = 0; i < hashKeys.size(); i++) {
                forges[i] = hashKeys.get(i).getHashKey().getKeyExpr().getForge();
            }

            String[] expressions = ExprNodeUtilityPrint.toExpressionStringsMinPrecedence(forges);
            CodegenExpression eval = MultiKeyCodegen.codegenExprEvaluatorMayMultikey(forges, hashKeyCoercionTypes.getCoercionTypes(), hashMultikeyClasses, methodNode, classScope);
            methodNode.getBlock().methodReturn(newInstance(SubordHashedTableLookupStrategyExprFactory.class, constant(expressions), eval, constant(isNWOnTrigger), constant(numStreamsOuter)));
            return localMethod(methodNode);
        }
    }

    private String[] getExpressions() {
        String[] expressions = new String[hashKeys.size()];
        for (int i = 0; i < hashKeys.size(); i++) {
            expressions[i] = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(hashKeys.get(i).getHashKey().getKeyExpr());
        }
        return expressions;
    }
}
