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
package com.espertech.esper.common.internal.rettype;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class EPChainableTypeEventSingle implements EPChainableType {
    public final static EPTypeClass EPTYPE = new EPTypeClass(EPChainableTypeEventSingle.class);

    private final EventType type;

    public EPChainableTypeEventSingle(EventType type) {
        this.type = type;
    }

    public static EventType fromInputOrNull(EPChainableType inputType) {
        if (inputType instanceof EPChainableTypeEventSingle) {
            return ((EPChainableTypeEventSingle) inputType).getType();
        }
        return null;
    }

    public EventType getType() {
        return type;
    }

    public CodegenExpression codegen(CodegenMethod method, CodegenClassScope classScope, CodegenExpression typeInitSvcRef) {
        return newInstance(EPChainableTypeEventSingle.EPTYPE, EventTypeUtility.resolveTypeCodegen(this.type, typeInitSvcRef));
    }
}
