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
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.avro.core.AvroConstant;
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

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterNestedIndexed implements EventPropertyGetterSPI {
    private final int top;
    private final int pos;
    private final int index;
    private final EventType fragmentEventType;
    private final EventBeanTypedEventFactory eventAdapterService;

    public AvroEventBeanGetterNestedIndexed(int top, int pos, int index, EventType fragmentEventType, EventBeanTypedEventFactory eventAdapterService) {
        this.top = top;
        this.pos = pos;
        this.index = index;
        this.fragmentEventType = fragmentEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(top);
        if (inner == null) {
            return null;
        }
        Collection collection = (Collection) inner.get(pos);
        return AvroEventBeanGetterIndexed.getAvroIndexedValue(collection, index);
    }

    private CodegenMethod getCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(EPTypePremade.OBJECT.getEPType(), this.getClass(), codegenClassScope).addParam(AvroConstant.EPTYPE_RECORD, "record").getBlock()
                .declareVar(AvroConstant.EPTYPE_RECORD, "inner", cast(AvroConstant.EPTYPE_RECORD, exprDotMethod(ref("record"), "get", constant(top))))
                .ifRefNullReturnNull("inner")
                .declareVar(EPTypePremade.COLLECTION.getEPType(), "collection", cast(EPTypePremade.COLLECTION.getEPType(), exprDotMethod(ref("inner"), "get", constant(pos))))
                .methodReturn(staticMethod(AvroEventBeanGetterIndexed.class, "getAvroIndexedValue", ref("collection"), constant(index)));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
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
        CodegenExpressionField eventType = codegenClassScope.addFieldUnshared(true, EventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(fragmentEventType, EPStatementInitServices.REF));
        return codegenMethodScope.makeChild(EPTypePremade.OBJECT.getEPType(), this.getClass(), codegenClassScope).addParam(AvroConstant.EPTYPE_RECORD, "record").getBlock()
                .declareVar(EPTypePremade.OBJECT.getEPType(), "value", underlyingGetCodegen(ref("record"), codegenMethodScope, codegenClassScope))
                .ifRefNullReturnNull("value")
                .methodReturn(exprDotMethod(factory, "adapterForTypedAvro", ref("value"), eventType));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(AvroConstant.EPTYPE_RECORD, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(AvroConstant.EPTYPE_RECORD, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (fragmentEventType == null) {
            return constantNull();
        }
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }
}
