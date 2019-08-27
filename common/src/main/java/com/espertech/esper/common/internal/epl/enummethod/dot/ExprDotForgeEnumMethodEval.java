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
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

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

    public static CodegenExpression codegen(ExprDotForgeEnumMethodBase forge, CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class returnType = EPTypeHelper.getCodegenReturnType(forge.getTypeInfo());
        CodegenMethod methodNode = codegenMethodScope.makeChild(returnType, ExprDotForgeEnumMethodEval.class, codegenClassScope).addParam(innerType, "param");

        CodegenExpressionRef refEPS = exprSymbol.getAddEPS(methodNode);
        CodegenExpression refIsNewData = exprSymbol.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);

        CodegenExpressionField forgeMember = codegenClassScope.addFieldUnshared(true, Object.class, newInstance(Object.class));
        CodegenBlock block = methodNode.getBlock();
        if (innerType == EventBean.class) {
            block.declareVar(Collection.class, "coll", staticMethod(Collections.class, "singletonList", ref("param")));
        } else {
            block.declareVar(Collection.class, "coll", ref("param"));
        }

        block.declareVar(ExpressionResultCacheForEnumerationMethod.class, "cache", exprDotMethodChain(refExprEvalCtx).add("getExpressionResultCacheService").add("getAllocateEnumerationMethod"));
        EnumForgeCodegenParams premade = new EnumForgeCodegenParams(ref("eventsLambda"), ref("coll"), refIsNewData, refExprEvalCtx);
        if (forge.cache) {
            block.declareVar(ExpressionResultCacheEntryLongArrayAndObj.class, "cacheValue", exprDotMethod(ref("cache"), "getEnumerationMethodLastValue", forgeMember))
                    .ifCondition(notEqualsNull(ref("cacheValue")))
                    .blockReturn(cast(returnType, exprDotMethod(ref("cacheValue"), "getResult")))
                    .ifRefNullReturnNull("coll")
                    .declareVar(EventBean[].class, "eventsLambda", staticMethod(ExprDotForgeEnumMethodEval.class, "allocateCopyEventLambda", refEPS, constant(forge.enumEvalNumRequiredEvents)))
                    .declareVar(EPTypeHelper.getCodegenReturnType(forge.getTypeInfo()), "result", forge.enumForge.codegen(premade, methodNode, codegenClassScope))
                    .expression(exprDotMethod(ref("cache"), "saveEnumerationMethodLastValue", forgeMember, ref("result")))
                    .methodReturn(ref("result"));
        } else {
            CodegenExpressionField contextNumberMember = codegenClassScope.addFieldUnshared(true, AtomicLong.class, newInstance(AtomicLong.class));
            block.declareVar(long.class, "contextNumber", exprDotMethod(contextNumberMember, "getAndIncrement"))
                    .tryCatch()
                    .expression(exprDotMethod(ref("cache"), "pushContext", ref("contextNumber")))
                    .ifRefNullReturnNull("coll")
                    .declareVar(EventBean[].class, "eventsLambda", staticMethod(ExprDotForgeEnumMethodEval.class, "allocateCopyEventLambda", refEPS, constant(forge.enumEvalNumRequiredEvents)))
                    .tryReturn(forge.enumForge.codegen(premade, methodNode, codegenClassScope))
                    .tryFinally()
                    .expression(exprDotMethod(ref("cache"), "popContext"))
                    .blockEnd()
                    .methodEnd();
        }
        return localMethod(methodNode, inner);
    }

    public EPType getTypeInfo() {
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
