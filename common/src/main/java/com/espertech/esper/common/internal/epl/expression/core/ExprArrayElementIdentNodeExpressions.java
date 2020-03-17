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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

public class ExprArrayElementIdentNodeExpressions {
    private final CodegenExpression index;
    private final CodegenExpression arrayGet;

    public ExprArrayElementIdentNodeExpressions(CodegenExpression index, CodegenExpression arrayGet) {
        this.index = index;
        this.arrayGet = arrayGet;
    }

    public CodegenExpression getIndex() {
        return index;
    }

    public CodegenExpression getArrayGet() {
        return arrayGet;
    }
}
