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
package com.espertech.esper.common.internal.epl.dataflow.realize;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowOperatorProviderContext;
import com.espertech.esper.common.internal.context.aifactory.createdataflow.DataflowDesc;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.common.internal.context.util.StatementAgentInstanceLock;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.dataflow.core.EPDataFlowInstanceImpl;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowSourceOperator;
import com.espertech.esper.common.internal.epl.dataflow.runnables.GraphSourceRunnable;
import com.espertech.esper.common.internal.epl.dataflow.util.DataFlowSignalManager;
import com.espertech.esper.common.internal.epl.dataflow.util.GraphTypeDesc;
import com.espertech.esper.common.internal.epl.dataflow.util.OperatorMetadataDescriptor;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Method;
import java.util.*;

public class DataflowInstantiator {
    public static EPDataFlowInstance instantiate(int agentInstanceId,
                                                 DataflowDesc dataflow,
                                                 EPDataFlowInstantiationOptions options)
            throws ExprValidationException {
        StatementContext statementContext = dataflow.getStatementContext();

        // allocate agent instance context
        StatementAgentInstanceLock lock = statementContext.getStatementAgentInstanceLockFactory().getStatementLock(statementContext.getStatementName(), statementContext.getAnnotations(), statementContext.isStatelessSelect(), statementContext.getStatementType());
        EPStatementAgentInstanceHandle handle = new EPStatementAgentInstanceHandle(statementContext.getEpStatementHandle(), agentInstanceId, lock);
        AuditProvider auditProvider = statementContext.getStatementInformationals().getAuditProvider();
        InstrumentationCommon instrumentationProvider = statementContext.getStatementInformationals().getInstrumentationProvider();
        AgentInstanceContext agentInstanceContext = new AgentInstanceContext(statementContext, handle, null, null, auditProvider, instrumentationProvider);

        // assure variables
        statementContext.getVariableManagementService().setLocalVersion();

        // instantiate operators
        Map<Integer, Object> operators = instantiateOperators(agentInstanceContext, options, dataflow);

        // determine binding of each channel to input methods (ports)
        List<LogicalChannelBinding> operatorChannelBindings = new ArrayList<LogicalChannelBinding>();
        for (LogicalChannel channel : dataflow.getLogicalChannels()) {
            Class targetClass = operators.get(channel.getConsumingOpNum()).getClass();
            LogicalChannelBindingMethodDesc consumingMethod = findMatchingMethod(channel.getConsumingOpPrettyPrint(), targetClass, channel, false);
            LogicalChannelBindingMethodDesc onSignalMethod = null;
            if (channel.getOutputPort().isHasPunctuation()) {
                onSignalMethod = findMatchingMethod(channel.getConsumingOpPrettyPrint(), targetClass, channel, true);
            }
            operatorChannelBindings.add(new LogicalChannelBinding(channel, consumingMethod, onSignalMethod));
        }

        // obtain realization
        DataFlowSignalManager dataFlowSignalManager = new DataFlowSignalManager();
        OperatorStatisticsProvider statistics = DataflowInstantiatorHelper.realize(dataflow, operators, operatorChannelBindings, dataFlowSignalManager, options, agentInstanceContext);

        // For each GraphSource add runnable
        List<GraphSourceRunnable> sourceRunnables = new ArrayList<GraphSourceRunnable>();
        boolean audit = AuditEnum.DATAFLOW_SOURCE.getAudit(statementContext.getAnnotations()) != null;
        for (Map.Entry<Integer, Object> operatorEntry : operators.entrySet()) {
            if (!(operatorEntry.getValue() instanceof DataFlowSourceOperator)) {
                continue;
            }
            OperatorMetadataDescriptor meta = dataflow.getOperatorMetadata().get(operatorEntry.getKey());

            DataFlowSourceOperator graphSource = (DataFlowSourceOperator) operatorEntry.getValue();
            GraphSourceRunnable runnable = new GraphSourceRunnable(agentInstanceContext, graphSource, dataflow.getDataflowName(), options.getDataFlowInstanceId(), meta.getOperatorName(), operatorEntry.getKey(), meta.getOperatorPrettyPrint(), options.getExceptionHandler(), audit);
            sourceRunnables.add(runnable);

            dataFlowSignalManager.addSignalListener(operatorEntry.getKey(), runnable);
        }

        return new EPDataFlowInstanceImpl(options.getDataFlowInstanceUserObject(), options.getDataFlowInstanceId(), statistics, operators,
                sourceRunnables, dataflow, agentInstanceContext, statistics, options.getParametersURIs());
    }

    private static Map<Integer, Object> instantiateOperators(AgentInstanceContext agentInstanceContext, EPDataFlowInstantiationOptions options, DataflowDesc dataflow) {
        Map<Integer, Object> operators = new HashMap<Integer, Object>();

        for (Integer operatorNum : dataflow.getOperatorMetadata().keySet()) {
            Object operator = instantiateOperator(operatorNum, dataflow, options, agentInstanceContext);
            operators.put(operatorNum, operator);
        }

        return operators;
    }

    private static Object instantiateOperator(int operatorNum, DataflowDesc dataflow, EPDataFlowInstantiationOptions options, AgentInstanceContext agentInstanceContext) {
        DataFlowOperatorFactory operatorFactory = dataflow.getOperatorFactories().get(operatorNum);
        OperatorMetadataDescriptor metadata = dataflow.getOperatorMetadata().get(operatorNum);

        // see if the operator is already provided by options
        if (options.getOperatorProvider() != null) {
            Object operator = options.getOperatorProvider().provide(new EPDataFlowOperatorProviderContext(dataflow.getDataflowName(), metadata.getOperatorName(), operatorFactory));
            if (operator != null) {
                return operator;
            }
        }

        Map<String, Object> additionalParameters = null;
        if (options.getParametersURIs() != null) {
            String prefix = metadata.getOperatorName() + "/";
            for (Map.Entry<String, Object> entry : options.getParametersURIs().entrySet()) {
                if (!entry.getKey().startsWith(prefix)) {
                    continue;
                }
                if (additionalParameters == null) {
                    additionalParameters = new HashMap<>();
                }
                additionalParameters.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }

        Object operator;
        try {
            operator = operatorFactory.operator(new DataFlowOpInitializeContext(dataflow.getDataflowName(),
                    metadata.getOperatorName(), operatorNum, agentInstanceContext, additionalParameters, options.getDataFlowInstanceId(), options.getParameterProvider(), operatorFactory, options.getDataFlowInstanceUserObject()));
        } catch (Throwable t) {
            OperatorMetadataDescriptor meta = dataflow.getOperatorMetadata().get(operatorNum);
            String message = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            throw new EPException("Failed to obtain operator instance for '" + meta.getOperatorName() + "': " + message, t);
        }
        return operator;
    }

    private static LogicalChannelBindingMethodDesc findMatchingMethod(String operatorName, Class target, LogicalChannel channelDesc, boolean isPunctuation)
            throws ExprValidationException {
        if (isPunctuation) {
            for (Method method : target.getMethods()) {
                if (method.getName().equals("onSignal")) {
                    return new LogicalChannelBindingMethodDesc(method, LogicalChannelBindingTypePassAlong.INSTANCE);
                }
            }
            return null;
        }

        LogicalChannelProducingPortCompiled outputPort = channelDesc.getOutputPort();

        Class[] expectedIndividual;
        Class expectedUnderlying;
        EventType expectedUnderlyingType;
        GraphTypeDesc typeDesc = outputPort.getGraphTypeDesc();

        if (typeDesc.isWildcard()) {
            expectedIndividual = new Class[0];
            expectedUnderlying = null;
            expectedUnderlyingType = null;
        } else {
            expectedIndividual = new Class[typeDesc.getEventType().getPropertyNames().length];
            int i = 0;
            for (EventPropertyDescriptor descriptor : typeDesc.getEventType().getPropertyDescriptors()) {
                expectedIndividual[i] = descriptor.getPropertyType();
                i++;
            }
            expectedUnderlying = typeDesc.getEventType().getUnderlyingType();
            expectedUnderlyingType = typeDesc.getEventType();
        }

        String channelSpecificMethodName = null;
        if (channelDesc.getConsumingOptStreamAliasName() != null) {
            channelSpecificMethodName = "on" + channelDesc.getConsumingOptStreamAliasName();
        }

        for (Method method : target.getMethods()) {

            boolean eligible = method.getName().equals("onInput");
            if (!eligible && method.getName().equals(channelSpecificMethodName)) {
                eligible = true;
            }

            if (!eligible) {
                continue;
            }

            // handle Object[]
            int numParams = method.getParameterTypes().length;
            Class[] paramTypes = method.getParameterTypes();

            if (expectedUnderlying != null) {
                if (numParams == 1 && JavaClassHelper.isSubclassOrImplementsInterface(paramTypes[0], expectedUnderlying)) {
                    return new LogicalChannelBindingMethodDesc(method, LogicalChannelBindingTypePassAlong.INSTANCE);
                }
                if (numParams == 2 && JavaClassHelper.getBoxedType(paramTypes[0]) == Integer.class && JavaClassHelper.isSubclassOrImplementsInterface(paramTypes[1], expectedUnderlying)) {
                    return new LogicalChannelBindingMethodDesc(method, new LogicalChannelBindingTypePassAlongWStream(channelDesc.getConsumingOpStreamNum()));
                }
            }

            if (numParams == 1 && (paramTypes[0] == Object.class || (paramTypes[0] == Object[].class && method.isVarArgs()))) {
                return new LogicalChannelBindingMethodDesc(method, LogicalChannelBindingTypePassAlong.INSTANCE);
            }
            if (numParams == 2 && paramTypes[0] == int.class && (paramTypes[1] == Object.class || (paramTypes[1] == Object[].class && method.isVarArgs()))) {
                return new LogicalChannelBindingMethodDesc(method, new LogicalChannelBindingTypePassAlongWStream(channelDesc.getConsumingOpStreamNum()));
            }

            // if exposing a method that exactly matches each property type in order, use that, i.e. "onInut(String p0, int p1)"
            if (expectedUnderlyingType instanceof ObjectArrayEventType && JavaClassHelper.isSignatureCompatible(expectedIndividual, method.getParameterTypes())) {
                return new LogicalChannelBindingMethodDesc(method, LogicalChannelBindingTypeUnwind.INSTANCE);
            }
        }

        Set<String> choices = new LinkedHashSet<String>();
        choices.add(Object.class.getSimpleName());
        choices.add("Object[]");
        if (expectedUnderlying != null) {
            choices.add(expectedUnderlying.getSimpleName());
        }
        throw new ExprValidationException("Failed to find onInput method on for operator '" + operatorName + "' class " +
                target.getName() + ", expected an onInput method that takes any of {" + CollectionUtil.toString(choices) + "}");
    }

}
