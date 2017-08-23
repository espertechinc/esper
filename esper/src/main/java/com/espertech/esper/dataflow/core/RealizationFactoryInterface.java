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
package com.espertech.esper.dataflow.core;

import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.client.dataflow.EPDataFlowExceptionHandler;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;
import com.espertech.esper.dataflow.util.*;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RealizationFactoryInterface {

    private static final Logger log = LoggerFactory.getLogger(RealizationFactoryInterface.class);

    public static DataflowStartDesc realize(String dataFlowName,
                                            Map<Integer, Object> operators,
                                            Map<Integer, OperatorMetadataDescriptor> operatorMetadata,
                                            Set<Integer> operatorBuildOrder,
                                            List<LogicalChannelBinding> bindings,
                                            DataFlowSignalManager dataFlowSignalManager,
                                            EPDataFlowInstantiationOptions options,
                                            EPServicesContext services,
                                            StatementContext statementContext) {


        // First pass: inject runtime context
        Map<Integer, EPDataFlowEmitter> runtimeContexts = new HashMap<Integer, EPDataFlowEmitter>();
        OperatorStatisticsProvider statisticsProvider = null;
        if (options.isOperatorStatistics()) {
            statisticsProvider = new OperatorStatisticsProvider(operatorMetadata);
        }

        boolean audit = AuditEnum.DATAFLOW_OP.getAudit(statementContext.getAnnotations()) != null;
        for (int producerOpNum : operatorBuildOrder) {
            String operatorPrettyPrint = operatorMetadata.get(producerOpNum).getOperatorPrettyPrint();
            if (log.isDebugEnabled()) {
                log.debug("Generating runtime context for " + operatorPrettyPrint);
            }

            // determine the number of output streams
            Object producingOp = operators.get(producerOpNum);
            int numOutputStreams = operatorMetadata.get(producerOpNum).getOperatorSpec().getOutput().getItems().size();
            List<ObjectBindingPair>[] targets = getOperatorConsumersPerStream(numOutputStreams, producerOpNum, operators, operatorMetadata, bindings);

            EPDataFlowEmitter runtimeContext = generateRuntimeContext(statementContext.getEngineURI(), statementContext.getStatementName(), audit, dataFlowName, producerOpNum, operatorPrettyPrint, dataFlowSignalManager, targets, options, statementContext.getEngineImportService());

            if (options.isOperatorStatistics()) {
                runtimeContext = new EPDataFlowEmitterWrapperWStatistics(runtimeContext, producerOpNum, statisticsProvider, options.isCpuStatistics());
            }

            JavaClassHelper.setFieldForAnnotation(producingOp, DataFlowContext.class, runtimeContext);
            runtimeContexts.put(producerOpNum, runtimeContext);
        }

        // Second pass: hook punctuation such that it gets forwarded
        for (int producerOpNum : operatorBuildOrder) {
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

        return new DataflowStartDesc(statisticsProvider);
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

    private static SignalHandler getSignalHandler(int producerNum, Object target, LogicalChannelBindingMethodDesc consumingSignalBindingDesc, EngineImportService engineImportService) {
        if (consumingSignalBindingDesc == null) {
            return SignalHandlerDefault.INSTANCE;
        } else {
            if (consumingSignalBindingDesc.getBindingType() instanceof LogicalChannelBindingTypePassAlong) {
                return new SignalHandlerDefaultWInvoke(target, consumingSignalBindingDesc.getMethod(), engineImportService);
            } else if (consumingSignalBindingDesc.getBindingType() instanceof LogicalChannelBindingTypePassAlongWStream) {
                LogicalChannelBindingTypePassAlongWStream streamInfo = (LogicalChannelBindingTypePassAlongWStream) consumingSignalBindingDesc.getBindingType();
                return new SignalHandlerDefaultWInvokeStream(target, consumingSignalBindingDesc.getMethod(), engineImportService, streamInfo.getStreamNum());
            } else {
                throw new IllegalStateException("Unrecognized signal binding: " + consumingSignalBindingDesc.getBindingType());
            }
        }
    }

    private static SubmitHandler getSubmitHandler(String engineURI, String statementName, boolean audit, String dataflowName, int producerOpNum, String operatorPrettyPrint, DataFlowSignalManager dataFlowSignalManager, ObjectBindingPair target, EPDataFlowExceptionHandler optionalExceptionHandler, EngineImportService engineImportService) {
        SignalHandler signalHandler = getSignalHandler(producerOpNum, target.getTarget(), target.getBinding().getConsumingSignalBindingDesc(), engineImportService);

        int receivingOpNum = target.getBinding().getLogicalChannel().getConsumingOpNum();
        String receivingOpPretty = target.getBinding().getLogicalChannel().getConsumingOpPrettyPrint();
        String receivingOpName = target.getBinding().getLogicalChannel().getConsumingOpName();
        EPDataFlowEmitterExceptionHandler exceptionHandler = new EPDataFlowEmitterExceptionHandler(engineURI, statementName, audit, dataflowName, receivingOpName, receivingOpNum, receivingOpPretty, optionalExceptionHandler);

        LogicalChannelBindingType bindingType = target.getBinding().getConsumingBindingDesc().getBindingType();
        if (bindingType instanceof LogicalChannelBindingTypePassAlong) {
            return new EPDataFlowEmitter1Stream1TargetPassAlong(producerOpNum, dataFlowSignalManager, signalHandler, exceptionHandler, target, engineImportService);
        } else if (bindingType instanceof LogicalChannelBindingTypePassAlongWStream) {
            LogicalChannelBindingTypePassAlongWStream type = (LogicalChannelBindingTypePassAlongWStream) bindingType;
            return new EPDataFlowEmitter1Stream1TargetPassAlongWStream(producerOpNum, dataFlowSignalManager, signalHandler, exceptionHandler, target, type.getStreamNum(), engineImportService);
        } else if (bindingType instanceof LogicalChannelBindingTypeUnwind) {
            return new EPDataFlowEmitter1Stream1TargetUnwind(producerOpNum, dataFlowSignalManager, signalHandler, exceptionHandler, target, engineImportService);
        } else {
            throw new UnsupportedOperationException("Unsupported binding type '" + bindingType + "'");
        }
    }

    private static EPDataFlowEmitter generateRuntimeContext(String engineURI,
                                                            String statementName,
                                                            boolean audit,
                                                            String dataflowName,
                                                            int producerOpNum,
                                                            String operatorPrettyPrint,
                                                            DataFlowSignalManager dataFlowSignalManager,
                                                            List<ObjectBindingPair>[] targetsPerStream,
                                                            EPDataFlowInstantiationOptions options,
                                                            EngineImportService engineImportService) {
        // handle no targets
        if (targetsPerStream == null) {
            return new EPDataFlowEmitterNoTarget(producerOpNum, dataFlowSignalManager);
        }

        // handle single-stream case
        if (targetsPerStream.length == 1) {
            List<ObjectBindingPair> targets = targetsPerStream[0];

            // handle single-stream single target case
            if (targets.size() == 1) {
                ObjectBindingPair target = targets.get(0);
                return getSubmitHandler(engineURI, statementName, audit, dataflowName, producerOpNum, operatorPrettyPrint, dataFlowSignalManager, target, options.getExceptionHandler(), engineImportService);
            }

            SubmitHandler[] handlers = new SubmitHandler[targets.size()];
            for (int i = 0; i < handlers.length; i++) {
                handlers[i] = getSubmitHandler(engineURI, statementName, audit, dataflowName, producerOpNum, operatorPrettyPrint, dataFlowSignalManager, targets.get(i), options.getExceptionHandler(), engineImportService);
            }
            return new EPDataFlowEmitter1StreamNTarget(producerOpNum, dataFlowSignalManager, handlers);
        } else {
            // handle multi-stream case
            SubmitHandler[][] handlersPerStream = new SubmitHandler[targetsPerStream.length][];
            for (int streamNum = 0; streamNum < targetsPerStream.length; streamNum++) {
                SubmitHandler[] handlers = new SubmitHandler[targetsPerStream[streamNum].size()];
                handlersPerStream[streamNum] = handlers;
                for (int i = 0; i < handlers.length; i++) {
                    handlers[i] = getSubmitHandler(engineURI, statementName, audit, dataflowName, producerOpNum, operatorPrettyPrint, dataFlowSignalManager, targetsPerStream[streamNum].get(i), options.getExceptionHandler(), engineImportService);
                }
            }
            return new EPDataFlowEmitterNStreamNTarget(producerOpNum, dataFlowSignalManager, handlersPerStream);
        }
    }
}
