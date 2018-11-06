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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.script.core.ExpressionScriptCompiled;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExpressionScriptProvided {

    private String name;
    private String expression;
    private String[] parameterNames;
    private String optionalReturnTypeName;
    private String optionalEventTypeName;
    private boolean optionalReturnTypeIsArray;
    private String optionalDialect;
    private String moduleName;
    private NameAccessModifier visibility = NameAccessModifier.TRANSIENT;

    private ExpressionScriptCompiled compiledBuf;

    public ExpressionScriptProvided() {
    }

    public ExpressionScriptProvided(String name, String expression, String[] parameterNames, String optionalReturnTypeName, boolean optionalReturnTypeIsArray, String optionalEventTypeName, String optionalDialect) {
        this.name = name;
        this.expression = expression;
        this.parameterNames = parameterNames;
        this.optionalReturnTypeName = optionalReturnTypeName;
        this.optionalReturnTypeIsArray = optionalReturnTypeIsArray;
        this.optionalEventTypeName = optionalEventTypeName;
        this.optionalDialect = optionalDialect;
        if (expression == null) {
            throw new IllegalArgumentException("Invalid null expression received");
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ExpressionScriptProvided.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ExpressionScriptProvided.class, "sp", newInstance(ExpressionScriptProvided.class))
                .exprDotMethod(ref("sp"), "setName", constant(name))
                .exprDotMethod(ref("sp"), "setExpression", constant(expression))
                .exprDotMethod(ref("sp"), "setParameterNames", constant(parameterNames))
                .exprDotMethod(ref("sp"), "setOptionalReturnTypeName", constant(optionalReturnTypeName))
                .exprDotMethod(ref("sp"), "setOptionalEventTypeName", constant(optionalEventTypeName))
                .exprDotMethod(ref("sp"), "setOptionalReturnTypeIsArray", constant(optionalReturnTypeIsArray))
                .exprDotMethod(ref("sp"), "setOptionalDialect", constant(optionalDialect))
                .exprDotMethod(ref("sp"), "setModuleName", constant(moduleName))
                .exprDotMethod(ref("sp"), "setVisibility", constant(visibility))
                .methodReturn(ref("sp"));
        return localMethod(method);
    }

    public String getName() {
        return name;
    }

    public String getExpression() {
        return expression;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    public String getOptionalReturnTypeName() {
        return optionalReturnTypeName;
    }

    public String getOptionalDialect() {
        return optionalDialect;
    }

    public boolean isOptionalReturnTypeIsArray() {
        return optionalReturnTypeIsArray;
    }

    public String getOptionalEventTypeName() {
        return optionalEventTypeName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void setParameterNames(String[] parameterNames) {
        this.parameterNames = parameterNames;
    }

    public void setOptionalReturnTypeName(String optionalReturnTypeName) {
        this.optionalReturnTypeName = optionalReturnTypeName;
    }

    public void setOptionalEventTypeName(String optionalEventTypeName) {
        this.optionalEventTypeName = optionalEventTypeName;
    }

    public void setOptionalReturnTypeIsArray(boolean optionalReturnTypeIsArray) {
        this.optionalReturnTypeIsArray = optionalReturnTypeIsArray;
    }

    public void setOptionalDialect(String optionalDialect) {
        this.optionalDialect = optionalDialect;
    }

    public void setCompiledBuf(ExpressionScriptCompiled compiledBuf) {
        this.compiledBuf = compiledBuf;
    }

    public ExpressionScriptCompiled getCompiledBuf() {
        return compiledBuf;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public NameAccessModifier getVisibility() {
        return visibility;
    }

    public void setVisibility(NameAccessModifier visibility) {
        this.visibility = visibility;
    }
}
