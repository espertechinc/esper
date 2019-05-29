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
package com.espertech.esper.common.internal.avro.getter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.avro.core.AvroEventPropertyGetter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import org.apache.avro.generic.GenericData;

import java.util.Collection;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterIndexed implements AvroEventPropertyGetter {
    private final int pos;
    private final int index;
    private final EventType fragmentEventType;
    private final EventBeanTypedEventFactory eventAdapterService;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param values coll
     * @param index  index
     * @return value
     */
    public static Object getAvroIndexedValue(Collection values, int index) {
        if (values == null) {
            return null;
        }
        if (values instanceof List) {
            List list = (List) values;
            return list.size() > index ? list.get(index) : null;
        }
        return values.toArray()[index];
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param values coll
     * @param index  index
     * @return value
     */
    public static boolean getAvroIndexedExists(Collection values, int index) {
        if (values == null) {
            return false;
        }
        return values.size() > index;
    }

    public AvroEventBeanGetterIndexed(int pos, int index, EventType fragmentEventType, EventBeanTypedEventFactory eventAdapterService) {
        this.pos = pos;
        this.index = index;
        this.fragmentEventType = fragmentEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Collection values = (Collection) record.get(pos);
        return getAvroIndexedValue(values, index);
    }

    public Object getAvroFieldValue(GenericData.Record record) {
        Collection values = (Collection) record.get(pos);
        return getAvroIndexedValue(values, index);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isExistsPropertyAvro((GenericData.Record) eventBean.getUnderlying());
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        Collection values = (Collection) record.get(pos);
        return getAvroIndexedExists(values, index);
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        return getAvroFragment(record);
    }

    public Object getAvroFragment(GenericData.Record record) {
        if (fragmentEventType == null) {
            return null;
        }
        Object value = getAvroFieldValue(record);
        if (value == null) {
            return null;
        }
        return eventAdapterService.adapterForTypedAvro(value, fragmentEventType);
    }

    private CodegenMethod getAvroFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField eventType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(fragmentEventType, EPStatementInitServices.REF));
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(GenericData.Record.class, "record").getBlock()
                .declareVar(Object.class, "value", underlyingGetCodegen(ref("record"), codegenMethodScope, codegenClassScope))
                .methodReturn(exprDotMethod(factory, "adapterForTypedAvro", ref("value"), eventType));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(GenericData.Record.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(GenericData.Record.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(GenericData.Record.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpression values = cast(Collection.class, exprDotMethod(underlyingExpression, "get", constant(pos)));
        return staticMethod(this.getClass(), "getAvroIndexedValue", values, constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpression values = cast(Collection.class, exprDotMethod(underlyingExpression, "get", constant(pos)));
        return staticMethod(this.getClass(), "getAvroIndexedExists", values, constant(index));
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (fragmentEventType == null) {
            return constantNull();
        }
        return localMethod(getAvroFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }
}
