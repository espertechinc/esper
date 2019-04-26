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
package com.espertech.esper.common.internal.compile.multikey;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

public class MultiKeyClassRefEmpty implements MultiKeyClassRef {
    public final static MultiKeyClassRefEmpty INSTANCE = new MultiKeyClassRefEmpty();

    private MultiKeyClassRefEmpty() {
    }

    public String getClassNameMK() {
        return null;
    }

    public Class[] getMKTypes() {
        return new Class[0];
    }

    public CodegenExpression getExprMKSerde(CodegenMethod method, CodegenClassScope classScope) {
        return constantNull();
    }
}
