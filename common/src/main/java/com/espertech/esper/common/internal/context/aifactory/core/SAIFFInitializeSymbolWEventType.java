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
package com.espertech.esper.common.internal.context.aifactory.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class SAIFFInitializeSymbolWEventType extends SAIFFInitializeSymbol {
    public final static CodegenExpressionRef REF_EVENTTYPE = ref("eventType");

    private CodegenExpressionRef optionalEventTypeRef;

    public CodegenExpressionRef getAddEventType(CodegenMethodScope scope) {
        if (optionalEventTypeRef == null) {
            optionalEventTypeRef = REF_EVENTTYPE;
        }
        scope.addSymbol(optionalEventTypeRef);
        return optionalEventTypeRef;
    }

    public void provide(Map<String, Class> symbols) {
        super.provide(symbols);
        if (optionalEventTypeRef != null) {
            symbols.put(optionalEventTypeRef.getRef(), EventType.class);
        }
    }
}
