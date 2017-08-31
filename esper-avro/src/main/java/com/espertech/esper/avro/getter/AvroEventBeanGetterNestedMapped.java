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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.EventPropertyGetterSPI;
import org.apache.avro.generic.GenericData;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterNestedMapped implements EventPropertyGetterSPI {
    private final int top;
    private final int pos;
    private final String key;

    public AvroEventBeanGetterNestedMapped(int top, int pos, String key) {
        this.top = top;
        this.pos = pos;
        this.key = key;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(top);
        if (inner == null) {
            return null;
        }
        Map map = (Map) inner.get(pos);
        return AvroEventBeanGetterMapped.getAvroMappedValueWNullCheck(map, key);
    }

    private CodegenMethodNode getCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(GenericData.Record.class, "record").getBlock()
                .declareVar(GenericData.Record.class, "inner", cast(GenericData.Record.class, exprDotMethod(ref("record"), "get", constant(top))))
                .ifRefNullReturnNull("inner")
                .declareVar(Map.class, "map", cast(Map.class, exprDotMethod(ref("inner"), "get", constant(pos))))
                .methodReturn(staticMethod(AvroEventBeanGetterMapped.class, "getAvroMappedValueWNullCheck", ref("map"), constant(key)));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
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
        return localMethod(getCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }
}
