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
package com.espertech.esper.common.internal.epl.fafquery.processor;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public interface FireAndForgetProcessorForge {
    String getNamedWindowOrTableName();

    String getContextName();

    EventType getEventTypeRSPInputEvents();

    EventType getEventTypePublic();

    String[][] getUniqueIndexes();

    CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    static CodegenExpression makeArray(FireAndForgetProcessorForge[] processors, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(FireAndForgetProcessor[].class, FireAndForgetProcessorForge.class, classScope);
        method.getBlock().declareVar(FireAndForgetProcessor[].class, "processors", newArrayByLength(FireAndForgetProcessor.class, constant(processors.length)));
        for (int i = 0; i < processors.length; i++) {
            method.getBlock().assignArrayElement("processors", constant(i), processors[i].make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref("processors"));
        return localMethod(method);
    }
}
