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
import com.espertech.esper.common.client.dataflow.util.DataFlowParameterValidation;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeResult;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInputPort;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.Map;

import static com.espertech.esper.common.internal.epl.dataflow.core.EPDataFlowServiceImpl.OP_PACKAGE_NAME;

public class LogSinkForge implements DataFlowOperatorForge {

    @DataFlowOpParameter
    private ExprNode title;

    @DataFlowOpParameter
    private ExprNode layout;

    @DataFlowOpParameter
    private ExprNode format;

    @DataFlowOpParameter
    private ExprNode log;

    @DataFlowOpParameter
    private ExprNode linefeed;

    private EventType[] eventTypes;

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        if (!context.getOutputPorts().isEmpty()) {
            throw new IllegalArgumentException("LogSink operator does not provide an output stream");
        }

        eventTypes = new EventType[context.getInputPorts().size()];
        for (Map.Entry<Integer, DataFlowOpInputPort> entry : context.getInputPorts().entrySet()) {
            eventTypes[entry.getKey()] = entry.getValue().getTypeDesc().getEventType();
        }

        title = DataFlowParameterValidation.validate("title", title, String.class, context);
        layout = DataFlowParameterValidation.validate("layout", layout, String.class, context);
        format = DataFlowParameterValidation.validate("format", format, String.class, context);
        log = DataFlowParameterValidation.validate("log", log, boolean.class, context);
        linefeed = DataFlowParameterValidation.validate("linefeed", linefeed, boolean.class, context);
        return null;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(OP_PACKAGE_NAME + ".logsink.LogSinkFactory", this.getClass(), "log", parent, symbols, classScope)
                .exprnode("title", title)
                .exprnode("layout", layout)
                .exprnode("format", format)
                .exprnode("log", log)
                .exprnode("linefeed", linefeed)
                .eventtypesMayNull("eventTypes", eventTypes)
                .build();
    }
}
