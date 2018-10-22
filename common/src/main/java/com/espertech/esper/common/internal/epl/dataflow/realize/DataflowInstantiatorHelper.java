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

import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowExceptionHandler;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignal;
import com.espertech.esper.common.internal.context.aifactory.createdataflow.DataflowDesc;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;
import com.espertech.esper.common.internal.epl.dataflow.util.DataFlowSignalListener;
import com.espertech.esper.common.internal.epl.dataflow.util.DataFlowSignalManager;
import com.espertech.esper.common.internal.epl.dataflow.util.OperatorMetadataDescriptor;
import com.espertech.esper.common.internal.settings.ClasspathImportService;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DataflowInstantiatorHelper {

    private static final Logger log = LoggerFactory.getLogger(DataflowInstantiatorHelper.class);

    public static OperatorStatisticsProvider realize(DataflowDesc dataflow,
                                                     Map<Integer, Object> operators,
                                                     List<LogicalChannelBinding> bindings,
                                                     DataFlowSignalManager dataFlowSignalManager,
                                                     EPDataFlowInstantiationOptions options,
                                                     AgentInstanceContext agentInstanceContext) {

        Map<Integer, OperatorMetadataDescriptor> operatorMetadata = dataflow.getOperatorMetadata();

        // First pass: inject runtime context
        Map<Integer, EPDataFlowEmitter> runtimeContexts = new HashMap<Integer, EPDataFlowEmitter>();
        OperatorStatisticsProvider statisticsProvider = null;
        if (options.isOperatorStatistics()) {
            statisticsProvider = new OperatorStatisticsProvider(operatorMetadata);
        }

        for (int producerOpNum : dataflow.getOperatorBuildOrder()) {
            String operatorPrettyPrint = operatorMetadata.get(producerOpNum).getOperatorPrettyPrint();
            if (log.isDebugEnabled()) {
                log.debug("Generating runtime context for " + operatorPrettyPrint);
            }

            // determine the number of output streams
            Object producingOp = operators.get(producerOpNum);
            int numOutputStreams = operatorMetadata.get(producerOpNum).getNumOutputPorts();
            List<ObjectBindingPair>[] targets = getOperatorConsumersPerStream(numOutputStreams, producerOpNum, operators, operatorMetadata, bindings);

            EPDataFlowEmitter runtimeContext = generateRuntimeContext(agentInstanceContext, dataflow, options.getDataFlowInstanceId(), producerOpNum, operatorPrettyPrint, dataFlowSignalManager, targets, options);

            if (options.isOperatorStatistics()) {
                runtimeContext = new EPDataFlowEmitterWrapperWStatistics(runtimeContext, producerOpNum, statisticsProvider, options.isCpuStatistics());
            }

            JavaClassHelper.setFieldForAnnotation(producingOp, DataFlowContext.class, runtimeContext);
            runtimeContexts.put(producerOpNum, runtimeContext);
        }

        // Second pass: hook punctuation such that it gets forwarded
        for (int producerOpNum : dataflow.getOperatorBuildOrder()) {
            String operatorPrettyPrint = operatorMetadata.get(producerOpNum).getOperatorPrettyPrint();
            if (log.isDebugEnabled()) {
                log.debug("Handling signals for " + operatorPrettyPrint);
            }

            // determine consumers that receive punctuation
            Set<Integer> consumingOperatorsWithPunctuation = new HashSet<Integer>();
            for (LogicalChannelBinding binding : bindings) {
                if (!binding.getLogicalChannel().getOutputPort().isHasPunctuation() || binding.getLogicalChannel().getOutputPort().getProducingOpNum() != producerOpNum) {
                    continue;
                }
                consumingOperatorsWithPunctuation.add(binding.getLogicalChannel().getConsumingOpNum());
            }

            // hook up a listener for each
            for (int consumerPunc : consumingOperatorsWithPunctuation) {
                final EPDataFlowEmitter context = runtimeContexts.get(consumerPunc);
                if (context == null) {
                    continue;
                }
                dataFlowSignalManager.addSignalListener(producerOpNum, new DataFlowSignalListener() {
                    public void processSignal(EPDataFlowSignal signal) {
                        context.submitSignal(signal);
                    }
                });
            }
        }

        return statisticsProvider;
    }

    private static List<ObjectBindingPair>[] getOperatorConsumersPerStream(int numOutputStreams, int producingOperator, Map<Integer, Object> operators, Map<Integer, OperatorMetadataDescriptor> operatorMetadata, List<LogicalChannelBinding> bindings) {
        List<LogicalChannelBinding> channelsForProducer = LogicalChannelUtil.getBindingsConsuming(producingOperator, bindings);
        if (channelsForProducer.isEmpty()) {
            return null;
        }

        List<ObjectBindingPair>[] submitTargets = new List[numOutputStreams];
        for (int i = 0; i < numOutputStreams; i++) {
            submitTargets[i] = new ArrayList<ObjectBindingPair>();
        }

        for (LogicalChannelBinding binding : channelsForProducer) {
            int consumingOp = binding.getLogicalChannel().getConsumingOpNum();
            Object operator = operators.get(consumingOp);
            int producingStreamNum = binding.getLogicalChannel().getOutputPort().getStreamNumber();
            List<ObjectBindingPair> pairs = submitTargets[producingStreamNum];
            OperatorMetadataDescriptor metadata = operatorMetadata.get(consumingOp);
            pairs.add(new ObjectBindingPair(operator, metadata.getOperatorPrettyPrint(), binding));
        }
        return submitTargets;
    }

    private static SignalHandler getSignalHandler(int producerNum, Object target, LogicalChannelBindingMethodDesc consumingSignalBindingDesc, ClasspathImportService classpathImportService) {
        if (consumingSignalBindingDesc == null) {
            return SignalHandlerDefault.INSTANCE;
        } else {
            if (consumingSignalBindingDesc.getBindingType() instanceof LogicalChannelBindingTypePassAlong) {
                return new SignalHandlerDefaultWInvoke(target, consumingSignalBindingDesc.getMethod());
            } else if (consumingSignalBindingDesc.getBindingType() instanceof LogicalChannelBindingTypePassAlongWStream) {
                LogicalChannelBindingTypePassAlongWStream streamInfo = (LogicalChannelBindingTypePassAlongWStream) consumingSignalBindingDesc.getBindingType();
                return new SignalHandlerDefaultWInvokeStream(target, consumingSignalBindingDesc.getMethod(), streamInfo.getStreamNum());
            } else {
                throw new IllegalStateException("Unrecognized signal binding: " + consumingSignalBindingDesc.getBindingType());
            }
        }
    }

    private static SubmitHandler getSubmitHandler(AgentInstanceContext agentInstanceContext, String dataflowName, String instanceId, int producerOpNum, String operatorPrettyPrint, DataFlowSignalManager dataFlowSignalManager, ObjectBindingPair target, EPDataFlowExceptionHandler optionalExceptionHandler, ClasspathImportService classpathImportService) {
        SignalHandler signalHandler = getSignalHandler(producerOpNum, target.getTarget(), target.getBinding().getConsumingSignalBindingDesc(), classpathImportService);

        int receivingOpNum = target.getBinding().getLogicalChannel().getConsumingOpNum();
        String receivingOpPretty = target.getBinding().getLogicalChannel().getConsumingOpPrettyPrint();
        String receivingOpName = target.getBinding().getLogicalChannel().getConsumingOpName();
        EPDataFlowEmitterExceptionHandler exceptionHandler = new EPDataFlowEmitterExceptionHandler(agentInstanceContext, dataflowName, instanceId, receivingOpName, receivingOpNum, receivingOpPretty, optionalExceptionHandler);

        LogicalChannelBindingType bindingType = target.getBinding().getConsumingBindingDesc().getBindingType();
        if (bindingType instanceof LogicalChannelBindingTypePassAlong) {
            return new EPDataFlowEmitter1Stream1TargetPassAlong(producerOpNum, dataFlowSignalManager, signalHandler, exceptionHandler, target, classpathImportService);
        } else if (bindingType instanceof LogicalChannelBindingTypePassAlongWStream) {
            LogicalChannelBindingTypePassAlongWStream type = (LogicalChannelBindingTypePassAlongWStream) bindingType;
            return new EPDataFlowEmitter1Stream1TargetPassAlongWStream(producerOpNum, dataFlowSignalManager, signalHandler, exceptionHandler, target, type.getStreamNum(), classpathImportService);
        } else if (bindingType instanceof LogicalChannelBindingTypeUnwind) {
            return new EPDataFlowEmitter1Stream1TargetUnwind(producerOpNum, dataFlowSignalManager, signalHandler, exceptionHandler, target, classpathImportService);
        } else {
            throw new UnsupportedOperationException("Unsupported binding type '" + bindingType + "'");
        }
    }

    private static EPDataFlowEmitter generateRuntimeContext(AgentInstanceContext agentInstanceContext,
                                                            DataflowDesc dataflow,
                                                            String instanceId,
                                                            int producerOpNum,
                                                            String operatorPrettyPrint,
                                                            DataFlowSignalManager dataFlowSignalManager,
                                                            List<ObjectBindingPair>[] targetsPerStream,
                                                            EPDataFlowInstantiationOptions options) {
        // handle no targets
        if (targetsPerStream == null) {
            return new EPDataFlowEmitterNoTarget(producerOpNum, dataFlowSignalManager);
        }

        String dataflowName = dataflow.getDataflowName();
        ClasspathImportServiceRuntime classpathImportService = agentInstanceContext.getClasspathImportServiceRuntime();

        // handle single-stream case
        if (targetsPerStream.length == 1) {
            List<ObjectBindingPair> targets = targetsPerStream[0];

            // handle single-stream single target case
            if (targets.size() == 1) {
                ObjectBindingPair target = targets.get(0);
                return getSubmitHandler(agentInstanceContext, dataflow.getDataflowName(), instanceId, producerOpNum, operatorPrettyPrint, dataFlowSignalManager, target, options.getExceptionHandler(), classpathImportService);
            }

            SubmitHandler[] handlers = new SubmitHandler[targets.size()];
            for (int i = 0; i < handlers.length; i++) {
                handlers[i] = getSubmitHandler(agentInstanceContext, dataflowName, instanceId, producerOpNum, operatorPrettyPrint, dataFlowSignalManager, targets.get(i), options.getExceptionHandler(), classpathImportService);
            }
            return new EPDataFlowEmitter1StreamNTarget(producerOpNum, dataFlowSignalManager, handlers);
        } else {
            // handle multi-stream case
            SubmitHandler[][] handlersPerStream = new SubmitHandler[targetsPerStream.length][];
            for (int streamNum = 0; streamNum < targetsPerStream.length; streamNum++) {
                SubmitHandler[] handlers = new SubmitHandler[targetsPerStream[streamNum].size()];
                handlersPerStream[streamNum] = handlers;
                for (int i = 0; i < handlers.length; i++) {
                    handlers[i] = getSubmitHandler(agentInstanceContext, dataflowName, instanceId, producerOpNum, operatorPrettyPrint, dataFlowSignalManager, targetsPerStream[streamNum].get(i), options.getExceptionHandler(), classpathImportService);
                }
            }
            return new EPDataFlowEmitterNStreamNTarget(producerOpNum, dataFlowSignalManager, handlersPerStream);
        }
    }
}
