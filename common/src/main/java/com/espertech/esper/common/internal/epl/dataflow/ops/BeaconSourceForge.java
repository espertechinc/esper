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
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeResult;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOutputPort;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorForge;
import com.espertech.esper.common.internal.epl.dataflow.util.GraphTypeDesc;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.etc.ExprEvalWithTypeWidener;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.util.TypeWidenerCustomizer;
import com.espertech.esper.common.internal.util.TypeWidenerException;
import com.espertech.esper.common.internal.util.TypeWidenerFactory;
import com.espertech.esper.common.internal.util.TypeWidenerSPI;

import java.util.*;

import static com.espertech.esper.common.internal.epl.dataflow.core.EPDataFlowServiceImpl.OP_PACKAGE_NAME;

public class BeaconSourceForge implements DataFlowOperatorForge {
    private final static List<String> PARAMETER_PROPERTIES = Arrays.asList("interval", "iterations", "initialDelay");

    @DataFlowOpParameter
    private ExprNode iterations;

    @DataFlowOpParameter
    private ExprNode initialDelay;

    @DataFlowOpParameter
    private ExprNode interval;

    private Map<String, ExprNode> allProperties = new LinkedHashMap<>();

    @DataFlowOpParameter(all = true)
    public void setProperty(String name, ExprNode value) {
        allProperties.put(name, value);
    }

    private boolean produceEventBean;
    private EventType outputEventType;
    private EventBeanManufacturerForge eventBeanManufacturer;
    private ExprForge[] evaluatorForges;

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        iterations = DataFlowParameterValidation.validate("iterations", iterations, Number.class, context);
        initialDelay = DataFlowParameterValidation.validate("initialDelay", initialDelay, Number.class, context);
        interval = DataFlowParameterValidation.validate("interval", interval, Number.class, context);

        if (context.getOutputPorts().size() != 1) {
            throw new IllegalArgumentException("BeaconSource operator requires one output stream but produces " + context.getOutputPorts().size() + " streams");
        }
        DataFlowOpOutputPort port = context.getOutputPorts().get(0);

        // Check if a type is declared
        if (port.getOptionalDeclaredType() == null || port.getOptionalDeclaredType().getEventType() == null) {
            return initializeTypeUndeclared(context);
        }

        return initializeTypeDeclared(port, context);
    }

    private DataFlowOpForgeInitializeResult initializeTypeDeclared(DataFlowOpOutputPort port, DataFlowOpForgeInitializeContext context)
            throws ExprValidationException {
        produceEventBean = port.getOptionalDeclaredType() != null && !port.getOptionalDeclaredType().isUnderlying();

        // compile properties to populate
        outputEventType = port.getOptionalDeclaredType().getEventType();
        Set<String> props = allProperties.keySet();
        props.removeAll(PARAMETER_PROPERTIES);
        WriteablePropertyDescriptor[] writables = setupProperties(props.toArray(new String[props.size()]), outputEventType);
        try {
            eventBeanManufacturer = EventTypeUtility.getManufacturer(outputEventType, writables, context.getServices().getClasspathImportServiceCompileTime(), false, context.getServices().getEventTypeAvroHandler());
        } catch (EventBeanManufactureException e) {
            throw new ExprValidationException("Cannot manufacture event for the provided type '" + outputEventType.getName() + "': " + e.getMessage(), e);
        }

        int index = 0;
        evaluatorForges = new ExprForge[writables.length];
        TypeWidenerCustomizer typeWidenerCustomizer = context.getServices().getEventTypeAvroHandler().getTypeWidenerCustomizer(outputEventType);
        for (WriteablePropertyDescriptor writable : writables) {

            Object providedProperty = allProperties.get(writable.getPropertyName());
            ExprNode exprNode = (ExprNode) providedProperty;
            ExprNode validated = EPLValidationUtil.validateSimpleGetSubtree(ExprNodeOrigin.DATAFLOWBEACON, exprNode, null, false, context.getBase().getStatementRawInfo(), context.getServices());
            TypeWidenerSPI widener;
            try {
                widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(validated), validated.getForge().getEvaluationType(),
                        writable.getType(), writable.getPropertyName(), false, typeWidenerCustomizer, context.getBase().getStatementName());
            } catch (TypeWidenerException e) {
                throw new ExprValidationException("Failed for property '" + writable.getPropertyName() + "'");

            }
            if (widener != null) {
                evaluatorForges[index] = new ExprEvalWithTypeWidener(widener, validated, writable.getType());
            } else {
                evaluatorForges[index] = validated.getForge();
            }
            index++;
        }

        return null;
    }

    private DataFlowOpForgeInitializeResult initializeTypeUndeclared(DataFlowOpForgeInitializeContext context)
            throws ExprValidationException {
        // No type has been declared, we can create one
        Map<String, Object> types = new LinkedHashMap<String, Object>();
        Set<String> props = allProperties.keySet();
        props.removeAll(PARAMETER_PROPERTIES);

        int count = 0;
        evaluatorForges = new ExprForge[props.size()];
        for (String propertyName : props) {
            ExprNode exprNode = allProperties.get(propertyName);
            ExprNode validated = EPLValidationUtil.validateSimpleGetSubtree(ExprNodeOrigin.DATAFLOWBEACON, exprNode, null, false, context.getStatementRawInfo(), context.getServices());
            types.put(propertyName, validated.getForge().getEvaluationType());
            evaluatorForges[count] = validated.getForge();
            count++;
        }

        String eventTypeName = context.getServices().getEventTypeNameGeneratorStatement().getDataflowOperatorTypeName(context.getOperatorNumber());
        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, context.getBase().getModuleName(), EventTypeTypeClass.DBDERIVED, EventTypeApplicationType.OBJECTARR, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        outputEventType = BaseNestableEventUtil.makeOATypeCompileTime(metadata, types, null, null, null, null, context.getServices().getBeanEventTypeFactoryPrivate(), context.getServices().getEventTypeCompileTimeResolver());
        context.getServices().getEventTypeCompileTimeRegistry().newType(outputEventType);

        return new DataFlowOpForgeInitializeResult(new GraphTypeDesc[]{new GraphTypeDesc(false, true, outputEventType)});
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(OP_PACKAGE_NAME + ".beaconsource.BeaconSourceFactory", this.getClass(), "factory", parent, symbols, classScope)
                .exprnode("iterations", iterations)
                .exprnode("initialDelay", initialDelay)
                .exprnode("interval", interval)
                .constant("produceEventBean", produceEventBean)
                .eventtype("outputEventType", outputEventType)
                .forges("propertyEvaluators", evaluatorForges)
                .manufacturer("manufacturer", eventBeanManufacturer)
                .build();
    }

    private static WriteablePropertyDescriptor[] setupProperties(String[] propertyNamesOffered, EventType outputEventType)
            throws ExprValidationException {
        Set<WriteablePropertyDescriptor> writeables = EventTypeUtility.getWriteableProperties(outputEventType, false, false);

        List<WriteablePropertyDescriptor> writablesList = new ArrayList<WriteablePropertyDescriptor>();

        for (int i = 0; i < propertyNamesOffered.length; i++) {
            String propertyName = propertyNamesOffered[i];
            WriteablePropertyDescriptor writable = EventTypeUtility.findWritable(propertyName, writeables);
            if (writable == null) {
                throw new ExprValidationException("Failed to find writable property '" + propertyName + "' for event type '" + outputEventType.getName() + "'");
            }
            writablesList.add(writable);
        }

        return writablesList.toArray(new WriteablePropertyDescriptor[writablesList.size()]);
    }
}
