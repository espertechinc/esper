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
import com.espertech.esper.common.internal.avro.core.AvroEventPropertyGetter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterDynamicPoly implements AvroEventPropertyGetter {
    private final AvroEventPropertyGetter[] getters;

    public AvroEventBeanGetterDynamicPoly(AvroEventPropertyGetter[] getters) {
        this.getters = getters;
    }

    public Object getAvroFieldValue(GenericData.Record record) {
        return getAvroFieldValuePoly(record, getters);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        return getAvroFieldValue(record);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isExistsPropertyAvro((GenericData.Record) eventBean.getUnderlying());
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }

    public Object getAvroFragment(GenericData.Record record) {
        return null;
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return getAvroFieldValuePolyExists(record, getters);
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(GenericData.Record.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(GenericData.Record.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getAvroFieldValuePolyCodegen(codegenMethodScope, codegenClassScope, getters), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getAvroFieldValuePolyExistsCodegen(codegenMethodScope, codegenClassScope, getters), underlyingExpression);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    static boolean getAvroFieldValuePolyExists(GenericData.Record record, AvroEventPropertyGetter[] getters) {
        if (record == null) {
            return false;
        }
        record = navigatePoly(record, getters);
        return record != null && getters[getters.length - 1].isExistsPropertyAvro(record);
    }

    static CodegenMethod getAvroFieldValuePolyExistsCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, AvroEventPropertyGetter[] getters) {
        return codegenMethodScope.makeChild(boolean.class, AvroEventBeanGetterDynamicPoly.class, codegenClassScope).addParam(GenericData.Record.class, "record").getBlock()
                .ifRefNullReturnFalse("record")
                .assignRef("record", localMethod(navigatePolyCodegen(codegenMethodScope, codegenClassScope, getters), ref("record")))
                .ifRefNullReturnFalse("record")
                .methodReturn(getters[getters.length - 1].underlyingExistsCodegen(ref("record"), codegenMethodScope, codegenClassScope));
    }

    static Object getAvroFieldValuePoly(GenericData.Record record, AvroEventPropertyGetter[] getters) {
        if (record == null) {
            return null;
        }
        record = navigatePoly(record, getters);
        if (record == null) {
            return null;
        }
        return getters[getters.length - 1].getAvroFieldValue(record);
    }

    static CodegenMethod getAvroFieldValuePolyCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, AvroEventPropertyGetter[] getters) {
        return codegenMethodScope.makeChild(Object.class, AvroEventBeanGetterDynamicPoly.class, codegenClassScope).addParam(GenericData.Record.class, "record").getBlock()
                .ifRefNullReturnNull("record")
                .assignRef("record", localMethod(navigatePolyCodegen(codegenMethodScope, codegenClassScope, getters), ref("record")))
                .ifRefNullReturnNull("record")
                .methodReturn(getters[getters.length - 1].underlyingGetCodegen(ref("record"), codegenMethodScope, codegenClassScope));
    }

    static Object getAvroFieldFragmentPoly(GenericData.Record record, AvroEventPropertyGetter[] getters) {
        if (record == null) {
            return null;
        }
        record = navigatePoly(record, getters);
        if (record == null) {
            return null;
        }
        return getters[getters.length - 1].getAvroFragment(record);
    }

    static CodegenMethod getAvroFieldFragmentPolyCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, AvroEventPropertyGetter[] getters) {
        return codegenMethodScope.makeChild(Object.class, AvroEventBeanGetterDynamicPoly.class, codegenClassScope).addParam(GenericData.Record.class, "record").getBlock()
                .ifRefNullReturnNull("record")
                .assignRef("record", localMethod(navigatePolyCodegen(codegenMethodScope, codegenClassScope, getters), ref("record")))
                .ifRefNullReturnNull("record")
                .methodReturn(getters[getters.length - 1].underlyingFragmentCodegen(ref("record"), codegenMethodScope, codegenClassScope));
    }

    private static GenericData.Record navigatePoly(GenericData.Record record, AvroEventPropertyGetter[] getters) {
        for (int i = 0; i < getters.length - 1; i++) {
            Object value = getters[i].getAvroFieldValue(record);
            if (!(value instanceof GenericData.Record)) {
                return null;
            }
            record = (GenericData.Record) value;
        }
        return record;
    }

    private static CodegenMethod navigatePolyCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, AvroEventPropertyGetter[] getters) {
        CodegenBlock block = codegenMethodScope.makeChild(GenericData.Record.class, AvroEventBeanGetterDynamicPoly.class, codegenClassScope).addParam(GenericData.Record.class, "record").getBlock();
        block.declareVar(Object.class, "value", constantNull());
        for (int i = 0; i < getters.length - 1; i++) {
            block.assignRef("value", getters[i].underlyingGetCodegen(ref("record"), codegenMethodScope, codegenClassScope))
                    .ifRefNotTypeReturnConst("value", GenericData.Record.class, null)
                    .assignRef("record", cast(GenericData.Record.class, ref("value")));
        }
        return block.methodReturn(ref("record"));
    }
}
