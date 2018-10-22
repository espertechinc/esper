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
package com.espertech.esper.common.internal.event.map;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventPropertyWriterSPI;
import com.espertech.esper.common.internal.event.core.MappedEventBean;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class MapEventBeanPropertyWriter implements EventPropertyWriterSPI {

    protected final String propertyName;

    public MapEventBeanPropertyWriter(String propertyName) {
        this.propertyName = propertyName;
    }

    public void write(Object value, EventBean target) {
        MappedEventBean map = (MappedEventBean) target;
        write(value, map.getProperties());
    }

    public void write(Object value, Map<String, Object> map) {
        map.put(propertyName, value);
    }

    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return exprDotMethod(und, "put", constant(propertyName), assigned);
    }
}
