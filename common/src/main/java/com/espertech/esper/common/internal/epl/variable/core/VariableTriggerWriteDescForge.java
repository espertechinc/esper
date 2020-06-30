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
package com.espertech.esper.common.internal.epl.variable.core;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.event.core.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class VariableTriggerWriteDescForge extends VariableTriggerWriteForge {
    private final EventTypeSPI type;
    private final String variableName;
    private final EventPropertyWriterSPI writer;
    private final EventPropertyGetterSPI getter;
    private final EPType getterType;
    private final EPType evaluationType;

    public VariableTriggerWriteDescForge(EventTypeSPI type, String variableName, EventPropertyWriterSPI writer, EventPropertyGetterSPI getter, EPType getterType, EPType evaluationType) {
        this.type = type;
        this.variableName = variableName;
        this.writer = writer;
        this.getter = getter;
        this.getterType = getterType;
        this.evaluationType = evaluationType;
    }

    public String getVariableName() {
        return variableName;
    }

    public EventPropertyWriterSPI getWriter() {
        return writer;
    }

    public EventTypeSPI getType() {
        return type;
    }

    public EventPropertyValueGetterForge getGetter() {
        return getter;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(VariableTriggerWriteDesc.EPTYPE, this.getClass(), classScope);
        method.getBlock()
                .declareVarNewInstance(VariableTriggerWriteDesc.EPTYPE, "desc")
                .exprDotMethod(ref("desc"), "setType", EventTypeUtility.resolveTypeCodegen(type, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("desc"), "setVariableName", constant(variableName))
                .exprDotMethod(ref("desc"), "setWriter", EventTypeUtility.codegenWriter(type, evaluationType, writer, method, this.getClass(), classScope))
                .exprDotMethod(ref("desc"), "setGetter", EventTypeUtility.codegenGetterWCoerce(getter, getterType, null, method, this.getClass(), classScope))
                .methodReturn(ref("desc"));
        return localMethod(method);
    }
}
