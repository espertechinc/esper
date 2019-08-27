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
package com.espertech.esper.common.internal.epl.enummethod.plugin;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.enummethod.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamExpr;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForge;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumSumEventsForgeEval;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumForgePlugin implements EnumForge {
    private final List<ExprDotEvalParam> bodiesAndParameters;
    private final EnumMethodModeStaticMethod mode;
    private final Class expectedStateReturnType;
    private final int numStreamsIncoming;
    private final DotMethodFP footprint;
    private final EventType inputEventType;

    public EnumForgePlugin(List<ExprDotEvalParam> bodiesAndParameters, EnumMethodModeStaticMethod mode, Class expectedStateReturnType, int numStreamsIncoming, DotMethodFP footprint, EventType inputEventType) {
        this.bodiesAndParameters = bodiesAndParameters;
        this.mode = mode;
        this.expectedStateReturnType = expectedStateReturnType;
        this.numStreamsIncoming = numStreamsIncoming;
        this.footprint = footprint;
        this.inputEventType = inputEventType;
    }

    public EnumEval getEnumEvaluator() {
        throw new UnsupportedOperationException("Enum-evaluator not available at compile-time");
    }

    public int getStreamNumSize() {
        int countLambda = 0;
        for (ExprDotEvalParam param : bodiesAndParameters) {
            if (param instanceof ExprDotEvalParamLambda) {
                ExprDotEvalParamLambda lambda = (ExprDotEvalParamLambda) param;
                countLambda += lambda.getGoesToNames().size();
            }
        }
        return numStreamsIncoming + countLambda;
    }

    public CodegenExpression codegen(EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {

        ExprForgeCodegenSymbol scope = new ExprForgeCodegenSymbol(false, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(expectedStateReturnType, EnumSumEventsForgeEval.class, scope, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);
        methodNode.getBlock().declareVar(mode.getStateClass(), "state", newInstance(mode.getStateClass()));

        // call set-parameter for each non-lambda expression
        int indexNonLambda = 0;
        for (ExprDotEvalParam param : bodiesAndParameters) {
            if (param instanceof ExprDotEvalParamExpr) {
                CodegenExpression expression = param.getBodyForge().evaluateCodegen(Object.class, methodNode, scope, codegenClassScope);
                methodNode.getBlock().exprDotMethod(ref("state"), "setParameter", constant(indexNonLambda), expression);
                indexNonLambda++;
            }
        }

        // allocate event type and field for each lambda expression
        int indexParameter = 0;
        for (ExprDotEvalParam param : bodiesAndParameters) {
            if (param instanceof ExprDotEvalParamLambda) {
                ExprDotEvalParamLambda lambda = (ExprDotEvalParamLambda) param;
                for (int i = 0; i < lambda.getGoesToTypes().length; i++) {
                    EventType eventType = lambda.getGoesToTypes()[i];
                    EnumMethodLambdaParameterType lambdaParameterType = mode.getLambdaParameters().apply(new EnumMethodLambdaParameterDescriptor(indexParameter, i));

                    if (eventType != inputEventType) {
                        CodegenExpressionField type = codegenClassScope.addFieldUnshared(true, ObjectArrayEventType.class, cast(ObjectArrayEventType.class, EventTypeUtility.resolveTypeCodegen(lambda.getGoesToTypes()[i], EPStatementInitServices.REF)));
                        String eventName = getNameExt("resultEvent", indexParameter, i);
                        String propName = getNameExt("props", indexParameter, i);
                        methodNode.getBlock()
                            .declareVar(ObjectArrayEventBean.class, eventName, newInstance(ObjectArrayEventBean.class, newArrayByLength(Object.class, constant(1)), type))
                            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(lambda.getStreamCountIncoming() + i), ref(eventName))
                            .declareVar(Object[].class, propName, exprDotMethod(ref(eventName), "getProperties"));

                        // initialize index-type lambda-parameters to zer0
                        if (lambdaParameterType instanceof EnumMethodLambdaParameterTypeIndex) {
                            methodNode.getBlock()
                                .assignArrayElement(propName, constant(0), constant(0));
                        }
                        if (lambdaParameterType instanceof EnumMethodLambdaParameterTypeStateGetter) {
                            EnumMethodLambdaParameterTypeStateGetter getter = (EnumMethodLambdaParameterTypeStateGetter) lambdaParameterType;
                            methodNode.getBlock()
                                .assignArrayElement(propName, constant(0), exprDotMethod(ref("state"), getter.getGetterMethodName()));
                        }
                    }
                }
            }
            indexParameter++;
        }

        Class elementType = inputEventType == null ? Object.class : EventBean.class;
        methodNode.getBlock().declareVar(int.class, "count", constant(-1));
        CodegenBlock forEach = methodNode.getBlock().forEach(elementType, "next", EnumForgeCodegenNames.REF_ENUMCOLL);
        {
            forEach.incrementRef("count");

            List<CodegenExpression> paramsNext = new ArrayList<>();
            paramsNext.add(ref("state"));
            paramsNext.add(ref("next"));

            indexParameter = 0;
            for (ExprDotEvalParam param : bodiesAndParameters) {
                if (param instanceof ExprDotEvalParamLambda) {
                    ExprDotEvalParamLambda lambda = (ExprDotEvalParamLambda) param;
                    String valueName = "value_" + indexParameter;
                    for (int i = 0; i < lambda.getGoesToTypes().length; i++) {
                        EnumMethodLambdaParameterType lambdaParameterType = mode.getLambdaParameters().apply(new EnumMethodLambdaParameterDescriptor(indexParameter, i));

                        String propName = getNameExt("props", indexParameter, i);
                        if (lambdaParameterType instanceof EnumMethodLambdaParameterTypeValue) {
                            EventType eventType = lambda.getGoesToTypes()[i];
                            if (eventType == inputEventType) {
                                forEach.assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(lambda.getStreamCountIncoming() + i), ref("next"));
                            } else {
                                forEach.assignArrayElement(propName, constant(0), ref("next"));
                            }
                        } else if (lambdaParameterType instanceof EnumMethodLambdaParameterTypeIndex) {
                            forEach.assignArrayElement(propName, constant(0), ref("count"));
                        } else if (lambdaParameterType instanceof EnumMethodLambdaParameterTypeStateGetter) {
                            EnumMethodLambdaParameterTypeStateGetter getter = (EnumMethodLambdaParameterTypeStateGetter) lambdaParameterType;
                            forEach.assignArrayElement(propName, constant(0), exprDotMethod(ref("state"), getter.getGetterMethodName()));
                        } else {
                            throw new UnsupportedOperationException("Unrecognized lambda parameter type " + lambdaParameterType);
                        }
                    }

                    ExprForge forge = lambda.getBodyForge();
                    forEach.declareVar(forge.getEvaluationType(), valueName, forge.evaluateCodegen(forge.getEvaluationType(), methodNode, scope, codegenClassScope));
                    paramsNext.add(ref(valueName));
                }
                indexParameter++;
            }

            forEach.expression(staticMethod(mode.getServiceClass(), mode.getMethodName(), paramsNext.toArray(new CodegenExpression[0])));

            if (mode.isEarlyExit()) {
                forEach.ifCondition(exprDotMethod(ref("state"), "completed")).breakLoop();
            }
        }

        methodNode.getBlock().methodReturn(cast(expectedStateReturnType, exprDotMethod(ref("state"), "state")));
        return localMethod(methodNode, args.getEps(), args.getEnumcoll(), args.getIsNewData(), args.getExprCtx());
    }

    private String getNameExt(String prefix, int indexLambda, int number) {
        return prefix + "_" + indexLambda + "_" + number;
    }
}
