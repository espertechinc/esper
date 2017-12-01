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
package com.espertech.esper.event.bean;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.ops.ExprNewInstanceNodeForge;
import net.sf.cglib.reflect.FastConstructor;

public class InstanceManufacturerFactoryFastCtor implements InstanceManufacturerFactory {
    private final Class targetClass;
    private final FastConstructor ctor;
    private final ExprForge[] forges;

    public InstanceManufacturerFactoryFastCtor(Class targetClass, FastConstructor ctor, ExprForge[] forges) {
        this.targetClass = targetClass;
        this.ctor = ctor;
        this.forges = forges;
    }

    public InstanceManufacturer makeEvaluator() {
        return new InstanceManufacturerFastCtor(this, ExprNodeUtilityCore.getEvaluatorsNoCompile(forges));
    }

    public CodegenExpression codegen(ExprNewInstanceNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return InstanceManufacturerFastCtor.codegen(codegenMethodScope, exprSymbol, codegenClassScope, targetClass, forges);
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public FastConstructor getCtor() {
        return ctor;
    }
}
