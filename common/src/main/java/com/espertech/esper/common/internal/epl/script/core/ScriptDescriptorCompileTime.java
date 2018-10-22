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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ScriptDescriptorCompileTime {
    private final String optionalDialect;
    private final String scriptName;
    private final String expression;
    private final String[] parameterNames;
    private final ExprForge[] parameters;
    private final Class returnType;
    private final String defaultDialect;

    public ScriptDescriptorCompileTime(String optionalDialect, String scriptName, String expression, String[] parameterNames, ExprForge[] parameters, Class returnType, String defaultDialect) {
        this.optionalDialect = optionalDialect;
        this.scriptName = scriptName;
        this.expression = expression;
        this.parameterNames = parameterNames;
        this.parameters = parameters;
        this.returnType = returnType;
        this.defaultDialect = defaultDialect;
    }

    public CodegenExpression make(CodegenMethodScope parentInitMethod, CodegenClassScope classScope) {
        CodegenMethod method = parentInitMethod.makeChild(ScriptDescriptorRuntime.class, this.getClass(), classScope).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef());
        method.getBlock()
                .declareVar(ScriptDescriptorRuntime.class, "sd", newInstance(ScriptDescriptorRuntime.class))
                .exprDotMethod(ref("sd"), "setOptionalDialect", constant(optionalDialect))
                .exprDotMethod(ref("sd"), "setScriptName", constant(scriptName))
                .exprDotMethod(ref("sd"), "setExpression", constant(expression))
                .exprDotMethod(ref("sd"), "setParameterNames", constant(parameterNames))
                .exprDotMethod(ref("sd"), "setEvaluationTypes", constant(ExprNodeUtilityQuery.getExprResultTypes(parameters)))
                .exprDotMethod(ref("sd"), "setParameters", ExprNodeUtilityCodegen.codegenEvaluators(parameters, method, this.getClass(), classScope))
                .exprDotMethod(ref("sd"), "setDefaultDialect", constant(defaultDialect))
                .exprDotMethod(ref("sd"), "setClasspathImportService", exprDotMethodChain(EPStatementInitServices.REF).add(EPStatementInitServices.GETCLASSPATHIMPORTSERVICERUNTIME))
                .exprDotMethod(ref("sd"), "setCoercer", JavaClassHelper.isNumeric(returnType) ? staticMethod(SimpleNumberCoercerFactory.class, "getCoercer", constant(Number.class),
                        constant(JavaClassHelper.getBoxedType(returnType))) : constantNull())
                .methodReturn(ref("sd"));
        return localMethod(method, EPStatementInitServices.REF);
    }

    public String getOptionalDialect() {
        return optionalDialect;
    }

    public String getScriptName() {
        return scriptName;
    }

    public String getExpression() {
        return expression;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    public ExprForge[] getParameters() {
        return parameters;
    }

    public Class getReturnType() {
        return returnType;
    }
}
