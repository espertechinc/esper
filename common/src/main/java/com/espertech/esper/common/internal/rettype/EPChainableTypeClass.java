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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Any Java primitive type as well as any class and other non-array or non-collection type
 */
public class EPChainableTypeClass implements EPChainableType {
    public final static EPTypeClass EPTYPE = new EPTypeClass(EPChainableTypeClass.class);

    private final EPTypeClass type;

    public EPChainableTypeClass(Class type) {
        this.type = EPTypePremade.getOrCreate(type);
    }

    public EPChainableTypeClass(EPTypeClass type) {
        this.type = type;
    }

    public static EPTypeClass fromInputOrNull(EPChainableType inputType) {
        if (inputType instanceof EPChainableTypeClass) {
            return ((EPChainableTypeClass) inputType).getType();
        }
        return null;
    }

    public EPTypeClass getType() {
        return type;
    }

    public CodegenExpression codegen(CodegenMethod method, CodegenClassScope classScope, CodegenExpression typeInitSvcRef) {
        return newInstance(EPChainableTypeClass.EPTYPE, constant(type));
    }
}
