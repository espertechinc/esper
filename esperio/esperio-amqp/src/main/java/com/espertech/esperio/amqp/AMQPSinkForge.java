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
package com.espertech.esperio.amqp;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowOpPropertyHolder;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeResult;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMQPSinkForge implements DataFlowOperatorForge {
    private static final Logger log = LoggerFactory.getLogger(AMQPSinkForge.class);

    @DataFlowOpPropertyHolder
    private AMQPSettingsSinkForge settings;

    private EventType eventType;

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        eventType = context.getInputPorts().isEmpty() ? null : context.getInputPorts().get(0).getTypeDesc().getEventType();
        return null;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        SAIFFInitializeBuilder builder = new SAIFFInitializeBuilder(AMQPSinkFactory.class, this.getClass(), "amqpSink", parent, symbols, classScope);
        builder
            .expression("settings", settings.make(builder.getMethod(), symbols, classScope))
            .eventtype("eventType", eventType);
        return builder.build();
    }
}
