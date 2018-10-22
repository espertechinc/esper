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
package com.espertech.esper.common.internal.event.bean.manufacturer;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;

import java.lang.reflect.Constructor;

public class InstanceManufacturerFactoryFastCtor implements InstanceManufacturerFactory {
    private final Class targetClass;
    private final Constructor ctor;
    private final ExprForge[] forges;

    public InstanceManufacturerFactoryFastCtor(Class targetClass, Constructor ctor, ExprForge[] forges) {
        this.targetClass = targetClass;
        this.ctor = ctor;
        this.forges = forges;
    }

    public InstanceManufacturer makeEvaluator() {
        return new InstanceManufacturerFastCtor(this, ExprNodeUtilityQuery.getEvaluatorsNoCompile(forges));
    }

    public CodegenExpression codegen(Object forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return InstanceManufacturerFastCtor.codegen(codegenMethodScope, exprSymbol, codegenClassScope, targetClass, forges);
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public Constructor getCtor() {
        return ctor;
    }
}
