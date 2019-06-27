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
package com.espertech.esper.common.internal.event.json.getter.provided;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterMappedSPI;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.lang.reflect.Field;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property getter for Json underlying fields.
 */
public final class JsonGetterMapRuntimeKeyedProvided implements EventPropertyGetterMappedSPI {
    private final Field field;

    public JsonGetterMapRuntimeKeyedProvided(Field field) {
        this.field = field;
    }

    public CodegenExpression eventBeanGetMappedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        return staticMethod(CollectionUtil.class, "getMapValueChecked", exprDotName(castUnderlying(field.getDeclaringClass(), beanExpression), field.getName()), key);
    }

    public Object get(EventBean eventBean, String mapKey) throws PropertyAccessException {
        return JsonFieldGetterHelperProvided.getJsonProvidedMappedProp(eventBean.getUnderlying(), field, mapKey);
    }
}
