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
package com.espertech.esper.common.internal.context.activator;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ViewableActivatorSubselectNoneForge implements ViewableActivatorForge {

    private final EventType eventType;

    public ViewableActivatorSubselectNoneForge(EventType eventType) {
        this.eventType = eventType;
    }

    public CodegenExpression makeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpressionField type = classScope.addFieldUnshared(true, EventType.class, EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF));
        CodegenMethod method = parent.makeChild(ViewableActivatorSubselectNone.class, this.getClass(), classScope);
        method.getBlock().declareVar(ViewableActivatorSubselectNone.class, "none", newInstance(ViewableActivatorSubselectNone.class))
                .exprDotMethod(ref("none"), "setEventType", type)
                .methodReturn(ref("none"));
        return localMethod(method);
    }
}
