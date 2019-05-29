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
package com.espertech.esper.common.internal.event.json.writer;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventPropertyWriterSPI;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class JsonEventBeanPropertyWriter implements EventPropertyWriterSPI {

    protected final JsonUnderlyingField field;

    public JsonEventBeanPropertyWriter(JsonUnderlyingField field) {
        this.field = field;
    }

    public void write(Object value, EventBean target) {
        write(value, (JsonEventObjectBase) target.getUnderlying());
    }

    public void write(Object value, JsonEventObjectBase und) {
        und.setNativeValue(field.getPropertyNumber(), value);
    }

    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return exprDotMethod(und, "setNativeValue", constant(field.getPropertyNumber()), assigned);
    }
}
