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
package com.espertech.esper.common.internal.rettype;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * An array or collection of native values. Always has a component type.
 * Either:
 * - array then "clazz.getArray()" returns true.
 * - collection then clazz implements collection
 */
public class ClassMultiValuedEPType implements EPType {
    private final Class container;
    private final Class component;

    public ClassMultiValuedEPType(Class container, Class component) {
        this.container = container;
        this.component = component;
    }

    public Class getContainer() {
        return container;
    }

    public Class getComponent() {
        return component;
    }

    public CodegenExpression codegen(CodegenMethod method, CodegenClassScope classScope, CodegenExpression typeInitSvcRef) {
        return newInstance(ClassMultiValuedEPType.class, constant(container), constant(component));
    }
}
