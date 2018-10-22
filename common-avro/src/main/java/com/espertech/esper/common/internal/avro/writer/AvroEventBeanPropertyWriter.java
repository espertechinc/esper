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
package com.espertech.esper.common.internal.avro.writer;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.avro.core.AvroGenericDataBackedEventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventPropertyWriterSPI;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class AvroEventBeanPropertyWriter implements EventPropertyWriterSPI {

    protected final int index;

    public AvroEventBeanPropertyWriter(int index) {
        this.index = index;
    }

    public void write(Object value, EventBean target) {
        AvroGenericDataBackedEventBean avroEvent = (AvroGenericDataBackedEventBean) target;
        write(value, avroEvent.getProperties());
    }

    public void write(Object value, GenericData.Record record) {
        record.put(index, value);
    }

    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return exprDotMethod(und, "put", constant(index), assigned);
    }
}
