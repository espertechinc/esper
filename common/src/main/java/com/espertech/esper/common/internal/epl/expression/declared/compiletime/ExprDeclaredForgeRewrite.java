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
package com.espertech.esper.common.internal.epl.expression.declared.compiletime;


import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprForgeConstantType;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDeclaredForgeRewrite extends ExprDeclaredForgeBase {
    private final int[] streamAssignments;

    public ExprDeclaredForgeRewrite(ExprDeclaredNodeImpl parent, ExprForge innerForge, boolean isCache, int[] streamAssignments, boolean audit, String statementName) {
        super(parent, innerForge, isCache, audit, statementName);
        this.streamAssignments = streamAssignments;
    }

    public EventBean[] getEventsPerStreamRewritten(EventBean[] eps) {

        // rewrite streams
        EventBean[] events = new EventBean[streamAssignments.length];
        for (int i = 0; i < streamAssignments.length; i++) {
            events[i] = eps[streamAssignments[i]];
        }

        return events;
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    protected CodegenExpression codegenEventsPerStreamRewritten(CodegenExpression eventsPerStream, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(EventBean[].class, ExprDeclaredForgeRewrite.class, codegenClassScope).addParam(EventBean[].class, "eps").getBlock()
                .declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, constant(streamAssignments.length)));
        for (int i = 0; i < streamAssignments.length; i++) {
            block.assignArrayElement("events", constant(i), arrayAtIndex(ref("eps"), constant(streamAssignments[i])));
        }
        return localMethodBuild(block.methodReturn(ref("events"))).pass(eventsPerStream).call();
    }
}