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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowOpProvideSignal;
import com.espertech.esper.common.client.dataflow.util.DataFlowParameterValidation;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeResult;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOutputPort;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.Map;

@DataFlowOpProvideSignal
public class FileSourceForge implements DataFlowOperatorForge {

    @DataFlowOpParameter
    private ExprNode file;

    @DataFlowOpParameter
    private ExprNode classpathFile;

    @DataFlowOpParameter
    private ExprNode hasHeaderLine;

    @DataFlowOpParameter
    private ExprNode hasTitleLine;

    @DataFlowOpParameter
    private Map<String, Object> adapterInputSource;

    @DataFlowOpParameter
    private ExprNode numLoops;

    @DataFlowOpParameter
    private String[] propertyNames;

    @DataFlowOpParameter
    private ExprNode format;

    @DataFlowOpParameter
    private ExprNode propertyNameLine;

    @DataFlowOpParameter
    private ExprNode propertyNameFile;

    @DataFlowOpParameter
    private ExprNode dateFormat;

    private EventType outputEventType;

    private EventType[] outputPortTypes;

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        outputEventType = context.getOutputPorts().get(0).getOptionalDeclaredType() != null ? context.getOutputPorts().get(0).getOptionalDeclaredType().getEventType() : null;
        if (outputEventType == null) {
            throw new ExprValidationException("No event type provided for output, please provide an event type name");
        }

        outputPortTypes = new EventType[context.getOutputPorts().size()];
        for (Map.Entry<Integer, DataFlowOpOutputPort> entry : context.getOutputPorts().entrySet()) {
            outputPortTypes[entry.getKey()] = entry.getValue().getOptionalDeclaredType().getEventType();
        }

        file = DataFlowParameterValidation.validate("file", file, String.class, context);
        classpathFile = DataFlowParameterValidation.validate("classpathFile", classpathFile, boolean.class, context);
        hasHeaderLine = DataFlowParameterValidation.validate("hasHeaderLine", hasHeaderLine, boolean.class, context);
        hasTitleLine = DataFlowParameterValidation.validate("hasTitleLine", hasTitleLine, boolean.class, context);
        numLoops = DataFlowParameterValidation.validate("numLoops", numLoops, Integer.class, context);
        format = DataFlowParameterValidation.validate("format", format, String.class, context);
        propertyNameLine = DataFlowParameterValidation.validate("propertyNameLine", propertyNameLine, String.class, context);
        propertyNameFile = DataFlowParameterValidation.validate("propertyNameFile", propertyNameFile, String.class, context);
        dateFormat = DataFlowParameterValidation.validate("dateFormat", dateFormat, String.class, context);
        return null;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(FileSourceFactory.class, this.getClass(), "factory", parent, symbols, classScope)
            .exprnode("file", file)
            .exprnode("classpathFile", classpathFile)
            .exprnode("hasHeaderLine", hasHeaderLine)
            .exprnode("hasTitleLine", hasTitleLine)
            .exprnode("numLoops", numLoops)
            .constant("propertyNames", propertyNames)
            .exprnode("format", format)
            .exprnode("propertyNameLine", propertyNameLine)
            .exprnode("propertyNameFile", propertyNameFile)
            .exprnode("dateFormat", dateFormat)
            .map("adapterInputSource", adapterInputSource)
            .eventtype("outputEventType", outputEventType)
            .eventtypes("outputPortTypes", outputPortTypes)
            .build();
    }
}
