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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheEntryLongArrayAndObj;
import com.espertech.esper.common.internal.epl.enummethod.cache.ExpressionResultCacheForEnumerationMethod;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEval;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotForge;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotForgeEnumMethodEval implements ExprDotEval {

    private final ExprDotForgeEnumMethodBase forge;
    private final EnumEval enumEval;
    private final int enumEvalNumRequiredEvents;

    public ExprDotForgeEnumMethodEval(ExprDotForgeEnumMethodBase forge, EnumEval enumEval, int enumEvalNumRequiredEvents) {
        this.forge = forge;
        this.enumEval = enumEval;
        this.enumEvalNumRequiredEvents = enumEvalNumRequiredEvents;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target instanceof EventBean) {
            target = Collections.singletonList((EventBean) target);
        }
        Collection coll = (Collection) target;
        if (coll == null) {
            return null;
        }
        EventBean[] eventsLambda = eventsPerStream == null ? new EventBean[0] : allocateCopyEventLambda(eventsPerStream, enumEvalNumRequiredEvents);
        return enumEval.evaluateEnumMethod(eventsLambda, coll, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(ExprDotForgeEnumMethodBase forge, CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        EPTypeClass returnType = EPChainableTypeHelper.getCodegenReturnType(forge.getTypeInfo());
        CodegenMethod methodNode = codegenMethodScope.makeChild(returnType, ExprDotForgeEnumMethodEval.class, codegenClassScope).addParam(innerType, "param");

        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);

        CodegenExpressionField forgeMember = codegenClassScope.addFieldUnshared(true, EPTypePremade.OBJECT.getEPType(), newInstance(EPTypePremade.OBJECT.getEPType()));
        CodegenBlock block = methodNode.getBlock();
        if (innerType.getType() == EventBean.class) {
            block.declareVar(EPTypePremade.COLLECTION.getEPType(), "coll", staticMethod(Collections.class, "singletonList", ref("param")));
        } else {
            block.declareVar(EPTypePremade.COLLECTION.getEPType(), "coll", ref("param"));
        }

        block.declareVar(ExpressionResultCacheForEnumerationMethod.EPTYPE, "cache", exprDotMethodChain(refExprEvalCtx).add("getExpressionResultCacheService").add("getAllocateEnumerationMethod"));
        EnumForgeCodegenParams premade = new EnumForgeCodegenParams(ref("eventsLambda"), ref("coll"), refIsNewData, refExprEvalCtx);
        if (forge.cache) {
            block.declareVar(ExpressionResultCacheEntryLongArrayAndObj.EPTYPE, "cacheValue", exprDotMethod(ref("cache"), "getEnumerationMethodLastValue", forgeMember))
                    .ifCondition(notEqualsNull(ref("cacheValue")))
                    .blockReturn(cast(returnType, exprDotMethod(ref("cacheValue"), "getResult")))
                    .ifRefNullReturnNull("coll")
                    .declareVar(EventBean.EPTYPEARRAY, "eventsLambda", staticMethod(ExprDotForgeEnumMethodEval.class, "allocateCopyEventLambda", refEPS, constant(forge.enumEvalNumRequiredEvents)))
                    .declareVar(EPChainableTypeHelper.getCodegenReturnType(forge.getTypeInfo()), "result", forge.enumForge.codegen(premade, methodNode, codegenClassScope))
                    .expression(exprDotMethod(ref("cache"), "saveEnumerationMethodLastValue", forgeMember, ref("result")))
                    .methodReturn(ref("result"));
        } else {
            CodegenExpressionField contextNumberMember = codegenClassScope.addFieldUnshared(true, EPTypePremade.ATOMICLONG.getEPType(), newInstance(EPTypePremade.ATOMICLONG.getEPType()));
            block.declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "contextNumber", exprDotMethod(contextNumberMember, "getAndIncrement"))
                    .tryCatch()
                    .expression(exprDotMethod(ref("cache"), "pushContext", ref("contextNumber")))
                    .ifRefNullReturnNull("coll")
                    .declareVar(EventBean.EPTYPEARRAY, "eventsLambda", staticMethod(ExprDotForgeEnumMethodEval.class, "allocateCopyEventLambda", refEPS, constant(forge.enumEvalNumRequiredEvents)))
                    .tryReturn(forge.enumForge.codegen(premade, methodNode, codegenClassScope))
                    .tryFinally()
                    .expression(exprDotMethod(ref("cache"), "popContext"))
                    .blockEnd()
                    .methodEnd();
        }
        return localMethod(methodNode, inner);
    }

    public EPChainableType getTypeInfo() {
        return forge.getTypeInfo();
    }

    public ExprDotForge getDotForge() {
        return forge;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param eventsPerStream           events
     * @param enumEvalNumRequiredEvents width
     * @return allocated
     */
    public static EventBean[] allocateCopyEventLambda(EventBean[] eventsPerStream, int enumEvalNumRequiredEvents) {
        EventBean[] eventsLambda = new EventBean[enumEvalNumRequiredEvents];
        EventBeanUtility.safeArrayCopy(eventsPerStream, eventsLambda);
        return eventsLambda;
    }
}
