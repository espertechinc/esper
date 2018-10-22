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
package com.espertech.esperio.file;

import com.espertech.esper.common.client.EPException;
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
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSinkForge implements DataFlowOperatorForge {

    private static final Logger log = LoggerFactory.getLogger(FileSinkForge.class);

    @DataFlowOpParameter
    private ExprNode file;

    @DataFlowOpParameter
    private ExprNode classpathFile;

    @DataFlowOpParameter
    private ExprNode append;

    private EventType eventType;

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        if (context.getInputPorts().size() != 1) {
            throw new EPException(this.getClass().getSimpleName() + " expected a single input port");
        }

        eventType = context.getInputPorts().get(0).getTypeDesc().getEventType();
        if (eventType == null) {
            throw new EPException("No event type defined for input port");
        }

        file = DataFlowParameterValidation.validate("file", file, String.class, context);
        classpathFile = DataFlowParameterValidation.validate("classpathFile", classpathFile, boolean.class, context);
        append = DataFlowParameterValidation.validate("append", append, boolean.class, context);
        return null;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(FileSinkFactory.class, this.getClass(), "factory", parent, symbols, classScope)
            .exprnode("file", file)
            .exprnode("classpathFile", classpathFile)
            .exprnode("append", append)
            .eventtype("eventType", eventType)
            .build();
    }
}
