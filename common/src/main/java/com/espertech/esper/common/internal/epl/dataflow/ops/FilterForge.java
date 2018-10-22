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
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorForge;
import com.espertech.esper.common.internal.epl.dataflow.util.GraphTypeDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import static com.espertech.esper.common.internal.epl.dataflow.core.EPDataFlowServiceImpl.OP_PACKAGE_NAME;

public class FilterForge implements DataFlowOperatorForge {

    @DataFlowOpParameter
    private ExprNode filter;

    private EventType eventType;
    private boolean singleOutputPort;

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        if (context.getInputPorts().size() != 1) {
            throw new ExprValidationException("Filter requires single input port");
        }

        if (filter == null) {
            throw new ExprValidationException("Required parameter 'filter' providing the filter expression is not provided");
        }

        if (context.getOutputPorts().isEmpty() || context.getOutputPorts().size() > 2) {
            throw new IllegalArgumentException("Filter operator requires one or two output stream(s) but produces " + context.getOutputPorts().size() + " streams");
        }

        eventType = context.getInputPorts().get(0).getTypeDesc().getEventType();
        singleOutputPort = context.getOutputPorts().size() == 1;

        filter = DataFlowParameterValidation.validate("filter", filter, eventType, Boolean.class, context);

        GraphTypeDesc[] typesPerPort = new GraphTypeDesc[context.getOutputPorts().size()];
        for (int i = 0; i < typesPerPort.length; i++) {
            typesPerPort[i] = new GraphTypeDesc(false, true, eventType);
        }
        return new DataFlowOpForgeInitializeResult(typesPerPort);
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(OP_PACKAGE_NAME + ".filter.FilterFactory", this.getClass(), "filter", parent, symbols, classScope)
                .exprnode("filter", filter)
                .eventtype("eventType", eventType)
                .constant("singleOutputPort", singleOutputPort)
                .build();
    }
}
