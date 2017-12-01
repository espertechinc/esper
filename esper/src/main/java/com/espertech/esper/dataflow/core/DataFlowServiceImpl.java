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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatementState;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.client.dataflow.*;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPRuntimeEventSender;
import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.dataflow.annotations.*;
import com.espertech.esper.dataflow.interfaces.*;
import com.espertech.esper.dataflow.runnables.GraphSourceRunnable;
import com.espertech.esper.dataflow.util.*;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.DependencyGraph;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class DataFlowServiceImpl implements DataFlowService {
    private static final Logger log = LoggerFactory.getLogger(DataFlowServiceImpl.class);

    private static final String EVENT_WRAPPED_TYPE = "eventbean";

    private final Map<String, DataFlowServiceEntry> graphs = new HashMap<String, DataFlowServiceEntry>();
    private final Map<String, EPDataFlowInstance> instances = new HashMap<String, EPDataFlowInstance>();

    private final EPServiceProvider epService;
    private final DataFlowConfigurationStateService configurationState;

    public DataFlowServiceImpl(EPServiceProvider epService, DataFlowConfigurationStateService configurationState) {
        this.epService = epService;
        this.configurationState = configurationState;
    }

    public synchronized EPDataFlowDescriptor getDataFlow(String dataFlowName) {
        DataFlowServiceEntry entry = graphs.get(dataFlowName);
        if (entry == null) {
            return null;
        }
        return new EPDataFlowDescriptor(dataFlowName, entry.getState(), entry.getDataFlowDesc().getStatementContext().getStatementName());
    }

    public synchronized String[] getDataFlows() {
        Set<String> names = graphs.keySet();
        return names.toArray(new String[names.size()]);
    }

    public synchronized void addStartGraph(CreateDataFlowDesc desc, StatementContext statementContext, EPServicesContext servicesContext, AgentInstanceContext agentInstanceContext, boolean newStatement) throws ExprValidationException {
        compileTimeValidate(desc);

        DataFlowServiceEntry existing = graphs.get(desc.getGraphName());
        if (existing != null && (existing.getState() == EPStatementState.STARTED || newStatement)) {
            throw new ExprValidationException("Data flow by name '" + desc.getGraphName() + "' has already been declared");
        }
        if (existing != null) {
            existing.setState(EPStatementState.STARTED);
            return;
        }

        // compile annotations
        Map<GraphOperatorSpec, Annotation[]> operatorAnnotations = new HashMap<GraphOperatorSpec, Annotation[]>();
        for (GraphOperatorSpec spec : desc.getOperators()) {
            Annotation[] operatorAnnotation = AnnotationUtil.compileAnnotations(spec.getAnnotations(), servicesContext.getEngineImportService(), null);
            operatorAnnotations.put(spec, operatorAnnotation);
        }

        DataFlowStmtDesc stmtDesc = new DataFlowStmtDesc(desc, statementContext, servicesContext, agentInstanceContext, operatorAnnotations);
        graphs.put(desc.getGraphName(), new DataFlowServiceEntry(stmtDesc, EPStatementState.STARTED));
    }

    public synchronized void stopGraph(String graphName) {
        DataFlowServiceEntry existing = graphs.get(graphName);
        if (existing != null && existing.getState() == EPStatementState.STARTED) {
            existing.setState(EPStatementState.STOPPED);
        }
    }

    public synchronized void removeGraph(String graphName) {
        graphs.remove(graphName);
    }

    public EPDataFlowInstance instantiate(String dataFlowName) {
        return instantiate(dataFlowName, null);
    }

    public synchronized EPDataFlowInstance instantiate(String dataFlowName, EPDataFlowInstantiationOptions options) {
        final DataFlowServiceEntry serviceDesc = graphs.get(dataFlowName);
        if (serviceDesc == null) {
            throw new EPDataFlowInstantiationException("Data flow by name '" + dataFlowName + "' has not been defined");
        }
        if (serviceDesc.getState() != EPStatementState.STARTED) {
            throw new EPDataFlowInstantiationException("Data flow by name '" + dataFlowName + "' is currently in STOPPED statement state");
        }
        DataFlowStmtDesc stmtDesc = serviceDesc.getDataFlowDesc();
        try {
            return instantiateInternal(dataFlowName, options, stmtDesc.getGraphDesc(), stmtDesc.getStatementContext(), stmtDesc.getServicesContext(), stmtDesc.getAgentInstanceContext(), stmtDesc.getOperatorAnnotations());
        } catch (Exception ex) {
            String message = "Failed to instantiate data flow '" + dataFlowName + "': " + ex.getMessage();
            log.debug(message, ex);
            throw new EPDataFlowInstantiationException(message, ex);
        }
    }

    public synchronized void destroy() {
        graphs.clear();
    }

    public synchronized void saveConfiguration(String dataflowConfigName, String dataFlowName, EPDataFlowInstantiationOptions options) {
        DataFlowServiceEntry dataFlow = graphs.get(dataFlowName);
        if (dataFlow == null) {
            String message = "Failed to locate data flow '" + dataFlowName + "'";
            throw new EPDataFlowNotFoundException(message);
        }
        if (configurationState.exists(dataflowConfigName)) {
            String message = "Data flow saved configuration by name '" + dataflowConfigName + "' already exists";
            throw new EPDataFlowAlreadyExistsException(message);
        }
        configurationState.add(new EPDataFlowSavedConfiguration(dataflowConfigName, dataFlowName, options));
    }

    public synchronized String[] getSavedConfigurations() {
        return configurationState.getSavedConfigNames();
    }

    public synchronized EPDataFlowSavedConfiguration getSavedConfiguration(String configurationName) {
        return configurationState.getSavedConfig(configurationName);
    }

    public synchronized EPDataFlowInstance instantiateSavedConfiguration(String configurationName) throws EPDataFlowInstantiationException {
        EPDataFlowSavedConfiguration savedConfiguration = configurationState.getSavedConfig(configurationName);
        if (savedConfiguration == null) {
            throw new EPDataFlowInstantiationException("Dataflow saved configuration '" + configurationName + "' could not be found");
        }
        EPDataFlowInstantiationOptions options = savedConfiguration.getOptions();
        if (options == null) {
            options = new EPDataFlowInstantiationOptions();
            options.setDataFlowInstanceId(configurationName);
        }
        return instantiate(savedConfiguration.getDataflowName(), options);
    }

    public synchronized boolean removeSavedConfiguration(String configurationName) {
        return configurationState.removePrototype(configurationName) != null;
    }

    public synchronized void saveInstance(String instanceName, EPDataFlowInstance instance) throws EPDataFlowAlreadyExistsException {
        if (instances.containsKey(instanceName)) {
            throw new EPDataFlowAlreadyExistsException("Data flow instance name '" + instanceName + "' already saved");
        }
        instances.put(instanceName, instance);
    }

    public synchronized String[] getSavedInstances() {
        Set<String> instanceids = instances.keySet();
        return instanceids.toArray(new String[instanceids.size()]);
    }

    public synchronized EPDataFlowInstance getSavedInstance(String instanceName) {
        return instances.get(instanceName);
    }

    public synchronized boolean removeSavedInstance(String instanceName) {
        return instances.remove(instanceName) != null;
    }

    private EPDataFlowInstance instantiateInternal(String dataFlowName,
                                                   EPDataFlowInstantiationOptions options,
                                                   CreateDataFlowDesc desc,
                                                   StatementContext statementContext,
                                                   EPServicesContext servicesContext,
                                                   AgentInstanceContext agentInstanceContext,
                                                   Map<GraphOperatorSpec, Annotation[]> operatorAnnotations) throws ExprValidationException {
        if (options == null) {
            options = new EPDataFlowInstantiationOptions();
        }

        //
        // Building a model.
        //

        // resolve types
        Map<String, EventType> declaredTypes = resolveTypes(desc, statementContext, servicesContext);

        // resolve operator classes
        Map<Integer, OperatorMetadataDescriptor> operatorMetadata = resolveMetadata(desc, options, servicesContext.getEngineImportService(), operatorAnnotations);

        // build dependency graph:  operator -> [input_providing_op, input_providing_op]
        Map<Integer, OperatorDependencyEntry> operatorDependencies = analyzeDependencies(desc);

        // determine build order of operators
        Set<Integer> operatorBuildOrder = analyzeBuildOrder(operatorDependencies);

        // assure variables
        servicesContext.getVariableService().setLocalVersion();

        // instantiate operators
        Map<Integer, Object> operators = instantiateOperators(operatorMetadata, desc, options, statementContext);

        // Build graph that references port numbers (port number is simply the method offset number or to-be-generated slot in the list)
        EPRuntimeEventSender runtimeEventSender = (EPRuntimeEventSender) epService.getEPRuntime();
        List<LogicalChannel> operatorChannels = determineChannels(dataFlowName, operatorBuildOrder, operatorDependencies, operators, declaredTypes, operatorMetadata, options, servicesContext.getEventAdapterService(), servicesContext.getEngineImportService(), statementContext, servicesContext, agentInstanceContext, runtimeEventSender);
        if (log.isDebugEnabled()) {
            log.debug("For flow '" + dataFlowName + "' channels are: " + LogicalChannelUtil.printChannels(operatorChannels));
        }

        //
        // Build the realization.
        //

        // Determine binding of each channel to input methods (ports)
        List<LogicalChannelBinding> operatorChannelBindings = new ArrayList<LogicalChannelBinding>();
        for (LogicalChannel channel : operatorChannels) {
            Class targetClass = operators.get(channel.getConsumingOpNum()).getClass();
            LogicalChannelBindingMethodDesc consumingMethod = findMatchingMethod(channel.getConsumingOpPrettyPrint(), targetClass, channel, false);
            LogicalChannelBindingMethodDesc onSignalMethod = null;
            if (channel.getOutputPort().isHasPunctuation()) {
                onSignalMethod = findMatchingMethod(channel.getConsumingOpPrettyPrint(), targetClass, channel, true);
            }
            operatorChannelBindings.add(new LogicalChannelBinding(channel, consumingMethod, onSignalMethod));
        }

        // Obtain realization
        DataFlowSignalManager dataFlowSignalManager = new DataFlowSignalManager();
        DataflowStartDesc startDesc = RealizationFactoryInterface.realize(dataFlowName, operators, operatorMetadata, operatorBuildOrder, operatorChannelBindings, dataFlowSignalManager, options, servicesContext, statementContext);

        // For each GraphSource add runnable
        List<GraphSourceRunnable> sourceRunnables = new ArrayList<GraphSourceRunnable>();
        boolean audit = AuditEnum.DATAFLOW_SOURCE.getAudit(statementContext.getAnnotations()) != null;
        for (Map.Entry<Integer, Object> operatorEntry : operators.entrySet()) {
            if (!(operatorEntry.getValue() instanceof DataFlowSourceOperator)) {
                continue;
            }
            OperatorMetadataDescriptor meta = operatorMetadata.get(operatorEntry.getKey());

            DataFlowSourceOperator graphSource = (DataFlowSourceOperator) operatorEntry.getValue();
            GraphSourceRunnable runnable = new GraphSourceRunnable(statementContext.getEngineURI(), statementContext.getStatementName(), graphSource, dataFlowName, meta.getOperatorName(), operatorEntry.getKey(), meta.getOperatorPrettyPrint(), options.getExceptionHandler(), audit);
            sourceRunnables.add(runnable);

            dataFlowSignalManager.addSignalListener(operatorEntry.getKey(), runnable);
        }

        boolean auditStates = AuditEnum.DATAFLOW_TRANSITION.getAudit(statementContext.getAnnotations()) != null;
        return new EPDataFlowInstanceImpl(servicesContext.getEngineURI(), statementContext.getStatementName(), auditStates, dataFlowName, options.getDataFlowInstanceUserObject(), options.getDataFlowInstanceId(), EPDataFlowState.INSTANTIATED, sourceRunnables, operators, operatorBuildOrder, startDesc.getStatisticsProvider(), options.getParametersURIs(), statementContext.getEngineImportService());
    }

    private Map<String, EventType> resolveTypes(CreateDataFlowDesc desc, StatementContext statementContext, EPServicesContext servicesContext)
            throws ExprValidationException {
        Map<String, EventType> types = new HashMap<String, EventType>();
        for (CreateSchemaDesc spec : desc.getSchemas()) {
            EventType eventType = EventTypeUtility.createNonVariantType(true, spec, statementContext.getAnnotations(), statementContext.getConfigSnapshot(),
                    statementContext.getEventAdapterService(), servicesContext.getEngineImportService());
            types.put(spec.getSchemaName(), eventType);
        }
        return types;
    }

    private Map<Integer, Object> instantiateOperators(Map<Integer, OperatorMetadataDescriptor> operatorClasses, CreateDataFlowDesc desc, EPDataFlowInstantiationOptions options, StatementContext statementContext)
            throws ExprValidationException {

        Map<Integer, Object> operators = new HashMap<Integer, Object>();
        ExprValidationContext exprValidationContext = EPLValidationUtil.getExprValidationContextStatementOnly(statementContext);

        for (Map.Entry<Integer, OperatorMetadataDescriptor> operatorEntry : operatorClasses.entrySet()) {
            Object operator = instantiateOperator(desc.getGraphName(), operatorEntry.getKey(), operatorEntry.getValue(), desc.getOperators().get(operatorEntry.getKey()), options, exprValidationContext);
            operators.put(operatorEntry.getKey(), operator);
        }

        return operators;
    }

    private Object instantiateOperator(String dataFlowName, int operatorNum, OperatorMetadataDescriptor desc, GraphOperatorSpec graphOperator, EPDataFlowInstantiationOptions options, ExprValidationContext exprValidationContext)
            throws ExprValidationException {

        Object operatorObject = desc.getOptionalOperatorObject();

        if (operatorObject == null) {
            Class clazz = desc.getOperatorFactoryClass() != null ? desc.getOperatorFactoryClass() : desc.getOperatorClass();

            // use non-factory class if provided
            try {
                operatorObject = clazz.newInstance();
            } catch (Exception e) {
                throw new ExprValidationException("Failed to instantiate: " + e.getMessage());
            }
        }

        // inject properties
        Map<String, Object> configs = graphOperator.getDetail() == null ? Collections.<String, Object>emptyMap() : graphOperator.getDetail().getConfigs();
        injectObjectProperties(dataFlowName, graphOperator.getOperatorName(), operatorNum, configs, operatorObject, options.getParameterProvider(), options.getParametersURIs(), exprValidationContext);

        if (operatorObject instanceof DataFlowOperatorFactory) {
            try {
                operatorObject = ((DataFlowOperatorFactory) operatorObject).create();
            } catch (RuntimeException ex) {
                throw new ExprValidationException("Failed to obtain operator '" + desc.getOperatorName() + "', encountered an exception raised by factory class " + operatorObject.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
            }
        }

        return operatorObject;
    }

    private void injectObjectProperties(String dataFlowName, String operatorName, int operatorNum, Map<String, Object> configs, Object instance, EPDataFlowOperatorParameterProvider optionalParameterProvider, Map<String, Object> optionalParameterURIs, ExprValidationContext exprValidationContext)
            throws ExprValidationException {

        // determine if there is a property holder which holds all properties
        Set<Field> propertyHolderFields = JavaClassHelper.findAnnotatedFields(instance.getClass(), DataFlowOpPropertyHolder.class);
        if (propertyHolderFields.size() > 1) {
            throw new IllegalArgumentException("May apply " + DataFlowOpPropertyHolder.class.getSimpleName() + " annotation only to a single field");
        }

        // determine which class to write properties to
        Object propertyInstance;
        if (propertyHolderFields.isEmpty()) {
            propertyInstance = instance;
        } else {
            Class propertyHolderClass = propertyHolderFields.iterator().next().getType();
            try {
                propertyInstance = propertyHolderClass.newInstance();
            } catch (Exception e) {
                throw new ExprValidationException("Failed to instantiate '" + propertyHolderClass + "': " + e.getMessage(), e);
            }
        }

        // populate either the instance itself or the property-holder
        PopulateUtil.populateObject(operatorName, operatorNum, dataFlowName, configs, propertyInstance, ExprNodeOrigin.DATAFLOW, exprValidationContext, optionalParameterProvider, optionalParameterURIs);

        // set holder
        if (!propertyHolderFields.isEmpty()) {
            Field field = propertyHolderFields.iterator().next();
            try {
                field.setAccessible(true);
                field.set(instance, propertyInstance);
            } catch (Exception e) {
                throw new ExprValidationException("Failed to set field '" + field.getName() + "': " + e.getMessage(), e);
            }
        }
    }

    private List<LogicalChannel> determineChannels(String dataflowName,
                                                   Set<Integer> operatorBuildOrder,
                                                   Map<Integer, OperatorDependencyEntry> operatorDependencies,
                                                   Map<Integer, Object> operators,
                                                   Map<String, EventType> types,
                                                   Map<Integer, OperatorMetadataDescriptor> operatorMetadata,
                                                   EPDataFlowInstantiationOptions options,
                                                   EventAdapterService eventAdapterService,
                                                   EngineImportService engineImportService,
                                                   StatementContext statementContext,
                                                   EPServicesContext servicesContext,
                                                   AgentInstanceContext agentInstanceContext,
                                                   EPRuntimeEventSender runtimeEventSender)
            throws ExprValidationException {
        // This is a multi-step process.
        //
        // Step 1: find all the operators that have explicit output ports and determine the type of such
        Map<Integer, List<LogicalChannelProducingPortDeclared>> declaredOutputPorts = new HashMap<Integer, List<LogicalChannelProducingPortDeclared>>();
        for (int operatorNum : operatorBuildOrder) {

            OperatorMetadataDescriptor metadata = operatorMetadata.get(operatorNum);
            Object operator = operators.get(operatorNum);

            List<LogicalChannelProducingPortDeclared> annotationPorts = determineAnnotatedOutputPorts(operatorNum, operator, metadata, engineImportService, eventAdapterService);
            List<LogicalChannelProducingPortDeclared> graphDeclaredPorts = determineGraphDeclaredOutputPorts(operator, operatorNum, metadata, types, servicesContext);

            List<LogicalChannelProducingPortDeclared> allDeclaredPorts = new ArrayList<LogicalChannelProducingPortDeclared>();
            allDeclaredPorts.addAll(annotationPorts);
            allDeclaredPorts.addAll(graphDeclaredPorts);

            declaredOutputPorts.put(operatorNum, allDeclaredPorts);
        }

        // Step 2: determine for each operator the output ports: some are determined via "prepare" and some can be implicit
        // since they may not be declared or can be punctuation.
        // Therefore we need to meet ends: on one end the declared types, on the other the implied and dynamically-determined types based on input.
        // We do this in operator build order.
        Map<Integer, List<LogicalChannelProducingPortCompiled>> compiledOutputPorts = new HashMap<Integer, List<LogicalChannelProducingPortCompiled>>();
        for (int myOpNum : operatorBuildOrder) {

            GraphOperatorSpec operatorSpec = operatorMetadata.get(myOpNum).getOperatorSpec();
            Object operator = operators.get(myOpNum);
            OperatorMetadataDescriptor metadata = operatorMetadata.get(myOpNum);

            // Handle incoming first: if the operator has incoming ports, each of such should already have type information
            // Compile type information, call method, obtain output types.
            Set<Integer> incomingDependentOpNums = operatorDependencies.get(myOpNum).getIncoming();
            GraphTypeDesc[] typesPerOutput = determineOutputForInput(dataflowName, myOpNum, operator, metadata, operatorSpec, declaredOutputPorts, compiledOutputPorts, types, incomingDependentOpNums, options, statementContext, servicesContext, agentInstanceContext, runtimeEventSender);

            // Handle outgoing second:
            //   If there is outgoing declared, use that.
            //   If output types have been determined based on input, use that.
            //   else error
            List<LogicalChannelProducingPortCompiled> outgoingPorts = determineOutgoingPorts(myOpNum, operator, operatorSpec, metadata, compiledOutputPorts, declaredOutputPorts, typesPerOutput, incomingDependentOpNums);
            compiledOutputPorts.put(myOpNum, outgoingPorts);
        }

        // Step 3: normalization and connecting input ports with output ports (logically, no methods yet)
        List<LogicalChannel> channels = new ArrayList<LogicalChannel>();
        int channelId = 0;
        for (Integer myOpNum : operatorBuildOrder) {

            OperatorDependencyEntry dependencies = operatorDependencies.get(myOpNum);
            List<GraphOperatorInputNamesAlias> inputNames = operatorMetadata.get(myOpNum).getOperatorSpec().getInput().getStreamNamesAndAliases();
            OperatorMetadataDescriptor descriptor = operatorMetadata.get(myOpNum);

            // handle each (a,b,c AS d)
            int streamNum = -1;
            for (GraphOperatorInputNamesAlias inputName : inputNames) {
                streamNum++;

                // get producers
                List<LogicalChannelProducingPortCompiled> producingPorts = LogicalChannelUtil.getOutputPortByStreamName(dependencies.getIncoming(), inputName.getInputStreamNames(), compiledOutputPorts);
                if (producingPorts.size() < inputName.getInputStreamNames().length) {
                    throw new IllegalStateException("Failed to find producing ports");
                }

                // determine type compatibility
                if (producingPorts.size() > 1) {
                    LogicalChannelProducingPortCompiled first = producingPorts.get(0);
                    for (int i = 1; i < producingPorts.size(); i++) {
                        LogicalChannelProducingPortCompiled other = producingPorts.get(i);
                        compareTypeInfo(descriptor.getOperatorName(), first.getStreamName(), first.getGraphTypeDesc(), other.getStreamName(), other.getGraphTypeDesc());
                    }
                }

                String optionalAlias = inputName.getOptionalAsName();

                // handle each stream name
                for (String streamName : inputName.getInputStreamNames()) {

                    for (LogicalChannelProducingPortCompiled port : producingPorts) {
                        if (port.getStreamName().equals(streamName)) {
                            LogicalChannel channel = new LogicalChannel(channelId++, descriptor.getOperatorName(), myOpNum, streamNum, streamName, optionalAlias, descriptor.getOperatorPrettyPrint(), port);
                            channels.add(channel);
                        }
                    }
                }
            }
        }

        return channels;
    }

    private void compareTypeInfo(String operatorName, String firstName, GraphTypeDesc firstType, String otherName, GraphTypeDesc otherType)
            throws ExprValidationException {
        if (firstType.getEventType() != null && otherType.getEventType() != null && !firstType.getEventType().equals(otherType.getEventType())) {
            throw new ExprValidationException("For operator '" + operatorName + "' stream '" + firstName + "'" +
                    " typed '" + firstType.getEventType().getName() + "'" +
                    " is not the same type as stream '" + otherName + "'" +
                    " typed '" + otherType.getEventType().getName() + "'");
        }
        if (firstType.isWildcard() != otherType.isWildcard()) {
            throw new ExprValidationException("For operator '" + operatorName + "' streams '" + firstName + "'" +
                    " and '" + otherName + "' have differing wildcard type information");
        }
        if (firstType.isUnderlying() != otherType.isUnderlying()) {
            throw new ExprValidationException("For operator '" + operatorName + "' streams '" + firstName + "'" +
                    " and '" + otherName + "' have differing underlying information");
        }
    }

    private List<LogicalChannelProducingPortCompiled> determineOutgoingPorts(int myOpNum,
                                                                             Object operator,
                                                                             GraphOperatorSpec operatorSpec,
                                                                             OperatorMetadataDescriptor metadata,
                                                                             Map<Integer, List<LogicalChannelProducingPortCompiled>> compiledOutputPorts,
                                                                             Map<Integer, List<LogicalChannelProducingPortDeclared>> declaredOutputPorts,
                                                                             GraphTypeDesc[] typesPerOutput,
                                                                             Set<Integer> incomingDependentOpNums)
            throws ExprValidationException {
        // Either
        //  (A) the port is explicitly declared via @OutputTypes
        //  (B) the port is declared via "=> ABC<type>"
        //  (C) the port is implicit since there is only one input port and the operator is a functor

        int numPorts = operatorSpec.getOutput().getItems().size();
        List<LogicalChannelProducingPortCompiled> result = new ArrayList<LogicalChannelProducingPortCompiled>();

        // we go port-by-port: what was declared, what types were determined
        Map<String, GraphTypeDesc> types = new HashMap<String, GraphTypeDesc>();
        for (int port = 0; port < numPorts; port++) {
            String portStreamName = operatorSpec.getOutput().getItems().get(port).getStreamName();

            // find declaration, if any
            LogicalChannelProducingPortDeclared foundDeclared = null;
            List<LogicalChannelProducingPortDeclared> declaredList = declaredOutputPorts.get(myOpNum);
            for (LogicalChannelProducingPortDeclared declared : declaredList) {
                if (declared.getStreamNumber() == port) {
                    if (foundDeclared != null) {
                        throw new ExprValidationException("Found a declaration twice for port " + port);
                    }
                    foundDeclared = declared;
                }
            }

            if (foundDeclared == null && (typesPerOutput == null || typesPerOutput.length <= port || typesPerOutput[port] == null)) {
                throw new ExprValidationException("Operator neither declares an output type nor provided by the operator itself in a 'prepare' method");
            }
            if (foundDeclared != null && typesPerOutput != null && typesPerOutput.length > port && typesPerOutput[port] != null) {
                throw new ExprValidationException("Operator both declares an output type and provided a type in the 'prepare' method");
            }

            // punctuation determined by input
            boolean hasPunctuationSignal = (foundDeclared != null ? foundDeclared.isHasPunctuation() : false) || determineReceivesPunctuation(incomingDependentOpNums, operatorSpec.getInput(), compiledOutputPorts);

            GraphTypeDesc compiledType;
            if (foundDeclared != null) {
                compiledType = foundDeclared.getTypeDesc();
            } else {
                compiledType = typesPerOutput[port];
            }

            LogicalChannelProducingPortCompiled compiled = new LogicalChannelProducingPortCompiled(myOpNum, metadata.getOperatorPrettyPrint(), portStreamName, port, compiledType, hasPunctuationSignal);
            result.add(compiled);

            // check type compatibility
            GraphTypeDesc existingType = types.get(portStreamName);
            types.put(portStreamName, compiledType);
            if (existingType != null) {
                compareTypeInfo(operatorSpec.getOperatorName(), portStreamName, existingType, portStreamName, compiledType);
            }
        }

        return result;
    }

    private boolean determineReceivesPunctuation(Set<Integer> incomingDependentOpNums, GraphOperatorInput input, Map<Integer, List<LogicalChannelProducingPortCompiled>> compiledOutputPorts) {
        for (GraphOperatorInputNamesAlias inputItem : input.getStreamNamesAndAliases()) {
            List<LogicalChannelProducingPortCompiled> list = LogicalChannelUtil.getOutputPortByStreamName(incomingDependentOpNums, inputItem.getInputStreamNames(), compiledOutputPorts);
            for (LogicalChannelProducingPortCompiled port : list) {
                if (port.isHasPunctuation()) {
                    return true;
                }
            }
        }
        return false;
    }

    private GraphTypeDesc[] determineOutputForInput(String dataFlowName,
                                                    int myOpNum,
                                                    Object operator,
                                                    OperatorMetadataDescriptor meta,
                                                    GraphOperatorSpec operatorSpec,
                                                    Map<Integer, List<LogicalChannelProducingPortDeclared>> declaredOutputPorts,
                                                    Map<Integer, List<LogicalChannelProducingPortCompiled>> compiledOutputPorts,
                                                    Map<String, EventType> types,
                                                    Set<Integer> incomingDependentOpNums,
                                                    EPDataFlowInstantiationOptions options,
                                                    StatementContext statementContext,
                                                    EPServicesContext servicesContext,
                                                    AgentInstanceContext agentInstanceContext,
                                                    EPRuntimeEventSender runtimeEventSender)
            throws ExprValidationException {
        if (!(operator instanceof DataFlowOpLifecycle)) {
            return null;
        }

        // determine input ports to build up the input port metadata
        int numDeclared = operatorSpec.getInput().getStreamNamesAndAliases().size();
        Map<Integer, DataFlowOpInputPort> inputPorts = new LinkedHashMap<Integer, DataFlowOpInputPort>();
        for (int inputPortNum = 0; inputPortNum < numDeclared; inputPortNum++) {
            GraphOperatorInputNamesAlias inputItem = operatorSpec.getInput().getStreamNamesAndAliases().get(inputPortNum);
            List<LogicalChannelProducingPortCompiled> producingPorts = LogicalChannelUtil.getOutputPortByStreamName(incomingDependentOpNums, inputItem.getInputStreamNames(), compiledOutputPorts);

            DataFlowOpInputPort port;
            if (producingPorts.isEmpty()) { // this can be when the operator itself is the incoming port, i.e. feedback loop
                List<LogicalChannelProducingPortDeclared> declareds = declaredOutputPorts.get(myOpNum);
                if (declareds == null || declareds.isEmpty()) {
                    throw new ExprValidationException("Failed validation for operator '" + operatorSpec.getOperatorName() + "': No output ports declared");
                }
                LogicalChannelProducingPortDeclared foundDeclared = null;
                for (LogicalChannelProducingPortDeclared declared : declareds) {
                    if (Arrays.asList(inputItem.getInputStreamNames()).contains(declared.getStreamName())) {
                        foundDeclared = declared;
                        break;
                    }
                }
                if (foundDeclared == null) {
                    throw new ExprValidationException("Failed validation for operator '" + operatorSpec.getOperatorName() + "': Failed to find output port declared");
                }
                port = new DataFlowOpInputPort(foundDeclared.getTypeDesc(), new HashSet<String>(Arrays.asList(inputItem.getInputStreamNames())), inputItem.getOptionalAsName(), false);
            } else {
                port = new DataFlowOpInputPort(new GraphTypeDesc(false, false, producingPorts.get(0).getGraphTypeDesc().getEventType()), new HashSet<String>(Arrays.asList(inputItem.getInputStreamNames())), inputItem.getOptionalAsName(), producingPorts.get(0).isHasPunctuation());
            }
            inputPorts.put(inputPortNum, port);
        }

        // determine output ports to build up the output port metadata
        Map<Integer, DataFlowOpOutputPort> outputPorts = getDeclaredOutputPorts(operatorSpec, types, servicesContext);

        // determine event sender
        EPRuntimeEventSender dfRuntimeEventSender = runtimeEventSender;
        if (options.getSurrogateEventSender() != null) {
            dfRuntimeEventSender = options.getSurrogateEventSender();
        }

        DataFlowOpLifecycle preparable = (DataFlowOpLifecycle) operator;
        DataFlowOpInitializateContext context = new DataFlowOpInitializateContext(dataFlowName, options.getDataFlowInstanceId(), options.getDataFlowInstanceUserObject(), inputPorts, outputPorts, statementContext, servicesContext, agentInstanceContext, dfRuntimeEventSender, epService, meta.getOperatorAnnotations());

        DataFlowOpInitializeResult prepareResult;
        try {
            prepareResult = preparable.initialize(context);
        } catch (ExprValidationException e) {
            throw new ExprValidationException("Failed validation for operator '" + operatorSpec.getOperatorName() + "': " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ExprValidationException("Failed initialization for operator '" + operatorSpec.getOperatorName() + "': " + e.getMessage(), e);
        }

        if (prepareResult == null) {
            return null;
        }
        return prepareResult.getTypeDescriptors();
    }

    private List<LogicalChannelProducingPortDeclared> determineAnnotatedOutputPorts(int producingOpNum, Object operator, OperatorMetadataDescriptor descriptor, EngineImportService engineImportService, EventAdapterService eventAdapterService)
            throws ExprValidationException {

        List<LogicalChannelProducingPortDeclared> ports = new ArrayList<LogicalChannelProducingPortDeclared>();

        // See if any @OutputTypes annotations exists
        List<Annotation> annotations = JavaClassHelper.getAnnotations(OutputTypes.class, operator.getClass().getDeclaredAnnotations());

        for (Annotation annotation : annotations) {

            OutputTypes outputTypes = (OutputTypes) annotation;

            // create local event type for the declared type
            Map<String, Object> propertiesRaw = new LinkedHashMap<String, Object>();
            OutputType[] outputTypeArr = outputTypes.value();
            for (OutputType outputType : outputTypeArr) {
                Class clazz;
                if (outputType.type() != null && outputType.type() != OutputType.class) {
                    clazz = outputType.type();
                } else {
                    String typeName = outputType.typeName();
                    clazz = JavaClassHelper.getClassForSimpleName(typeName, engineImportService.getClassForNameProvider());
                    if (clazz == null) {
                        try {
                            clazz = engineImportService.resolveClass(typeName, false);
                        } catch (EngineImportException e) {
                            throw new RuntimeException("Failed to resolve type '" + typeName + "'");
                        }
                    }
                }
                propertiesRaw.put(outputType.name(), clazz);
            }
            Map<String, Object> propertiesCompiled = EventTypeUtility.compileMapTypeProperties(propertiesRaw, eventAdapterService);
            EventType eventType = eventAdapterService.createAnonymousObjectArrayType("TYPE_" + operator.getClass(), propertiesCompiled);

            // determine output stream name, which must be provided
            List<GraphOperatorOutputItem> declaredOutput = descriptor.getOperatorSpec().getOutput().getItems();
            if (declaredOutput.isEmpty()) {
                throw new ExprValidationException("No output stream declared");
            }
            if (declaredOutput.size() < outputTypes.portNumber()) {
                throw new ExprValidationException("No output stream declared for this port");
            }
            String streamName = declaredOutput.get(outputTypes.portNumber()).getStreamName();

            boolean isDeclaredPunctuated = JavaClassHelper.isAnnotationListed(DataFlowOpProvideSignal.class, operator.getClass().getAnnotations());
            LogicalChannelProducingPortDeclared port = new LogicalChannelProducingPortDeclared(producingOpNum, descriptor.getOperatorPrettyPrint(), streamName, outputTypes.portNumber(), new GraphTypeDesc(false, false, eventType), isDeclaredPunctuated);
            ports.add(port);
        }

        return ports;
    }

    private List<LogicalChannelProducingPortDeclared> determineGraphDeclaredOutputPorts(Object operator, int producingOpNum, OperatorMetadataDescriptor metadata, Map<String, EventType> types, EPServicesContext servicesContext)
            throws ExprValidationException {

        List<LogicalChannelProducingPortDeclared> ports = new ArrayList<LogicalChannelProducingPortDeclared>();

        int portNumber = 0;
        for (GraphOperatorOutputItem outputItem : metadata.getOperatorSpec().getOutput().getItems()) {
            if (outputItem.getTypeInfo().size() > 1) {
                throw new ExprValidationException("Multiple parameter types are not supported");
            }

            if (!outputItem.getTypeInfo().isEmpty()) {
                GraphTypeDesc typeDesc = determineTypeOutputPort(outputItem.getTypeInfo().get(0), types, servicesContext);
                boolean isDeclaredPunctuated = JavaClassHelper.isAnnotationListed(DataFlowOpProvideSignal.class, operator.getClass().getAnnotations());
                ports.add(new LogicalChannelProducingPortDeclared(producingOpNum, metadata.getOperatorPrettyPrint(), outputItem.getStreamName(), portNumber, typeDesc, isDeclaredPunctuated));
            }
            portNumber++;
        }

        return ports;
    }

    private Map<Integer, OperatorDependencyEntry> analyzeDependencies(CreateDataFlowDesc graphDesc)
            throws ExprValidationException {
        Map<Integer, OperatorDependencyEntry> logicalOpDependencies = new HashMap<Integer, OperatorDependencyEntry>();
        for (int i = 0; i < graphDesc.getOperators().size(); i++) {
            OperatorDependencyEntry entry = new OperatorDependencyEntry();
            logicalOpDependencies.put(i, entry);
        }
        for (int consumingOpNum = 0; consumingOpNum < graphDesc.getOperators().size(); consumingOpNum++) {
            OperatorDependencyEntry entry = logicalOpDependencies.get(consumingOpNum);
            GraphOperatorSpec op = graphDesc.getOperators().get(consumingOpNum);

            // for each input item
            for (GraphOperatorInputNamesAlias input : op.getInput().getStreamNamesAndAliases()) {

                // for each stream name listed
                for (String inputStreamName : input.getInputStreamNames()) {

                    // find all operators providing such input stream
                    boolean found = false;

                    // for each operator
                    for (int providerOpNum = 0; providerOpNum < graphDesc.getOperators().size(); providerOpNum++) {
                        GraphOperatorSpec from = graphDesc.getOperators().get(providerOpNum);

                        for (GraphOperatorOutputItem outputItem : from.getOutput().getItems()) {
                            if (outputItem.getStreamName().equals(inputStreamName)) {
                                found = true;
                                entry.addIncoming(providerOpNum);
                                logicalOpDependencies.get(providerOpNum).addOutgoing(consumingOpNum);
                            }
                        }
                    }

                    if (!found) {
                        throw new ExprValidationException("Input stream '" + inputStreamName + "' consumed by operator '" + op.getOperatorName() + "' could not be found");
                    }
                }
            }
        }
        return logicalOpDependencies;
    }

    private Map<Integer, OperatorMetadataDescriptor> resolveMetadata(CreateDataFlowDesc graphDesc, EPDataFlowInstantiationOptions options, EngineImportService engineImportService, Map<GraphOperatorSpec, Annotation[]> operatorAnnotations)
            throws ExprValidationException {
        Map<Integer, OperatorMetadataDescriptor> operatorClasses = new HashMap<Integer, OperatorMetadataDescriptor>();
        for (int i = 0; i < graphDesc.getOperators().size(); i++) {
            GraphOperatorSpec operatorSpec = graphDesc.getOperators().get(i);
            String operatorPrettyPrint = toPrettyPrint(i, operatorSpec);
            Annotation[] operatorAnnotation = operatorAnnotations.get(operatorSpec);

            // see if the operator is already provided by options
            if (options.getOperatorProvider() != null) {
                Object operator = options.getOperatorProvider().provide(new EPDataFlowOperatorProviderContext(graphDesc.getGraphName(), operatorSpec.getOperatorName(), operatorSpec));
                if (operator != null) {
                    OperatorMetadataDescriptor descriptor = new OperatorMetadataDescriptor(operatorSpec, i, operator.getClass(), null, operator, operatorPrettyPrint, operatorAnnotation);
                    operatorClasses.put(i, descriptor);
                    continue;
                }
            }

            // try to find factory class with factory annotation
            Class factoryClass = null;
            try {
                factoryClass = engineImportService.resolveClass(operatorSpec.getOperatorName() + "Factory", false);
            } catch (EngineImportException e) {
            }

            // if the factory implements the interface use that
            if (factoryClass != null && JavaClassHelper.isImplementsInterface(factoryClass, DataFlowOperatorFactory.class)) {
                OperatorMetadataDescriptor descriptor = new OperatorMetadataDescriptor(operatorSpec, i, null, factoryClass, null, operatorPrettyPrint, operatorAnnotation);
                operatorClasses.put(i, descriptor);
                continue;
            }

            // resolve by class name
            Class clazz;
            try {
                clazz = engineImportService.resolveClass(operatorSpec.getOperatorName(), false);
            } catch (EngineImportException e) {
                throw new ExprValidationException("Failed to resolve operator '" + operatorSpec.getOperatorName() + "': " + e.getMessage(), e);
            }

            if (!JavaClassHelper.isImplementsInterface(clazz, DataFlowSourceOperator.class) &&
                    !JavaClassHelper.isAnnotationListed(DataFlowOperator.class, clazz.getDeclaredAnnotations())) {
                throw new ExprValidationException("Failed to resolve operator '" + operatorSpec.getOperatorName() + "', operator class " + clazz.getName() + " does not declare the " + DataFlowOperator.class.getSimpleName() + " annotation or implement the " + DataFlowSourceOperator.class.getSimpleName() + " interface");
            }

            OperatorMetadataDescriptor descriptor = new OperatorMetadataDescriptor(operatorSpec, i, clazz, null, null, operatorPrettyPrint, operatorAnnotation);
            operatorClasses.put(i, descriptor);
        }
        return operatorClasses;
    }

    private String toPrettyPrint(int operatorNum, GraphOperatorSpec spec) {
        StringWriter writer = new StringWriter();
        writer.write(spec.getOperatorName());
        writer.write("#");
        writer.write(Integer.toString(operatorNum));

        writer.write("(");
        String delimiter = "";
        for (GraphOperatorInputNamesAlias inputItem : spec.getInput().getStreamNamesAndAliases()) {
            writer.write(delimiter);
            toPrettyPrintInput(inputItem, writer);
            if (inputItem.getOptionalAsName() != null) {
                writer.write(" as ");
                writer.write(inputItem.getOptionalAsName());
            }
            delimiter = ", ";
        }
        writer.write(")");

        if (spec.getOutput().getItems().isEmpty()) {
            return writer.toString();
        }
        writer.write(" -> ");

        delimiter = "";
        for (GraphOperatorOutputItem outputItem : spec.getOutput().getItems()) {
            writer.write(delimiter);
            writer.write(outputItem.getStreamName());
            writeTypes(outputItem.getTypeInfo(), writer);
            delimiter = ",";
        }

        return writer.toString();
    }

    private void toPrettyPrintInput(GraphOperatorInputNamesAlias inputItem, StringWriter writer) {
        if (inputItem.getInputStreamNames().length == 1) {
            writer.write(inputItem.getInputStreamNames()[0]);
        } else {
            writer.write("(");
            String delimiterNames = "";
            for (String name : inputItem.getInputStreamNames()) {
                writer.write(delimiterNames);
                writer.write(name);
                delimiterNames = ",";
            }
            writer.write(")");
        }
    }

    private void writeTypes(List<GraphOperatorOutputItemType> types, StringWriter writer) {
        if (types.isEmpty()) {
            return;
        }

        writer.write("<");
        String typeDelimiter = "";
        for (GraphOperatorOutputItemType type : types) {
            writer.write(typeDelimiter);
            writeType(type, writer);
            typeDelimiter = ",";
        }
        writer.write(">");
    }

    private void writeType(GraphOperatorOutputItemType type, StringWriter writer) {
        if (type.isWildcard()) {
            writer.append('?');
            return;
        }
        writer.append(type.getTypeOrClassname());
        writeTypes(type.getTypeParameters(), writer);
    }

    private Set<Integer> analyzeBuildOrder(Map<Integer, OperatorDependencyEntry> operators) throws ExprValidationException {

        DependencyGraph graph = new DependencyGraph(operators.size(), true);
        for (Map.Entry<Integer, OperatorDependencyEntry> entry : operators.entrySet()) {
            int myOpNum = entry.getKey();
            Set<Integer> incomings = entry.getValue().getIncoming();
            for (int incoming : incomings) {
                graph.addDependency(myOpNum, incoming);
            }
        }

        LinkedHashSet<Integer> topDownSet = new LinkedHashSet<Integer>();
        while (topDownSet.size() < operators.size()) {

            // secondary sort according to the order of listing
            Set<Integer> rootNodes = new TreeSet<Integer>(new Comparator<Integer>() {
                public int compare(Integer o1, Integer o2) {
                    return -1 * o1.compareTo(o2);
                }
            });
            rootNodes.addAll(graph.getRootNodes(topDownSet));

            if (rootNodes.isEmpty()) {   // circular dependency could cause this
                for (int i = 0; i < operators.size(); i++) {
                    if (!topDownSet.contains(i)) {
                        rootNodes.add(i);
                        break;
                    }
                }
            }

            topDownSet.addAll(rootNodes);
        }

        // invert the output
        LinkedHashSet<Integer> inverted = new LinkedHashSet<Integer>();
        Integer[] arr = topDownSet.toArray(new Integer[topDownSet.size()]);
        for (int i = arr.length - 1; i >= 0; i--) {
            inverted.add(arr[i]);
        }

        return inverted;
    }

    private LogicalChannelBindingMethodDesc findMatchingMethod(String operatorName, Class target, LogicalChannel channelDesc, boolean isPunctuation)
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

    private static Map<Integer, DataFlowOpOutputPort> getDeclaredOutputPorts(GraphOperatorSpec operatorSpec, Map<String, EventType> types, EPServicesContext servicesContext)
            throws ExprValidationException {
        Map<Integer, DataFlowOpOutputPort> outputPorts = new LinkedHashMap<Integer, DataFlowOpOutputPort>();
        for (int outputPortNum = 0; outputPortNum < operatorSpec.getOutput().getItems().size(); outputPortNum++) {
            GraphOperatorOutputItem outputItem = operatorSpec.getOutput().getItems().get(outputPortNum);

            GraphTypeDesc typeDesc = null;
            if (!outputItem.getTypeInfo().isEmpty()) {
                typeDesc = determineTypeOutputPort(outputItem.getTypeInfo().get(0), types, servicesContext);
            }
            outputPorts.put(outputPortNum, new DataFlowOpOutputPort(outputItem.getStreamName(), typeDesc));
        }

        return outputPorts;
    }

    private static GraphTypeDesc determineTypeOutputPort(GraphOperatorOutputItemType outType, Map<String, EventType> types, EPServicesContext servicesContext)
            throws ExprValidationException {

        EventType eventType = null;
        boolean isWildcard = false;
        boolean isUnderlying = true;

        String typeOrClassname = outType.getTypeOrClassname();
        if (typeOrClassname != null && typeOrClassname.toLowerCase(Locale.ENGLISH).equals(EVENT_WRAPPED_TYPE)) {
            isUnderlying = false;
            if (!outType.getTypeParameters().isEmpty() && !outType.getTypeParameters().get(0).isWildcard()) {
                String typeName = outType.getTypeParameters().get(0).getTypeOrClassname();
                eventType = resolveType(typeName, types, servicesContext);
            } else {
                isWildcard = true;
            }
        } else if (typeOrClassname != null) {
            eventType = resolveType(typeOrClassname, types, servicesContext);
        } else {
            isWildcard = true;
        }
        return new GraphTypeDesc(isWildcard, isUnderlying, eventType);
    }

    private static EventType resolveType(String typeOrClassname, Map<String, EventType> types, EPServicesContext servicesContext)
            throws ExprValidationException {
        EventType eventType = types.get(typeOrClassname);
        if (eventType == null) {
            eventType = servicesContext.getEventAdapterService().getExistsTypeByName(typeOrClassname);
        }
        if (eventType == null) {
            throw new ExprValidationException("Failed to find event type '" + typeOrClassname + "'");
        }
        return eventType;
    }

    private void compileTimeValidate(CreateDataFlowDesc desc) throws ExprValidationException {
        for (GraphOperatorSpec spec : desc.getOperators()) {
            for (GraphOperatorOutputItem out : spec.getOutput().getItems()) {
                if (out.getTypeInfo().size() > 1) {
                    throw new ExprValidationException("Failed to validate operator '" + spec.getOperatorName() + "': Multiple output types for a single stream '" + out.getStreamName() + "' are not supported");
                }
            }
        }

        Set<String> schemaNames = new HashSet<String>();
        for (CreateSchemaDesc schema : desc.getSchemas()) {
            if (schemaNames.contains(schema.getSchemaName())) {
                throw new ExprValidationException("Schema name '" + schema.getSchemaName() + "' is declared more then once");
            }
            schemaNames.add(schema.getSchemaName());
        }
    }
}
