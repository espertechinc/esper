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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.firstoflastof;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.ThreeFormScalar;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
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

public class EnumLastOfScalar extends ThreeFormScalar {

    protected final EPChainableType resultType;

    public EnumLastOfScalar(ExprDotEvalParamLambda lambda, ObjectArrayEventType fieldEventType, int numParameters, EPChainableType resultType) {
        super(lambda, fieldEventType, numParameters);
        this.resultType = resultType;
    }

    public EnumEval getEnumEvaluator() {
        ExprEvaluator inner = innerExpression.getExprEvaluator();
        return new EnumEval() {
            public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
                Object result = null;
                ObjectArrayEventBean evalEvent = new ObjectArrayEventBean(new Object[3], fieldEventType);
                eventsLambda[getStreamNumLambda()] = evalEvent;
                Object[] props = evalEvent.getProperties();
                props[2] = enumcoll.size();

                int count = -1;
                for (Object next : enumcoll) {
                    count++;
                    props[0] = next;
                    props[1] = count;

                    Object pass = inner.evaluate(eventsLambda, isNewData, context);
                    if (pass == null || (!(Boolean) pass)) {
                        continue;
                    }

                    result = next;
                }

                return result;
            }
        };
    }

    public EPTypeClass returnTypeOfMethod() {
        return JavaClassHelper.getBoxedType(EPChainableTypeHelper.getCodegenReturnType(resultType));
    }

    public CodegenExpression returnIfEmptyOptional() {
        return constantNull();
    }

    public void initBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        block.declareVar(EPTypePremade.OBJECT.getEPType(), "result", constantNull());
    }

    public void forEachBlock(CodegenBlock block, CodegenMethod methodNode, ExprForgeCodegenSymbol scope, CodegenClassScope codegenClassScope) {
        CodegenLegoBooleanExpression.codegenContinueIfNotNullAndNotPass(block, innerExpression.getEvaluationType(), innerExpression.evaluateCodegen(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), methodNode, scope, codegenClassScope));
        block.assignRef("result", ref("next"));
    }

    public void returnResult(CodegenBlock block) {
        block.methodReturn(cast(returnTypeOfMethod(), ref("result")));
    }
}
