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
package com.espertech.esper.common.internal.event.variant;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class VariantPropertyGetterCacheCodegenField implements CodegenFieldSharable {

    private final VariantEventType variantEventType;

    public VariantPropertyGetterCacheCodegenField(VariantEventType variantEventType) {
        this.variantEventType = variantEventType;
    }

    public Class type() {
        return VariantPropertyGetterCache.class;
    }

    public CodegenExpression initCtorScoped() {
        CodegenExpression type = cast(VariantEventType.class, EventTypeUtility.resolveTypeCodegen(variantEventType, EPStatementInitServices.REF));
        return exprDotMethod(type, "getVariantPropertyGetterCache");
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariantPropertyGetterCacheCodegenField that = (VariantPropertyGetterCacheCodegenField) o;

        return variantEventType.equals(that.variantEventType);
    }

    public int hashCode() {
        return variantEventType.hashCode();
    }
}
