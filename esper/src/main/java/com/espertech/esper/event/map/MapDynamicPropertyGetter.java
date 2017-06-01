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
package com.espertech.esper.event.map;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class MapDynamicPropertyGetter implements MapEventPropertyGetter {

    private final String propertyName;

    public MapDynamicPropertyGetter(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        return map.get(propertyName);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        return map.containsKey(propertyName);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Map map = (Map) eventBean.getUnderlying();
        return map.get(propertyName);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Map map = (Map) eventBean.getUnderlying();
        return map.containsKey(propertyName);
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return exprDotMethod(castUnderlying(Map.class, beanExpression), "get", constant(propertyName));
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return exprDotMethod(castUnderlying(Map.class, beanExpression), "containsKey", constant(propertyName));
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return exprDotMethod(underlyingExpression, "get", constant(propertyName));
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return exprDotMethod(underlyingExpression, "containsKey", constant(propertyName));
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }
}
