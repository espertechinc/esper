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
package com.espertech.esper.common.internal.event.arr;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventPropertyWriterSPI;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ObjectArrayEventBeanPropertyWriter implements EventPropertyWriterSPI {

    protected final int index;

    public ObjectArrayEventBeanPropertyWriter(int index) {
        this.index = index;
    }

    public void write(Object value, EventBean target) {
        ObjectArrayBackedEventBean arrayEvent = (ObjectArrayBackedEventBean) target;
        write(value, arrayEvent.getProperties());
    }

    public void write(Object value, Object[] array) {
        array[index] = value;
    }

    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return assign(arrayAtIndex(und, constant(index)), assigned);
    }
}
