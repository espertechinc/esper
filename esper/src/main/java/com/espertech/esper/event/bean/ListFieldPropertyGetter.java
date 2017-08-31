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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterAndIndexed;
import com.espertech.esper.event.vaevent.PropertyUtility;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Field;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

/**
 * Getter for a list property backed by a field, identified by a given index, using vanilla reflection.
 */
public class ListFieldPropertyGetter extends BaseNativePropertyGetter implements BeanEventPropertyGetter, EventPropertyGetterAndIndexed {
    private final Field field;
    private final int index;

    /**
     * Constructor.
     *
     * @param field               is the field to use to retrieve a value from the object
     * @param index               is tge index within the array to get the property from
     * @param eventAdapterService factory for event beans and event types
     */
    public ListFieldPropertyGetter(Field field, int index, EventAdapterService eventAdapterService) {
        super(eventAdapterService, JavaClassHelper.getGenericFieldType(field, false), null);
        this.index = index;
        this.field = field;

        if (index < 0) {
            throw new IllegalArgumentException("Invalid negative index value");
        }
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        return getBeanPropInternal(eventBean.getUnderlying(), index);
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        return getBeanPropInternal(object, index);
    }

    private Object getBeanPropInternal(Object object, int index) throws PropertyAccessException {
        try {
            Object value = field.get(object);
            if (!(value instanceof List)) {
                return null;
            }
            List valueList = (List) value;
            if (valueList.size() <= index) {
                return null;
            }
            return valueList.get(index);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(field, object, e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(field, e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(field, e);
        }
    }

    private CodegenMethodNode getBeanPropInternalCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(getBeanPropType(), this.getClass(), codegenClassScope).addParam(getTargetType(), "object").addParam(int.class, "index").getBlock()
                .declareVar(Object.class, "value", exprDotName(ref("object"), field.getName()))
                .ifRefNotTypeReturnConst("value", List.class, null)
                .declareVar(List.class, "l", cast(List.class, ref("value")))
                .ifConditionReturnConst(relational(exprDotMethod(ref("l"), "size"), LE, ref("index")), null)
                .methodReturn(cast(getBeanPropType(), exprDotMethod(ref("l"), "get", ref("index"))));
    }

    public boolean isBeanExistsProperty(Object object) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        Object underlying = obj.getUnderlying();
        return getBeanProp(underlying);
    }

    public String toString() {
        return "ListFieldPropertyGetter " +
                " field=" + field.toString() +
                " index=" + index;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Class getBeanPropType() {
        return JavaClassHelper.getGenericFieldType(field, false);
    }

    public Class getTargetType() {
        return field.getDeclaringClass();
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(getTargetType(), beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getBeanPropInternalCodegen(codegenMethodScope, codegenClassScope), underlyingExpression, constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanGetIndexedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        return localMethod(getBeanPropInternalCodegen(codegenMethodScope, codegenClassScope), castUnderlying(getTargetType(), beanExpression), key);
    }
}
