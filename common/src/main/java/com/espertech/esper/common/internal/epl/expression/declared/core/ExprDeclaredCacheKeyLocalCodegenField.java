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
package com.espertech.esper.common.internal.epl.expression.declared.core;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class ExprDeclaredCacheKeyLocalCodegenField implements CodegenFieldSharable {

    private final String expressionName;

    public ExprDeclaredCacheKeyLocalCodegenField(String expressionName) {
        this.expressionName = expressionName;
    }

    public EPTypeClass type() {
        return EPTypePremade.OBJECT.getEPType();
    }

    public CodegenExpression initCtorScoped() {
        return newInstance(EPTypePremade.OBJECT.getEPType());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprDeclaredCacheKeyLocalCodegenField that = (ExprDeclaredCacheKeyLocalCodegenField) o;

        return expressionName.equals(that.expressionName);
    }

    public int hashCode() {
        return expressionName.hashCode();
    }
}
