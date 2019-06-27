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
package com.espertech.esper.common.internal.event.json.getter.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.util.CollectionUtil;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class JsonGetterIndexedBase implements JsonEventPropertyGetter {
    protected final int index;
    protected final String underlyingClassName;
    protected final EventType optionalInnerType;
    protected final EventBeanTypedEventFactory eventBeanTypedEventFactory;

    public abstract String getFieldName();

    public JsonGetterIndexedBase(int index, String underlyingClassName, EventType optionalInnerType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.index = index;
        this.underlyingClassName = underlyingClassName;
        this.optionalInnerType = optionalInnerType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public final CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(CollectionUtil.class, "arrayValueAtIndex", exprDotName(underlyingExpression, getFieldName()), constant(index));
    }

    public final CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public final CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(CollectionUtil.class, "arrayExistsAtIndex", exprDotName(underlyingExpression, getFieldName()), constant(index));
    }

    public final CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(underlyingClassName, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public final boolean isExistsProperty(EventBean eventBean) {
        return getJsonExists(eventBean.getUnderlying());
    }

    public final Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return getJsonFragment(eventBean.getUnderlying());
    }

    public final Object get(EventBean eventBean) throws PropertyAccessException {
        return getJsonProp(eventBean.getUnderlying());
    }
}
