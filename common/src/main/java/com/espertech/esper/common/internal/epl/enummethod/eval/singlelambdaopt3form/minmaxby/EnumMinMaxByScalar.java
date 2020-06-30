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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.minmaxby;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormScalar;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LT;

public class EnumMinMaxByScalar extends ThreeFormScalar {

    protected final boolean max;
    protected final EPChainableType resultType;
    private final EPTypeClass innerTypeBoxed;
    private final EPTypeClass resultTypeBoxed;

    public EnumMinMaxByScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldEventType, int numParameters, boolean max, EPChainableType resultType) {
        super(lambda, fieldEventType, numParameters);
        this.max = max;
        this.resultType = resultType;
        this.innerTypeBoxed = (EPTypeClass) JavaClassHelper.getBoxedType(innerExpression.getEvaluationType());
        this.resultTypeBoxed = JavaClassHelper.getBoxedType(EPChainableTypeHelper.getCodegenReturnType(resultType));
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                Comparable minKey = null;
                Object result = null;
                ObjectArrayEventBean resultEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
                eventsLambda[getStreamNumLambda()] = resultEvent;
                Object[] props = resultEvent.getProperties();
                props[2] = enumcoll.size();
                Collection<Object> values = (Collection<Object>) enumcoll;

                int count = -1;
                for (Object next : values) {
                    count++;
                    props[1] = count;
                    props[0] = next;

                    Object comparable = inner.evaluate(eventsLambda, isNewData, context);
                    if (comparable == null) {
                        continue;
                    }

                    if (minKey == null) {
                        minKey = (Comparable) comparable;
                        result = next;
                    } else {
                        if (max) {
                            if (minKey.compareTo(comparable) < 0) {
                                minKey = (Comparable) comparable;
                                result = next;
                            }
                        } else {
                            if (minKey.compareTo(comparable) > 0) {
                                minKey = (Comparable) comparable;
                                result = next;
                            }
                        }
                    }
                }

                return result;
            }
        };
    }

    public EPTypeClass returnTypeOfMethod() {
        return resultTypeBoxed;
    }

    public CodegenExpression returnIfEmptyOptional() {
        return constantNull();
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(innerTypeBoxed, "minKey", constantNull())
            .declareVar(resultTypeBoxed, "result", constantNull());
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(innerTypeBoxed, "value", innerExpression.evaluateCodegen(innerTypeBoxed, methodNode, scope, codegenClassScope))
            .ifRefNull("value").blockContinue()
            .ifCondition(equalsNull(ref("minKey")))
            .assignRef("minKey", ref("value"))
            .assignRef("result", cast(resultTypeBoxed, ref("next")))
            .ifElse()
            .ifCondition(relational(exprDotMethod(ref("minKey"), "compareTo", ref("value")), max ? LT : GT, constant(0)))
            .assignRef("minKey", ref("value"))
            .assignRef("result", cast(resultTypeBoxed, ref("next")));
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(ref("result"));
    }
}
