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
package com.espertech.esper.epl.script;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.script.mvel.MVELInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprNodeScriptEvalMVEL extends ExprNodeScriptEvalBase implements ExprNodeScriptEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ExprNodeScriptEvalMVEL.class);

    private final Object executable;
    private volatile ExprEvaluator[] evaluators;

    public ExprNodeScriptEvalMVEL(ExprNodeScript parent, String statementName, String[] names, ExprForge[] parameters, Class returnType, EventType eventTypeCollection, Object executable) {
        super(parent, statementName, names, parameters, returnType, eventTypeCollection);
        this.executable = executable;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (evaluators == null) {
            evaluators = ExprNodeUtilityCore.getEvaluatorsNoCompile(parameters);
        }
        Map<String, Object> paramsList = getMVELScriptParamsList(context);
        for (int i = 0; i < names.length; i++) {
            paramsList.put(names[i], evaluators[i].evaluate(eventsPerStream, isNewData, context));
        }
        return evaluateInternal(paramsList);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(returnType, ExprNodeScriptEvalMVEL.class, codegenClassScope);

        CodegenMember member = codegenClassScope.makeAddMember(ExprNodeScriptEvalMVEL.class, this);
        CodegenExpressionRef refExprEvalCtx = exprSymbol.getAddExprEvalCtx(methodNode);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(Map.class, "paramsList", staticMethod(ExprNodeScriptEvalMVEL.class, "getMVELScriptParamsList", refExprEvalCtx));
        for (int i = 0; i < names.length; i++) {
            block.expression(exprDotMethod(ref("paramsList"), "put", constant(names[i]), parameters[i].evaluateCodegen(requiredType, methodNode, exprSymbol, codegenClassScope)));
        }

        CodegenExpression scriptResult = exprDotMethod(member(member.getMemberId()), "evaluateInternal", ref("paramsList"));
        if (requiredType == Object.class) {
            block.methodReturn(scriptResult);
        } else {
            block.methodReturn(cast(returnType, scriptResult));
        }
        return localMethod(methodNode);
    }

    public ExprForgeComplexityEnum getComplexity() {
        return names.length == 0 ? ExprForgeComplexityEnum.SINGLE : ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public Object evaluate(Object[] lookupValues, ExprEvaluatorContext context) {
        Map<String, Object> paramsList = getMVELScriptParamsList(context);
        for (int i = 0; i < names.length; i++) {
            paramsList.put(names[i], lookupValues[i]);
        }
        return evaluateInternal(paramsList);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param context ctx
     * @return params
     */
    public static Map<String, Object> getMVELScriptParamsList(ExprEvaluatorContext context) {
        Map<String, Object> paramsList = new HashMap<String, Object>();
        paramsList.put(ExprNodeScript.CONTEXT_BINDING_NAME, context.getAllocateAgentInstanceScriptContext());
        return paramsList;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param paramsList params
     * @return result
     */
    public Object evaluateInternal(Map<String, Object> paramsList) {
        try {
            Object result = MVELInvoker.executeExpression(executable, paramsList);

            if (coercer != null) {
                return coercer.coerceBoxed((Number) result);
            }

            return result;
        } catch (InvocationTargetException ex) {
            Throwable mvelException = ex.getCause();
            String message = "Unexpected exception executing script '" + parent.getScript().getName() + "' for statement '" + statementName + "' : " + mvelException.getMessage();
            log.error(message, mvelException);
            throw new EPException(message, ex);
        }
    }
}
