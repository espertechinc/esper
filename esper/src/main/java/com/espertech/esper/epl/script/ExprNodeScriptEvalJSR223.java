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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprNodeScriptEvalJSR223 extends ExprNodeScriptEvalBase implements ExprNodeScriptEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ExprNodeScriptEvalJSR223.class);

    private final CompiledScript executable;
    private volatile ExprEvaluator[] evaluators;

    public ExprNodeScriptEvalJSR223(ExprNodeScript parent, String statementName, String[] names, ExprForge[] parameters, Class returnType, EventType eventTypeCollection, CompiledScript executable) {
        super(parent, statementName, names, parameters, returnType, eventTypeCollection);
        this.executable = executable;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (evaluators == null) {
            evaluators = ExprNodeUtility.getEvaluatorsNoCompile(parameters);
        }
        Bindings bindings = getBindings(context);
        for (int i = 0; i < names.length; i++) {
            bindings.put(names[i], evaluators[i].evaluate(eventsPerStream, isNewData, context));
        }
        return evaluateInternal(bindings);
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public CodegenExpression evaluateCodegen(CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember member = context.makeAddMember(ExprNodeScriptEvalJSR223.class, this);
        CodegenBlock block = context.addMethod(returnType, ExprNodeScriptEvalJSR223.class).add(params).begin()
                .declareVar(Bindings.class, "bindings", exprDotMethod(ref(member.getMemberName()), "getBindings", params.passEvalCtx()));
        for (int i = 0; i < names.length; i++) {
            block.expression(exprDotMethod(ref("bindings"), "put", constant(names[i]), parameters[i].evaluateCodegen(params, context)));
        }
        String method = block.methodReturn(cast(returnType, exprDotMethod(ref(member.getMemberName()), "evaluateInternal", ref("bindings"))));
        return localMethodBuild(method).passAll(params).call();
    }

    public ExprForgeComplexityEnum getComplexity() {
        return names.length == 0 ? ExprForgeComplexityEnum.SINGLE : ExprForgeComplexityEnum.INTER;
    }

    public Class getEvaluationType() {
        return returnType;
    }

    public Object evaluate(Object[] lookupValues, ExprEvaluatorContext context) {
        Bindings bindings = getBindings(context);
        for (int i = 0; i < names.length; i++) {
            bindings.put(names[i], lookupValues[i]);
        }
        return evaluateInternal(bindings);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param context context
     * @return bindings
     */
    public Bindings getBindings(ExprEvaluatorContext context) {
        Bindings bindings = executable.getEngine().createBindings();
        bindings.put(ExprNodeScript.CONTEXT_BINDING_NAME, context.getAllocateAgentInstanceScriptContext());
        return bindings;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Evaluate.
     * @param bindings bindings
     * @return result
     */
    public Object evaluateInternal(Bindings bindings) {
        try {
            Object result = executable.eval(bindings);

            if (coercer != null) {
                return coercer.coerceBoxed((Number) result);
            }

            return result;
        } catch (ScriptException e) {
            String message = "Unexpected exception executing script '" + parent.getScript() + "' for statement '" + statementName + "' : " + e.getMessage();
            log.error(message, e);
            throw new EPException(message, e);
        }
    }
}
