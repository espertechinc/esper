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
package com.espertech.esper.common.internal.epl.dataflow.ops;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeResult;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.Map;

import static com.espertech.esper.common.internal.epl.dataflow.core.EPDataFlowServiceImpl.OP_PACKAGE_NAME;

public class EventBusSinkForge implements DataFlowOperatorForge {

    @DataFlowOpParameter
    private Map collector;

    private EventType[] eventTypes;

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        if (!context.getOutputPorts().isEmpty()) {
            throw new IllegalArgumentException("EventBusSink operator does not provide an output stream");
        }

        eventTypes = new EventType[context.getInputPorts().size()];
        for (int i = 0; i < eventTypes.length; i++) {
            eventTypes[i] = context.getInputPorts().get(i).getTypeDesc().getEventType();
        }

        return null;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(OP_PACKAGE_NAME + ".eventbussink.EventBusSinkFactory", this.getClass(), "sink", parent, symbols, classScope)
                .eventtypesMayNull("eventTypes", eventTypes)
                .map("collector", collector)
                .build();
    }
}
