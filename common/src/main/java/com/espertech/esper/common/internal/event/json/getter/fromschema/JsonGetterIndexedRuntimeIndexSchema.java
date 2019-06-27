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
package com.espertech.esper.common.internal.event.json.getter.fromschema;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterIndexedSPI;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property getter for Json underlying fields.
 */
public final class JsonGetterIndexedRuntimeIndexSchema implements EventPropertyGetterIndexedSPI {
    private final JsonUnderlyingField field;

    public JsonGetterIndexedRuntimeIndexSchema(JsonUnderlyingField field) {
        this.field = field;
    }

    public CodegenExpression eventBeanGetIndexedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        return staticMethod(JsonFieldGetterHelperSchema.class, "getJsonIndexedProp", exprDotMethod(beanExpression, "getUnderlying"), constant(field.getPropertyNumber()), key);
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        return JsonFieldGetterHelperSchema.getJsonIndexedProp(eventBean.getUnderlying(), field.getPropertyNumber(), index);
    }
}
