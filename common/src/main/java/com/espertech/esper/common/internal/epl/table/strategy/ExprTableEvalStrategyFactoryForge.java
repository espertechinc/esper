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
package com.espertech.esper.common.internal.epl.table.strategy;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationMethodForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprEnumerationGivenEventForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprTableEvalStrategyFactoryForge {

    private final TableMetaData tableMeta;
    private final ExprForge[] optionalGroupKeys;
    private ExprTableEvalStrategyEnum strategyEnum;
    private int aggColumnNum = -1;
    private int propertyIndex = -1;
    private ExprEnumerationGivenEventForge optionalEnumEval;
    private AggregationMethodForge aggregationMethod;

    public ExprTableEvalStrategyFactoryForge(TableMetaData tableMeta, ExprForge[] optionalGroupKeys) {
        this.tableMeta = tableMeta;
        this.optionalGroupKeys = optionalGroupKeys;
    }

    public void setStrategyEnum(ExprTableEvalStrategyEnum strategyEnum) {
        this.strategyEnum = strategyEnum;
    }

    public void setPropertyIndex(int propertyIndex) {
        this.propertyIndex = propertyIndex;
    }

    public void setOptionalEnumEval(ExprEnumerationGivenEventForge optionalEnumEval) {
        this.optionalEnumEval = optionalEnumEval;
    }

    public void setAggColumnNum(int aggColumnNum) {
        this.aggColumnNum = aggColumnNum;
    }

    public void setAggregationMethod(AggregationMethodForge aggregationMethod) {
        this.aggregationMethod = aggregationMethod;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ExprTableEvalStrategyFactory.class, this.getClass(), classScope);

        CodegenExpression groupKeyEval = constantNull();
        if (optionalGroupKeys != null && optionalGroupKeys.length > 0) {
            groupKeyEval = MultiKeyCodegen.codegenEvaluatorReturnObjectOrArrayWCoerce(optionalGroupKeys, tableMeta.getKeyTypes(), true, method, this.getClass(), classScope);
        }

        method.getBlock()
            .declareVar(ExprTableEvalStrategyFactory.class, "factory", newInstance(ExprTableEvalStrategyFactory.class))
            .exprDotMethod(ref("factory"), "setStrategyEnum", constant(strategyEnum))
            .exprDotMethod(ref("factory"), "setTable", TableDeployTimeResolver.makeResolveTable(tableMeta, symbols.getAddInitSvc(method)))
            .exprDotMethod(ref("factory"), "setGroupKeyEval", groupKeyEval)
            .exprDotMethod(ref("factory"), "setAggColumnNum", constant(aggColumnNum))
            .exprDotMethod(ref("factory"), "setPropertyIndex", constant(propertyIndex))
            .exprDotMethod(ref("factory"), "setOptionalEnumEval", optionalEnumEval == null ? constantNull() : ExprNodeUtilityCodegen.codegenExprEnumEval(optionalEnumEval, method, symbols, classScope, this.getClass()))
            .exprDotMethod(ref("factory"), "setAggregationMethod", aggregationMethod == null ? constantNull() : aggregationMethod.codegenCreateReader(method, symbols, classScope))
            .methodReturn(ref("factory"));
        return localMethod(method);
    }
}
