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

import com.espertech.esper.common.client.soda.Expression;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleExpressionDeclaredInitializeSymbol;
import com.espertech.esper.common.internal.util.Copyable;

import java.util.function.Supplier;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExpressionDeclItem implements Copyable<ExpressionDeclItem> {
    private final String name;
    private final String[] parametersNames;
    private final boolean alias;
    private Expression optionalSoda;
    private Supplier<byte[]> optionalSodaBytes;
    private String moduleName;
    private NameAccessModifier visibility = NameAccessModifier.TRANSIENT;

    public ExpressionDeclItem(String name, String[] parametersNames, boolean alias) {
        this.name = name;
        this.parametersNames = parametersNames;
        this.alias = alias;
    }

    public ExpressionDeclItem(String name, String[] parametersNames, boolean alias, Expression optionalSoda, Supplier<byte[]> optionalSodaBytes, String moduleName, NameAccessModifier visibility) {
        this.name = name;
        this.parametersNames = parametersNames;
        this.alias = alias;
        this.optionalSoda = optionalSoda;
        this.optionalSodaBytes = optionalSodaBytes;
        this.moduleName = moduleName;
        this.visibility = visibility;
    }

    public ExpressionDeclItem copy() {
        return new ExpressionDeclItem(name, parametersNames, alias, optionalSoda, optionalSodaBytes, moduleName, visibility);
    }

    public String getName() {
        return name;
    }

    public String[] getParametersNames() {
        return parametersNames;
    }

    public boolean isAlias() {
        return alias;
    }

    public Expression getOptionalSoda() {
        return optionalSoda;
    }

    public void setOptionalSoda(Expression optionalSoda) {
        this.optionalSoda = optionalSoda;
    }

    public Supplier<byte[]> getOptionalSodaBytes() {
        return optionalSodaBytes;
    }

    public void setOptionalSodaBytes(Supplier<byte[]> optionalSodaBytes) {
        this.optionalSodaBytes = optionalSodaBytes;
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

    public CodegenExpression make(CodegenMethod parent, ModuleExpressionDeclaredInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ExpressionDeclItem.class, this.getClass(), classScope);

        CodegenExpressionNewAnonymousClass supplierSodaBytes = newAnonymousClass(method.getBlock(), Supplier.class);
        CodegenMethod get = CodegenMethod.makeParentNode(Object.class, this.getClass(), classScope);
        supplierSodaBytes.addMethod("get", get);
        get.getBlock().methodReturn(constant(optionalSodaBytes.get()));

        method.getBlock()
                .declareVar(ExpressionDeclItem.class, "item", newInstance(ExpressionDeclItem.class, constant(name), constant(parametersNames), constant(alias)))
                .exprDotMethod(ref("item"), "setOptionalSodaBytes", supplierSodaBytes)
                .exprDotMethod(ref("item"), "setModuleName", constant(moduleName))
                .exprDotMethod(ref("item"), "setVisibility", constant(visibility))
                .methodReturn(ref("item"));
        return localMethod(method);
    }
}


