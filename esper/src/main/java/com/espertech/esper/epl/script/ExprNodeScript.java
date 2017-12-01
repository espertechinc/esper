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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.hook.EPLScriptContext;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeVisitorWithParent;
import com.espertech.esper.epl.script.jsr223.ExpressionScriptCompiledJSR223;
import com.espertech.esper.epl.script.jsr223.JSR223Helper;
import com.espertech.esper.epl.script.mvel.ExpressionScriptCompiledMVEL;
import com.espertech.esper.epl.script.mvel.MVELHelper;
import com.espertech.esper.epl.spec.ExpressionScriptCompiled;
import com.espertech.esper.epl.spec.ExpressionScriptProvided;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.util.JavaClassHelper;

import javax.script.CompiledScript;
import java.io.StringWriter;
import java.util.*;

public class ExprNodeScript extends ExprNodeBase implements ExprNodeInnerNodeProvider {

    private static final long serialVersionUID = 2661218104424440161L;
    public static final String CONTEXT_BINDING_NAME = "epl";

    private final String defaultDialect;
    private final ExpressionScriptProvided script;
    private List<ExprNode> parameters;

    private transient ExprNodeScriptEvaluator evaluator;

    public ExprNodeScript(String defaultDialect, ExpressionScriptProvided script, List<ExprNode> parameters) {
        this.defaultDialect = defaultDialect;
        this.script = script;
        this.parameters = parameters;
    }

    public ExprForge getForge() {
        return evaluator;
    }

    public List<ExprNode> getAdditionalNodes() {
        return parameters;
    }

    public ExprNodeScriptEvaluator getExprEvaluator() {
        return evaluator;
    }

    public ExprNodeScriptEvaluator getScriptExprEvaluator() {
        return evaluator;
    }

    public List<ExprNode> getParameters() {
        return parameters;
    }

    public String getEventTypeNameAnnotation() {
        return script.getOptionalEventTypeName();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(script.getName());
        ExprNodeUtilityCore.toExpressionStringIncludeParen(parameters, writer);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public ExpressionScriptProvided getScript() {
        return script;
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (this == node) return true;
        if (node == null || getClass() != node.getClass()) return false;

        ExprNodeScript that = (ExprNodeScript) node;

        if (script != null ? !script.equals(that.script) : that.script != null) return false;
        return ExprNodeUtilityCore.deepEquals(parameters, that.parameters);
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {

        if (evaluator != null) {
            return null;
        }

        if (script.getParameterNames().size() != parameters.size()) {
            throw new ExprValidationException("Invalid number of parameters for script '" + script.getName() + "', expected " + script.getParameterNames().size() + " parameters but received " + parameters.size() + " parameters");
        }

        // validate all expression parameters
        List<ExprNode> validatedParameters = new ArrayList<ExprNode>();
        for (ExprNode expr : parameters) {
            validatedParameters.add(ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.SCRIPTPARAMS, expr, validationContext));
        }

        // set up map of input parameter names and evaluators
        String[] inputParamNames = new String[script.getParameterNames().size()];
        ExprForge[] forges = new ExprForge[script.getParameterNames().size()];

        for (int i = 0; i < script.getParameterNames().size(); i++) {
            inputParamNames[i] = script.getParameterNames().get(i);
            forges[i] = validatedParameters.get(i).getForge();
        }
        this.parameters = validatedParameters;

        // Compile script
        if (script.getCompiled() == null) {
            compileScript(forges, validationContext.getEngineImportService());
        }

        // Determine declared return type
        Class declaredReturnType = getDeclaredReturnType(script.getOptionalReturnTypeName(), validationContext);
        if (script.isOptionalReturnTypeIsArray() && declaredReturnType != null) {
            declaredReturnType = JavaClassHelper.getArrayType(declaredReturnType);
        }
        Class returnType;
        if (script.getCompiled().getKnownReturnType() == null && script.getOptionalReturnTypeName() == null) {
            returnType = Object.class;
        } else if (script.getCompiled().getKnownReturnType() != null) {
            if (declaredReturnType == null) {
                returnType = script.getCompiled().getKnownReturnType();
            } else {
                Class knownReturnType = script.getCompiled().getKnownReturnType();
                if (declaredReturnType.isArray() && knownReturnType.isArray()) {
                    // we are fine
                } else if (!JavaClassHelper.isAssignmentCompatible(knownReturnType, declaredReturnType)) {
                    throw new ExprValidationException("Return type and declared type not compatible for script '" + script.getName() + "', known return type is " + knownReturnType.getName() + " versus declared return type " + declaredReturnType.getName());
                }
                returnType = declaredReturnType;
            }
        } else {
            returnType = declaredReturnType;
        }
        if (returnType == null) {
            returnType = Object.class;
        }

        EventType eventTypeCollection = null;
        if (script.getOptionalEventTypeName() != null) {
            if (returnType.isArray() && returnType.getComponentType() == EventBean.class) {
                eventTypeCollection = EventTypeUtility.requireEventType("Script", script.getName(), validationContext.getEventAdapterService(), script.getOptionalEventTypeName());
            } else {
                throw new ExprValidationException(EventTypeUtility.disallowedAtTypeMessage());
            }
        }

        // Prepare evaluator - this sets the evaluator
        prepareEvaluator(validationContext.getStatementName(), inputParamNames, forges, returnType, eventTypeCollection);
        return null;
    }

    @Override
    public void accept(ExprNodeVisitor visitor) {
        super.accept(visitor);
        ExprNodeUtilityCore.acceptParams(visitor, parameters);
    }

    @Override
    public void accept(ExprNodeVisitorWithParent visitor) {
        super.accept(visitor);
        ExprNodeUtilityCore.acceptParams(visitor, parameters);
    }

    @Override
    public void acceptChildnodes(ExprNodeVisitorWithParent visitor, ExprNode parent) {
        super.acceptChildnodes(visitor, parent);
        ExprNodeUtilityCore.acceptParams(visitor, parameters, this);
    }

    private void compileScript(ExprForge[] forges, EngineImportService engineImportService)
            throws ExprValidationException {
        String dialect = script.getOptionalDialect() == null ? defaultDialect : script.getOptionalDialect();

        ExpressionScriptCompiled compiled;
        if (dialect.toLowerCase(Locale.ENGLISH).trim().equals("mvel")) {
            Map<String, Class> mvelInputParamTypes = new HashMap<String, Class>();
            for (int i = 0; i < script.getParameterNames().size(); i++) {
                String mvelParamName = script.getParameterNames().get(i);
                mvelInputParamTypes.put(mvelParamName, forges[i].getEvaluationType());
            }
            mvelInputParamTypes.put(CONTEXT_BINDING_NAME, EPLScriptContext.class);
            compiled = MVELHelper.compile(script.getName(), script.getExpression(), mvelInputParamTypes, engineImportService);
        } else {
            CompiledScript compiledScript = JSR223Helper.verifyCompileScript(script, dialect);
            compiled = new ExpressionScriptCompiledJSR223(compiledScript);
        }
        script.setCompiled(compiled);
    }

    private void prepareEvaluator(String statementName, String[] inputParamNames, ExprForge[] evaluators, Class returnType, EventType eventTypeCollection) {
        if (script.getCompiled() instanceof ExpressionScriptCompiledMVEL) {
            ExpressionScriptCompiledMVEL mvel = (ExpressionScriptCompiledMVEL) script.getCompiled();
            evaluator = new ExprNodeScriptEvalMVEL(this, statementName, inputParamNames, evaluators, returnType, eventTypeCollection, mvel.getCompiled());
        } else {
            ExpressionScriptCompiledJSR223 jsr223 = (ExpressionScriptCompiledJSR223) script.getCompiled();
            evaluator = new ExprNodeScriptEvalJSR223(this, statementName, inputParamNames, evaluators, returnType, eventTypeCollection, jsr223.getCompiled());
        }
    }

    private Class getDeclaredReturnType(String returnTypeName, ExprValidationContext validationContext)
            throws ExprValidationException {
        if (returnTypeName == null) {
            return null;
        }

        if (returnTypeName.equals("void")) {
            return null;
        }

        Class returnType = JavaClassHelper.getClassForSimpleName(returnTypeName, validationContext.getEngineImportService().getClassForNameProvider());
        if (returnType != null) {
            return returnType;
        }

        if (returnTypeName.equals("EventBean")) {
            return EventBean.class;
        }

        try {
            return validationContext.getEngineImportService().resolveClass(returnTypeName, false);
        } catch (EngineImportException e1) {
            throw new ExprValidationException("Failed to resolve return type '" + returnTypeName + "' specified for script '" + script.getName() + "'");
        }
    }
}
