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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterIndexedSPI;
import org.apache.avro.generic.GenericData;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterIndexedRuntimeKeyed implements EventPropertyGetterIndexedSPI {
    private final int pos;

    public AvroEventBeanGetterIndexedRuntimeKeyed(int pos) {
        this.pos = pos;
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Collection values = (Collection) record.get(pos);
        return AvroEventBeanGetterIndexed.getAvroIndexedValue(values, index);
    }

    public CodegenExpression eventBeanGetIndexedCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope, CodegenExpression beanExpression, CodegenExpression key) {
        CodegenMethod method = codegenMethodScope.makeChild(Object.class, AvroEventBeanGetterIndexedRuntimeKeyed.class, codegenClassScope).addParam(EventBean.class, "event").addParam(int.class, "index").getBlock()
                .declareVar(GenericData.Record.class, "record", castUnderlying(GenericData.Record.class, ref("event")))
                .declareVar(Collection.class, "values", cast(Collection.class, exprDotMethod(ref("record"), "get", constant(pos))))
                .methodReturn(staticMethod(AvroEventBeanGetterIndexed.class, "getAvroIndexedValue", ref("values"), ref("index")));
        return localMethodBuild(method).pass(beanExpression).pass(key).call();
    }
}
