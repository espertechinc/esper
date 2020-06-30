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
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.avro.core.AvroConstant;
import com.espertech.esper.common.internal.avro.core.AvroEventPropertyGetter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import org.apache.avro.generic.GenericData;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterNestedIndexRooted implements EventPropertyGetterSPI {
    private final int posTop;
    private final int index;
    private final AvroEventPropertyGetter nested;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param record record
     * @param posTop postop
     * @param index  index
     * @return value
     * @throws PropertyAccessException ex
     */
    public static GenericData.Record getAtIndex(GenericData.Record record, int posTop, int index) throws PropertyAccessException {
        Collection values = (Collection) record.get(posTop);
        Object value = AvroEventBeanGetterIndexed.getAvroIndexedValue(values, index);
        if (value == null || !(value instanceof GenericData.Record)) {
            return null;
        }
        return (GenericData.Record) value;
    }

    public AvroEventBeanGetterNestedIndexRooted(int posTop, int index, AvroEventPropertyGetter nested) {
        this.posTop = posTop;
        this.index = index;
        this.nested = nested;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = getAtIndex(record, posTop, index);
        return inner == null ? null : nested.getAvroFieldValue(inner);
    }

    private CodegenMethod getCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(EPTypePremade.OBJECT.getEPType(), this.getClass(), codegenClassScope).addParam(AvroConstant.EPTYPE_RECORD, "record").getBlock()
                .declareVar(AvroConstant.EPTYPE_RECORD, "inner", staticMethod(this.getClass(), "getAtIndex", ref("record"), constant(posTop), constant(index)))
                .ifRefNullReturnNull("inner")
                .methodReturn(nested.underlyingGetCodegen(ref("inner"), codegenMethodScope, codegenClassScope));
    }

    private CodegenMethod existsCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), this.getClass(), codegenClassScope).addParam(AvroConstant.EPTYPE_RECORD, "record").getBlock()
            .declareVar(AvroConstant.EPTYPE_RECORD, "inner", staticMethod(this.getClass(), "getAtIndex", ref("record"), constant(posTop), constant(index)))
            .ifRefNullReturnFalse("inner")
            .methodReturn(nested.underlyingExistsCodegen(ref("inner"), codegenMethodScope, codegenClassScope));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Collection values = (Collection) record.get(posTop);
        return AvroEventBeanGetterIndexed.getAvroIndexedExists(values, index);
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Collection values = (Collection) record.get(posTop);
        Object value = AvroEventBeanGetterIndexed.getAvroIndexedValue(values, index);
        if (value == null || !(value instanceof GenericData.Record)) {
            return null;
        }
        return nested.getAvroFragment((GenericData.Record) value);
    }

    private CodegenMethod getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(EPTypePremade.OBJECT.getEPType(), this.getClass(), codegenClassScope).addParam(AvroConstant.EPTYPE_RECORD, "record").getBlock()
                .declareVar(EPTypePremade.COLLECTION.getEPType(), "values", cast(EPTypePremade.COLLECTION.getEPType(), exprDotMethod(ref("record"), "get", constant(posTop))))
                .declareVar(EPTypePremade.OBJECT.getEPType(), "value", staticMethod(AvroEventBeanGetterIndexed.class, "getAvroIndexedValue", ref("values"), constant(index)))
                .ifRefNullReturnNull("value")
                .ifRefNotTypeReturnConst("value", AvroConstant.EPTYPE_RECORD, null)
                .methodReturn(nested.underlyingFragmentCodegen(cast(AvroConstant.EPTYPE_RECORD, ref("value")), codegenMethodScope, codegenClassScope));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(AvroConstant.EPTYPE_RECORD, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(AvroConstant.EPTYPE_RECORD, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(AvroConstant.EPTYPE_RECORD, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(existsCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }
}
