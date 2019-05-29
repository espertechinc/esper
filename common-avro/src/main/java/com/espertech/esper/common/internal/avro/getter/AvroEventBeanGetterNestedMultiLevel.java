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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterNestedMultiLevel implements EventPropertyGetterSPI {
    private final int top;
    private final int[] path;
    private final EventType fragmentEventType;
    private final EventBeanTypedEventFactory eventAdapterService;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param record record
     * @param top    top index
     * @param path   path of indexes
     * @return value
     * @throws PropertyAccessException property access problem
     */
    public static Object getRecordValueTopWPath(GenericData.Record record, int top, int[] path) throws PropertyAccessException {
        GenericData.Record inner = (GenericData.Record) record.get(top);
        if (inner == null) {
            return null;
        }
        for (int i = 0; i < path.length - 1; i++) {
            inner = (GenericData.Record) inner.get(path[i]);
            if (inner == null) {
                return null;
            }
        }
        return inner.get(path[path.length - 1]);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param record record
     * @param top    top index
     * @param path   path of indexes
     * @return value
     * @throws PropertyAccessException property access problem
     */
    public static boolean existsRecordValueTopWPath(GenericData.Record record, int top, int[] path) throws PropertyAccessException {
        GenericData.Record inner = (GenericData.Record) record.get(top);
        if (inner == null) {
            return false;
        }
        for (int i = 0; i < path.length - 1; i++) {
            inner = (GenericData.Record) inner.get(path[i]);
            if (inner == null) {
                return false;
            }
        }
        return true;
    }

    public AvroEventBeanGetterNestedMultiLevel(int top, int[] path, EventType fragmentEventType, EventBeanTypedEventFactory eventAdapterService) {
        this.top = top;
        this.path = path;
        this.fragmentEventType = fragmentEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getRecordValueTopWPath((GenericData.Record) eventBean.getUnderlying(), top, path);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return existsRecordValueTopWPath((GenericData.Record) eventBean.getUnderlying(), top, path);
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        if (fragmentEventType == null) {
            return null;
        }
        Object value = get(eventBean);
        if (value == null) {
            return null;
        }
        return eventAdapterService.adapterForTypedAvro(value, fragmentEventType);
    }

    private CodegenMethod getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField eventType = codegenClassScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(fragmentEventType, EPStatementInitServices.REF));
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(GenericData.Record.class, "record").getBlock()
                .declareVar(Object.class, "value", underlyingGetCodegen(ref("record"), codegenMethodScope, codegenClassScope))
                .ifRefNullReturnNull("value")
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
        return staticMethod(AvroEventBeanGetterNestedMultiLevel.class, "getRecordValueTopWPath", underlyingExpression, constant(top), constant(path));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(AvroEventBeanGetterNestedMultiLevel.class, "existsRecordValueTopWPath", underlyingExpression, constant(top), constant(path));
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }
}
