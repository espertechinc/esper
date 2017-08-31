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
package com.espertech.esper.avro.getter;

import com.espertech.esper.avro.core.AvroEventPropertyGetter;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import org.apache.avro.generic.GenericData;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterMapped implements AvroEventPropertyGetter {
    private final int pos;
    private final String key;

    public AvroEventBeanGetterMapped(int pos, String key) {
        this.pos = pos;
        this.key = key;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Map values = (Map) record.get(pos);
        return getAvroMappedValueWNullCheck(values, key);
    }

    public Object getAvroFieldValue(GenericData.Record record) {
        Map values = (Map) record.get(pos);
        return getAvroMappedValueWNullCheck(values, key);
    }

    private CodegenMethodNode getAvroFieldValueCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(GenericData.Record.class, "record").getBlock()
                .declareVar(Map.class, "values", cast(Map.class, exprDotMethod(ref("record"), "get", constant(pos))))
                .ifRefNullReturnNull("values")
                .methodReturn(exprDotMethod(ref("values"), "get", constant(key)));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }

    public Object getAvroFragment(GenericData.Record record) {
        return null;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(GenericData.Record.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getAvroFieldValueCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param map map
     * @param key key
     * @return value
     */
    public static Object getAvroMappedValueWNullCheck(Map map, String key) {
        if (map == null) {
            return null;
        }
        return map.get(key);
    }
}
