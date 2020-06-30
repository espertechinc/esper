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
package com.espertech.esper.common.internal.epl.script.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVisitorWithParent;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.settings.ClasspathImportEPTypeUtil;
import com.espertech.esper.common.internal.type.ClassDescriptor;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class ExprNodeScript extends ExprNodeBase implements ExprForge, ExprEnumerationForge, ExprNodeInnerNodeProvider {

    public static final String CONTEXT_BINDING_NAME = "epl";

    private final String defaultDialect;
    private final ExpressionScriptProvided script;
    private List<ExprNode> parameters;
    private ScriptDescriptorCompileTime scriptDescriptor;
    private EventType eventTypeCollection;

    public ExprNodeScript(String defaultDialect, ExpressionScriptProvided script, List<ExprNode> parameters) {
        this.defaultDialect = defaultDialect;
        this.script = script;
        this.parameters = parameters;
    }

    public ExprForge getForge() {
        return this;
    }

    public List<ExprNode> getAdditionalNodes() {
        return parameters;
    }

    public List<ExprNode> getParameters() {
        return parameters;
    }

    public String getEventTypeNameAnnotation() {
        return script.getOptionalEventTypeName();
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        writer.append(script.getName());
        ExprNodeUtilityPrint.toExpressionStringIncludeParen(parameters, writer);
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
        return ExprNodeUtilityCompare.deepEquals(parameters, that.parameters);
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {

        if (script.getParameterNames().length != parameters.size()) {
            throw new ExprValidationException("Invalid number of parameters for script '" + script.getName() + "', expected " + script.getParameterNames().length + " parameters but received " + parameters.size() + " parameters");
        }
        if (!validationContext.getStatementCompileTimeService().getConfiguration().getCompiler().getScripts().isEnabled()) {
            throw new ExprValidationException("Script compilation has been disabled by configuration");
        }

        // validate all expression parameters
        List<ExprNode> validatedParameters = new ArrayList<ExprNode>();
        for (ExprNode expr : parameters) {
            validatedParameters.add(ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.SCRIPTPARAMS, expr, validationContext));
        }

        // set up map of input parameter names and evaluators
        ExprForge[] forges = new ExprForge[script.getParameterNames().length];
        for (int i = 0; i < script.getParameterNames().length; i++) {
            forges[i] = validatedParameters.get(i).getForge();
        }
        this.parameters = validatedParameters;

        // Compile script
        EPType[] parameterTypes = ExprNodeUtilityQuery.getExprResultTypes(forges);
        String dialect = script.getOptionalDialect() == null ? defaultDialect : script.getOptionalDialect();
        ExpressionScriptCompiled compiled = ExpressionNodeScriptCompiler.compileScript(dialect, script.getName(), script.getExpression(), script.getParameterNames(), parameterTypes, script.getCompiledBuf(), validationContext.getClasspathImportService());

        // Determine declared return type
        EPTypeClass declaredReturnType = getDeclaredReturnType(script.getOptionalReturnTypeName(), validationContext);
        EPTypeClass returnType;
        if (compiled.getKnownReturnType() == null && script.getOptionalReturnTypeName() == null) {
            returnType = EPTypePremade.OBJECT.getEPType();
        } else if (compiled.getKnownReturnType() != null) {
            if (declaredReturnType == null) {
                returnType = compiled.getKnownReturnType();
            } else {
                EPTypeClass knownReturnType = compiled.getKnownReturnType();
                if (declaredReturnType.getType().isArray() && knownReturnType.getType().isArray()) {
                    // we are fine
                } else if (!JavaClassHelper.isAssignmentCompatible(knownReturnType, declaredReturnType.getType())) {
                    throw new ExprValidationException("Return type and declared type not compatible for script '" + script.getName() + "', known return type is " + knownReturnType.getTypeName() + " versus declared return type " + declaredReturnType.getTypeName());
                }
                returnType = declaredReturnType;
            }
        } else {
            returnType = declaredReturnType;
        }
        if (returnType == null) {
            returnType = EPTypePremade.OBJECT.getEPType();
        }

        eventTypeCollection = null;
        if (script.getOptionalEventTypeName() != null) {
            if (returnType.getType().isArray() && returnType.getType().getComponentType() == EventBean.class) {
                eventTypeCollection = EventTypeUtility.requireEventType("Script", script.getName(), script.getOptionalEventTypeName(), validationContext.getStatementCompileTimeService().getEventTypeCompileTimeResolver());
            } else {
                throw new ExprValidationException(EventTypeUtility.disallowedAtTypeMessage());
            }
        }

        scriptDescriptor = new ScriptDescriptorCompileTime(script.getOptionalDialect(), script.getName(), script.getExpression(),
                script.getParameterNames(), parameters.toArray(new ExprNode[0]), returnType, defaultDialect);
        return null;
    }

    @Override
    public void accept(ExprNodeVisitor visitor) {
        super.accept(visitor);
        ExprNodeUtilityQuery.acceptParams(visitor, parameters);
    }

    @Override
    public void accept(ExprNodeVisitorWithParent visitor) {
        super.accept(visitor);
        ExprNodeUtilityQuery.acceptParams(visitor, parameters);
    }

    @Override
    public void acceptChildnodes(ExprNodeVisitorWithParent visitor, ExprNode parent) {
        super.acceptChildnodes(visitor, parent);
        ExprNodeUtilityQuery.acceptParams(visitor, parameters, this);
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprEvaluator() {
            public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
            }
        };
    }

    public EPTypeClass getEvaluationType() {
        return scriptDescriptor.getReturnType();
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public EPTypeClass getComponentTypeCollection() throws ExprValidationException {
        EPTypeClass returnType = scriptDescriptor.getReturnType();
        if (returnType.getType().isArray()) {
            return JavaClassHelper.getArrayComponentType(returnType);
        }
        return null;
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return eventTypeCollection;
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return null;
    }

    public CodegenExpression evaluateCodegen(EPTypeClass requiredType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol symbols, CodegenClassScope codegenClassScope) {
        return CodegenLegoCast.castSafeFromObjectType(requiredType, makeEval("evaluate", codegenMethodScope, symbols, codegenClassScope));
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol symbols, CodegenClassScope codegenClassScope) {
        return makeEval("evaluateGetROCollectionEvents", codegenMethodScope, symbols, codegenClassScope);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol symbols, CodegenClassScope codegenClassScope) {
        return makeEval("evaluateGetROCollectionScalar", codegenMethodScope, symbols, codegenClassScope);
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol symbols, CodegenClassScope codegenClassScope) {
        return makeEval("evaluateGetEventBean", codegenMethodScope, symbols, codegenClassScope);
    }

    private CodegenExpression makeEval(String method, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol symbols, CodegenClassScope codegenClassScope) {
        CodegenExpressionField eval = getField(codegenClassScope);
        return exprDotMethod(eval, method, symbols.getAddEPS(codegenMethodScope), symbols.getAddIsNewData(codegenMethodScope), symbols.getAddExprEvalCtx(codegenMethodScope));
    }

    public CodegenExpressionField getField(CodegenClassScope codegenClassScope) {
        return codegenClassScope.getPackageScope().addOrGetFieldSharable(new ScriptCodegenFieldSharable(scriptDescriptor, codegenClassScope));
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    private EPTypeClass getDeclaredReturnType(String returnTypeName, ExprValidationContext validationContext)
            throws ExprValidationException {
        if (returnTypeName == null) {
            return null;
        }

        if (returnTypeName.equals("void")) {
            return null;
        }

        Class simpleNameClass = JavaClassHelper.getClassForSimpleName(returnTypeName, validationContext.getClasspathImportService().getClassForNameProvider());
        if (simpleNameClass != null) {
            return ClassHelperGenericType.getClassEPType(simpleNameClass);
        }

        String returnTypeLower = returnTypeName.toLowerCase(Locale.ENGLISH);
        if (returnTypeLower.equals("eventbean")) {
            return EventBean.EPTYPE;
        }
        if (returnTypeLower.equals("eventbean[]")) {
            return EventBean.EPTYPEARRAY;
        }

        ClassDescriptor classDescriptor = ClassDescriptor.parseTypeText(returnTypeName);
        EPTypeClass returnType = ClasspathImportEPTypeUtil.resolveClassIdentifierToEPType(classDescriptor, false, validationContext.getClasspathImportService(), validationContext.getClassProvidedClasspathExtension());
        if (returnType == null) {
            throw new ExprValidationException("Failed to resolve return type '" + returnTypeName + "' specified for script '" + script.getName() + "'");
        }
        return returnType;
    }
}
