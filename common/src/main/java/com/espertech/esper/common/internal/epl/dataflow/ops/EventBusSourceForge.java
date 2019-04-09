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
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEventBeanCollector;
import com.espertech.esper.common.client.dataflow.util.DataFlowParameterValidation;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiledDesc;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiler;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeResult;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOutputPort;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;

import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.epl.dataflow.core.EPDataFlowServiceImpl.OP_PACKAGE_NAME;

public class EventBusSourceForge implements DataFlowOperatorForge {

    @DataFlowOpParameter
    protected ExprNode filter;

    @DataFlowOpParameter
    protected EPDataFlowEventBeanCollector collector;

    private FilterSpecCompiled filterSpecCompiled;
    private boolean submitEventBean;

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        if (context.getOutputPorts().size() != 1) {
            throw new IllegalArgumentException("EventBusSource operator requires one output stream but produces " + context.getOutputPorts().size() + " streams");
        }

        DataFlowOpOutputPort portZero = context.getOutputPorts().get(0);
        if (portZero.getOptionalDeclaredType() == null || portZero.getOptionalDeclaredType().getEventType() == null) {
            throw new IllegalArgumentException("EventBusSource operator requires an event type declated for the output stream");
        }

        EventType eventType = portZero.getOptionalDeclaredType().getEventType();
        if (!portZero.getOptionalDeclaredType().isUnderlying()) {
            submitEventBean = true;
        }

        DataFlowParameterValidation.validate("filter", filter, eventType, boolean.class, context);

        try {
            List<ExprNode> filters = Collections.emptyList();
            if (filter != null) {
                filters = Collections.singletonList(filter);
            }
            StreamTypeServiceImpl streamTypeService = new StreamTypeServiceImpl(eventType, eventType.getName(), true);
            FilterSpecCompiledDesc compiledDesc = FilterSpecCompiler.makeFilterSpec(eventType, eventType.getName(), filters, null,
                null, null, streamTypeService, null, context.getStatementRawInfo(), context.getServices());
            filterSpecCompiled = compiledDesc.getFilterSpecCompiled();
        } catch (ExprValidationException ex) {
            throw new ExprValidationException("Failed to obtain filter parameters: " + ex.getMessage(), ex);
        }

        return null;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        SAIFFInitializeBuilder builder = new SAIFFInitializeBuilder(OP_PACKAGE_NAME + ".eventbussource.EventBusSourceFactory", this.getClass(), "eventbussource", parent, symbols, classScope);
        builder.expression("filterSpecActivatable", localMethod(filterSpecCompiled.makeCodegen(builder.getMethod(), symbols, classScope)))
                .constant("submitEventBean", submitEventBean);
        return builder.build();
    }

    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpecCompiled;
    }
}
