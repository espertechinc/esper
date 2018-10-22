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
package com.espertech.esper.common.internal.bytecodemodel.base;

import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInnerClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldName;

import java.util.ArrayList;
import java.util.List;

public class CodegenClassScope extends CodegenScope {

    private final CodegenPackageScope packageScope;
    private final String outermostClassName;
    private final List<CodegenInnerClass> additionalInnerClasses = new ArrayList<>();

    public CodegenClassScope(boolean debug, CodegenPackageScope packageScope, String outermostClassName) {
        super(debug);
        this.packageScope = packageScope;
        this.outermostClassName = outermostClassName;
    }

    public <T> CodegenExpressionField addFieldUnshared(boolean isFinal, Class<? extends T> clazz, CodegenExpression assignScopedPackageInitMethod) {
        return packageScope.addFieldUnshared(isFinal, clazz, assignScopedPackageInitMethod);
    }

    public CodegenExpressionField addOrGetFieldSharable(CodegenFieldSharable sharable) {
        return packageScope.addOrGetFieldSharable(sharable);
    }

    public CodegenExpressionField addOrGetFieldWellKnown(CodegenFieldName fieldName, Class type) {
        return packageScope.addOrGetFieldWellKnown(fieldName, type);
    }

    public void addInnerClass(CodegenInnerClass innerClass) {
        additionalInnerClasses.add(innerClass);
    }

    public CodegenPackageScope getPackageScope() {
        return packageScope;
    }

    public void addInnerClasses(List<CodegenInnerClass> innerClasses) {
        additionalInnerClasses.addAll(innerClasses);
    }

    public String getOutermostClassName() {
        return outermostClassName;
    }

    public List<CodegenInnerClass> getAdditionalInnerClasses() {
        return additionalInnerClasses;
    }

    public CodegenField addSubstitutionParameter(String name, Class type) {
        return packageScope.addSubstitutionParameter(name, type);
    }

    public boolean isInstrumented() {
        return packageScope.isInstrumented();
    }
}
