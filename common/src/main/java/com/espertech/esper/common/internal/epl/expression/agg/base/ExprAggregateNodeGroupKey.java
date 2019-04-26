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
package com.espertech.esper.common.internal.epl.expression.agg.base;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.util.MultiKey;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldName;
import com.espertech.esper.common.internal.collection.MultiKeyArrayWrap;
import com.espertech.esper.common.internal.epl.agg.core.AggregationResultFuture;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprAggregateNodeGroupKey extends ExprNodeBase implements ExprForge, ExprEvaluator {
    private final int numGroupKeys;
    private final int groupKeyIndex;
    private final Class returnType;
    private final CodegenFieldName aggregationResultFutureMemberName;

    public ExprAggregateNodeGroupKey(int numGroupKeys, int groupKeyIndex, Class returnType, CodegenFieldName aggregationResultFutureMemberName) {
        this.numGroupKeys = numGroupKeys;
        this.groupKeyIndex = groupKeyIndex;
        this.returnType = returnType;
        this.aggregationResultFutureMemberName = aggregationResultFutureMemberName;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbol, CodegenClassScope classScope) {
        CodegenExpression future = classScope.getPackageScope().addOrGetFieldWellKnown(aggregationResultFutureMemberName, AggregationResultFuture.class);
        CodegenMethod method = parent.makeChild(returnType, this.getClass(), classScope);
        method.getBlock()
            .declareVar(Object.class, "key", exprDotMethod(future, "getGroupKey", exprDotMethod(symbol.getAddExprEvalCtx(method), "getAgentInstanceId")));

        method.getBlock().ifCondition(instanceOf(ref("key"), MultiKey.class))
            .declareVar(MultiKey.class, "mk", cast(MultiKey.class, ref("key")))
            .blockReturn(CodegenLegoCast.castSafeFromObjectType(returnType, exprDotMethod(ref("mk"), "getKey", constant(groupKeyIndex))));

        method.getBlock().ifCondition(instanceOf(ref("key"), MultiKeyArrayWrap.class))
            .declareVar(MultiKeyArrayWrap.class, "mk", cast(MultiKeyArrayWrap.class, ref("key")))
            .blockReturn(CodegenLegoCast.castSafeFromObjectType(returnType, exprDotMethod(ref("mk"), "getArray")));

        method.getBlock().methodReturn(CodegenLegoCast.castSafeFromObjectType(returnType, ref("key")));
        return localMethod(method);
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public String toExpressionString(ExprPrecedenceEnum precedence) {
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return false;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // not required
        return null;
    }
}
