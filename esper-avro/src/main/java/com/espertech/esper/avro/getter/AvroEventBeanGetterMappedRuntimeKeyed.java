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
import com.espertech.esper.event.EventPropertyGetterMappedSPI;
import org.apache.avro.generic.GenericData;

import java.util.Map;

import static com.espertech.esper.avro.getter.AvroEventBeanGetterMapped.getAvroMappedValueWNullCheck;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterMappedRuntimeKeyed implements EventPropertyGetterMappedSPI {
    private final int pos;

    public AvroEventBeanGetterMappedRuntimeKeyed(int pos) {
        this.pos = pos;
    }

    public Object get(EventBean event, String key) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) event.getUnderlying();
        Map values = (Map) record.get(pos);
        return getAvroMappedValueWNullCheck(values, key);
    }

    public CodegenExpression eventBeanGetMappedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        CodegenMethodNode method = codegenMethodScope.makeChild(Object.class, AvroEventBeanGetterMappedRuntimeKeyed.class, codegenClassScope).addParam(EventBean.class, "event").addParam(String.class, "key").getBlock()
                .declareVar(GenericData.Record.class, "record", castUnderlying(GenericData.Record.class, ref("event")))
                .declareVar(Map.class, "values", cast(Map.class, exprDotMethod(ref("record"), "get", constant(pos))))
                .methodReturn(staticMethod(AvroEventBeanGetterMapped.class, "getAvroMappedValueWNullCheck", ref("values"), ref("key")));
        return localMethodBuild(method).pass(beanExpression).pass(key).call();
    }
}
