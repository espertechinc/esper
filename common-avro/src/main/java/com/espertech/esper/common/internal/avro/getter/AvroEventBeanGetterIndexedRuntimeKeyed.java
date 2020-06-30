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
        CodegenMethod method = codegenMethodScope.makeChild(EPTypePremade.OBJECT.getEPType(), AvroEventBeanGetterIndexedRuntimeKeyed.class, codegenClassScope).addParam(EventBean.EPTYPE, "event").addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "index").getBlock()
                .declareVar(AvroConstant.EPTYPE_RECORD, "record", castUnderlying(AvroConstant.EPTYPE_RECORD, ref("event")))
                .declareVar(EPTypePremade.COLLECTION.getEPType(), "values", cast(EPTypePremade.COLLECTION.getEPType(), exprDotMethod(ref("record"), "get", constant(pos))))
                .methodReturn(staticMethod(AvroEventBeanGetterIndexed.class, "getAvroIndexedValue", ref("values"), ref("index")));
        return localMethodBuild(method).pass(beanExpression).pass(key).call();
    }
}
