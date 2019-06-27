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
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class JsonEventBeanPropertyWriter implements EventPropertyWriterSPI {

    protected final JsonDelegateFactory delegateFactory;
    protected final JsonUnderlyingField field;

    public JsonEventBeanPropertyWriter(JsonDelegateFactory delegateFactory, JsonUnderlyingField field) {
        this.delegateFactory = delegateFactory;
        this.field = field;
    }

    public void write(Object value, EventBean target) {
        write(value, target.getUnderlying());
    }

    public void write(Object value, Object und) {
        delegateFactory.setValue(field.getPropertyNumber(), value, und);
    }

    public CodegenExpression writeCodegen(CodegenExpression assigned, CodegenExpression und, CodegenExpression target, CodegenMethodScope parent, CodegenClassScope classScope) {
        return assign(exprDotName(und, field.getFieldName()), assigned);
    }
}
