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
package com.espertech.esper.common.internal.context.aifactory.createdataflow;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowOpPropertyHolder;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowOpProvideSignal;
import com.espertech.esper.common.client.dataflow.annotations.OutputType;
import com.espertech.esper.common.client.dataflow.annotations.OutputTypes;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowOperatorParameterProvider;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileException;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.epl.dataflow.ops.EventBusSourceForge;
import com.espertech.esper.common.internal.epl.dataflow.realize.LogicalChannel;
import com.espertech.esper.common.internal.epl.dataflow.realize.LogicalChannelProducingPortCompiled;
import com.espertech.esper.common.internal.epl.dataflow.realize.LogicalChannelProducingPortDeclared;
import com.espertech.esper.common.internal.epl.dataflow.realize.LogicalChannelUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.GraphTypeDesc;
import com.espertech.esper.common.internal.epl.dataflow.util.OperatorDependencyEntry;
import com.espertech.esper.common.internal.epl.dataflow.util.OperatorMetadataDescriptor;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.event.core.BaseNestableEventUtil;
import com.espertech.esper.common.internal.event.core.EventTypeForgablesPair;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamExprNodeForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.common.internal.util.DependencyGraph;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

public class StmtForgeMethodCreateDataflow implements StmtForgeMethod {
    private static final String EVENT_WRAPPED_TYPE = "eventbean";
    private static final Logger log = LoggerFactory.getLogger(StmtForgeMethodCreateDataflow.class);

    private final StatementBaseInfo base;

    public StmtForgeMethodCreateDataflow(StatementBaseInfo base) {
        this.base = base;
    }

    public StmtForgeMethodResult make(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {
        StatementSpecCompiled statementSpec = base.getStatementSpec();

        CreateDataFlowDesc createDataFlowDesc = statementSpec.getRaw().getCreateDataFlowDesc();
        services.getDataFlowCompileTimeRegistry().newDataFlow(createDataFlowDesc.getGraphName());

        String eventTypeName = services.getEventTypeNameGeneratorStatement().getAnonymousTypeName();
        EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, base.getModuleName(), EventTypeTypeClass.STATEMENTOUT, EventTypeApplicationType.MAP, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        EventType eventType = BaseNestableEventUtil.makeMapTypeCompileTime(metadata, Collections.emptyMap(), null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
        services.getEventTypeCompileTimeRegistry().newType(eventType);

        String statementFieldsClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementFields.class, classPostfix);
        DataFlowOpForgeCodegenEnv codegenEnv = new DataFlowOpForgeCodegenEnv(packageName, classPostfix);

        DataflowDescForge dataflowForge = buildForge(createDataFlowDesc, codegenEnv, packageName, base, services);

        CodegenPackageScope packageScope = new CodegenPackageScope(packageName, statementFieldsClassName, services.isInstrumented());
        String aiFactoryProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementAIFactoryProvider.class, classPostfix);
        StatementAgentInstanceFactoryCreateDataflowForge forge = new StatementAgentInstanceFactoryCreateDataflowForge(eventType, dataflowForge);
        StmtClassForgeableAIFactoryProviderCreateDataflow aiFactoryForgeable = new StmtClassForgeableAIFactoryProviderCreateDataflow(aiFactoryProviderClassName, packageScope, forge);

        SelectSubscriberDescriptor selectSubscriberDescriptor = new SelectSubscriberDescriptor();
        StatementInformationalsCompileTime informationals = StatementInformationalsUtil.getInformationals(base, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, selectSubscriberDescriptor, packageScope, services);
        informationals.getProperties().put(StatementProperty.CREATEOBJECTNAME, createDataFlowDesc.getGraphName());
        String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameSimple(StatementProvider.class, classPostfix);
        StmtClassForgeableStmtProvider stmtProvider = new StmtClassForgeableStmtProvider(aiFactoryProviderClassName, statementProviderClassName, informationals, packageScope);

        List<StmtClassForgeable> forgeables = new ArrayList<>();
        for (StmtClassForgeableFactory additional : dataflowForge.getAdditionalForgables()) {
            forgeables.add(additional.make(packageScope, classPostfix));
        }
        forgeables.add(aiFactoryForgeable);
        forgeables.add(stmtProvider);
        forgeables.add(new StmtClassForgeableStmtFields(statementFieldsClassName, packageScope, 0));

        // compiled filter spec list
        List<FilterSpecCompiled> filterSpecCompileds = new ArrayList<>();
        for (Map.Entry<Integer, DataFlowOperatorForge> entry : dataflowForge.getOperatorFactories().entrySet()) {
            if (entry.getValue() instanceof EventBusSourceForge) {
                EventBusSourceForge eventBusSource = (EventBusSourceForge) entry.getValue();
                filterSpecCompileds.add(eventBusSource.getFilterSpecCompiled());
            }
        }
        List<FilterSpecParamExprNodeForge> filterBooleanExpr = FilterSpecCompiled.makeExprNodeList(filterSpecCompileds, Collections.emptyList());
        List<NamedWindowConsumerStreamSpec> namedWindowConsumers = new ArrayList<>();
        List<ScheduleHandleCallbackProvider> scheduleds = new ArrayList<>();

        // add additional forgeables
        for (StmtForgeMethodResult additional : dataflowForge.getForgables()) {
            forgeables.addAll(0, additional.getForgeables());
            scheduleds.addAll(additional.getScheduleds());
        }

        return new StmtForgeMethodResult(forgeables, filterSpecCompileds, scheduleds, namedWindowConsumers, filterBooleanExpr);
    }

    private static DataflowDescForge buildForge(CreateDataFlowDesc desc, DataFlowOpForgeCodegenEnv codegenEnv, String packageName, StatementBaseInfo base, StatementCompileTimeServices services) throws ExprValidationException {
        // basic validation
        validate(desc);

        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        // compile operator annotations
        Map<Integer, Annotation[]> operatorAnnotations = new HashMap<>();
        int count = 0;
        for (GraphOperatorSpec spec : desc.getOperators()) {
            Annotation[] operatorAnnotation;
            try {
                operatorAnnotation = AnnotationUtil.compileAnnotations(spec.getAnnotations(), services.getClasspathImportServiceCompileTime(), null);
            } catch (StatementSpecCompileException e) {
                throw new ExprValidationException("Invalid annotation: " + e.getMessage(), e);
            }
            operatorAnnotations.put(count, operatorAnnotation);
            count++;
        }

        // resolve types
        ResolveTypesResult resolveTypesResult = resolveTypes(desc, packageName, base, services);
        Map<String, EventType> declaredTypes = resolveTypesResult.types;
        additionalForgeables.addAll(resolveTypesResult.additionalForgeables);

        // resolve operator classes
        Map<Integer, OperatorMetadataDescriptor> operatorMetadata = resolveMetadata(desc, operatorAnnotations, base, services);

        // build dependency graph:  operator -> [input_providing_op, input_providing_op]
        Map<Integer, OperatorDependencyEntry> operatorDependencies = analyzeDependencies(desc);

        // determine build order of operators
        Set<Integer> operatorBuildOrder = analyzeBuildOrder(operatorDependencies);

        // instantiate operator forges
        Map<Integer, DataFlowOperatorForge> operatorForges = instantiateOperatorForges(operatorDependencies, operatorMetadata, operatorAnnotations, declaredTypes, desc, base, services);

        // Build graph that references port numbers (port number is simply the method offset number or to-be-generated slot in the list)
        InitForgesResult initForgesResult = determineChannelsInitForges(operatorForges, operatorBuildOrder, operatorAnnotations, operatorDependencies, operatorMetadata, declaredTypes, desc, codegenEnv, base, services);
        if (log.isDebugEnabled()) {
            log.debug("For flow '" + desc.getGraphName() + "' channels are: " + LogicalChannelUtil.printChannels(initForgesResult.logicalChannels));
        }

        return new DataflowDescForge(desc.getGraphName(), declaredTypes, operatorMetadata, operatorBuildOrder,
                operatorForges, initForgesResult.getLogicalChannels(), initForgesResult.forgables, additionalForgeables);
    }

    private static InitForgesResult determineChannelsInitForges(Map<Integer, DataFlowOperatorForge> operatorForges, Set<Integer> operatorBuildOrder, Map<Integer, Annotation[]> operatorAnnotations, Map<Integer, OperatorDependencyEntry> operatorDependencies, Map<Integer, OperatorMetadataDescriptor> operatorMetadata, Map<String, EventType> declaredTypes, CreateDataFlowDesc desc, DataFlowOpForgeCodegenEnv codegenEnv, StatementBaseInfo base, StatementCompileTimeServices services)
            throws ExprValidationException {
        // Step 1: find all the operators that have explicit output ports and determine the type of such
        Map<Integer, List<LogicalChannelProducingPortDeclared>> declaredOutputPorts = new HashMap<Integer, List<LogicalChannelProducingPortDeclared>>();
        for (int operatorNum : operatorBuildOrder) {
            OperatorMetadataDescriptor metadata = operatorMetadata.get(operatorNum);
            DataFlowOperatorForge operatorForge = operatorForges.get(operatorNum);
            GraphOperatorSpec operatorSpec = desc.getOperators().get(operatorNum);
            List<LogicalChannelProducingPortDeclared> annotationPorts = determineAnnotatedOutputPorts(operatorNum, operatorForge, operatorSpec, metadata, base, services);
            List<LogicalChannelProducingPortDeclared> graphDeclaredPorts = determineGraphDeclaredOutputPorts(operatorNum, operatorForge, operatorSpec, metadata, declaredTypes, services);

            List<LogicalChannelProducingPortDeclared> allDeclaredPorts = new ArrayList<>();
            allDeclaredPorts.addAll(annotationPorts);
            allDeclaredPorts.addAll(graphDeclaredPorts);

            declaredOutputPorts.put(operatorNum, allDeclaredPorts);
        }

        // Step 2: determine for each operator the output ports: some are determined via "prepare" and some can be implicit
        // since they may not be declared or can be punctuation.
        // Therefore we need to meet ends: on one end the declared types, on the other the implied and dynamically-determined types based on input.
        // We do this in operator build order.
        Map<Integer, List<LogicalChannelProducingPortCompiled>> compiledOutputPorts = new HashMap<Integer, List<LogicalChannelProducingPortCompiled>>();
        List<StmtForgeMethodResult> additionalForgeables = new ArrayList<>();
        for (int operatorNum : operatorBuildOrder) {

            OperatorMetadataDescriptor metadata = operatorMetadata.get(operatorNum);
            DataFlowOperatorForge operatorForge = operatorForges.get(operatorNum);
            GraphOperatorSpec operatorSpec = desc.getOperators().get(operatorNum);
            Annotation[] operatorAnno = operatorAnnotations.get(operatorNum);

            // Handle incoming first: if the operator has incoming ports, each of such should already have type information
            // Compile type information, call method, obtain output types.
            Set<Integer> incomingDependentOpNums = operatorDependencies.get(operatorNum).getIncoming();
            DataFlowOpForgeInitializeResult initializeResult = initializeOperatorForge(operatorNum, operatorForge, operatorAnno, metadata, operatorSpec, declaredOutputPorts, compiledOutputPorts, declaredTypes, incomingDependentOpNums, desc, codegenEnv, base, services);

            GraphTypeDesc[] typesPerOutput = null;
            if (initializeResult != null) {
                typesPerOutput = initializeResult.getTypeDescriptors();
                if (initializeResult.getAdditionalForgeables() != null) {
                    additionalForgeables.add(initializeResult.getAdditionalForgeables());
                }
            }

            // Handle outgoing second:
            //   If there is outgoing declared, use that.
            //   If output types have been determined based on input, use that.
            //   else error
            List<LogicalChannelProducingPortCompiled> outgoingPorts = determineOutgoingPorts(operatorNum, operatorSpec, metadata, compiledOutputPorts, declaredOutputPorts, typesPerOutput, incomingDependentOpNums);
            compiledOutputPorts.put(operatorNum, outgoingPorts);
        }

        // Step 3: normalization and connecting input ports with output ports (logically, no methods yet)
        List<LogicalChannel> channels = new ArrayList<LogicalChannel>();
        int channelId = 0;
        for (Integer operatorNum : operatorBuildOrder) {

            OperatorDependencyEntry dependencies = operatorDependencies.get(operatorNum);
            GraphOperatorSpec operatorSpec = desc.getOperators().get(operatorNum);
            List<GraphOperatorInputNamesAlias> inputNames = operatorSpec.getInput().getStreamNamesAndAliases();
            OperatorMetadataDescriptor descriptor = operatorMetadata.get(operatorNum);

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
                            LogicalChannel channel = new LogicalChannel(channelId++, descriptor.getOperatorName(), operatorNum, streamNum, streamName, optionalAlias, descriptor.getOperatorPrettyPrint(), port);
                            channels.add(channel);
                        }
                    }
                }
            }
        }
        return new InitForgesResult(channels, additionalForgeables);
    }

    private static Map<Integer, DataFlowOperatorForge> instantiateOperatorForges(Map<Integer, OperatorDependencyEntry> operatorDependencies, Map<Integer, OperatorMetadataDescriptor> operatorMetadata, Map<Integer, Annotation[]> operatorAnnotations, Map<String, EventType> declaredTypes, CreateDataFlowDesc createDataFlowDesc, StatementBaseInfo base, StatementCompileTimeServices services)
            throws ExprValidationException {
        Map<Integer, DataFlowOperatorForge> forges = new HashMap<>();
        for (Map.Entry<Integer, OperatorMetadataDescriptor> entry : operatorMetadata.entrySet()) {
            DataFlowOperatorForge forge = instantiateOperatorForge(createDataFlowDesc, entry.getKey(), entry.getValue(), base, services);
            forges.put(entry.getKey(), forge);
        }
        return forges;
    }

    private static DataFlowOperatorForge instantiateOperatorForge(CreateDataFlowDesc createDataFlowDesc, int operatorNum, OperatorMetadataDescriptor desc, StatementBaseInfo base, StatementCompileTimeServices services)
            throws ExprValidationException {
        GraphOperatorSpec operatorSpec = createDataFlowDesc.getOperators().get(operatorNum);
        String dataflowName = createDataFlowDesc.getGraphName();
        Class clazz = desc.getForgeClass();

        // use non-factory class if provided
        Object forgeObject;
        try {
            forgeObject = clazz.newInstance();
        } catch (Exception e) {
            throw new ExprValidationException("Failed to instantiate: " + e.getMessage());
        }

        // inject properties
        ExprValidationContext exprValidationContext = new ExprValidationContextBuilder(new StreamTypeServiceImpl(false), base.getStatementRawInfo(), services).build();
        Map<String, Object> configs = operatorSpec.getDetail() == null ? Collections.<String, Object>emptyMap() : operatorSpec.getDetail().getConfigs();
        injectObjectProperties(dataflowName, operatorSpec.getOperatorName(), operatorNum, configs, forgeObject, null, null, exprValidationContext);

        if (!(forgeObject instanceof DataFlowOperatorForge)) {
            throw new ExprValidationException("Operator object '" + forgeObject.getClass().getSimpleName() + "' does not implement the '" + DataFlowOperatorForge.class + "' interface ");
        }

        return (DataFlowOperatorForge) forgeObject;
    }

    private static Map<Integer, DataFlowOpInputPort> getInputPorts(int operatorNumber,
                                                                   GraphOperatorSpec operatorSpec,
                                                                   Set<Integer> incomingDependentOpNums,
                                                                   Map<Integer, List<LogicalChannelProducingPortDeclared>> declaredOutputPorts,
                                                                   Map<Integer, List<LogicalChannelProducingPortCompiled>> compiledOutputPorts)
            throws ExprValidationException {
        // determine input ports to build up the input port metadata
        int numDeclared = operatorSpec.getInput().getStreamNamesAndAliases().size();
        Map<Integer, DataFlowOpInputPort> inputPorts = new LinkedHashMap<Integer, DataFlowOpInputPort>();
        for (int inputPortNum = 0; inputPortNum < numDeclared; inputPortNum++) {
            GraphOperatorInputNamesAlias inputItem = operatorSpec.getInput().getStreamNamesAndAliases().get(inputPortNum);
            List<LogicalChannelProducingPortCompiled> producingPorts = LogicalChannelUtil.getOutputPortByStreamName(incomingDependentOpNums, inputItem.getInputStreamNames(), compiledOutputPorts);

            DataFlowOpInputPort port;
            if (producingPorts.isEmpty()) { // this can be when the operator itself is the incoming port, i.e. feedback loop
                List<LogicalChannelProducingPortDeclared> declareds = declaredOutputPorts.get(operatorNumber);
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

        return inputPorts;
    }

    private static Map<Integer, OperatorMetadataDescriptor> resolveMetadata(CreateDataFlowDesc desc, Map<Integer, Annotation[]> operatorAnnotations, StatementBaseInfo base, StatementCompileTimeServices services)
            throws ExprValidationException {
        Map<Integer, OperatorMetadataDescriptor> operatorClasses = new HashMap<Integer, OperatorMetadataDescriptor>();
        for (int i = 0; i < desc.getOperators().size(); i++) {
            GraphOperatorSpec operatorSpec = desc.getOperators().get(i);
            int numOutputPorts = operatorSpec.getOutput().getItems().size();
            String operatorName = operatorSpec.getOperatorName();
            String operatorPrettyPrint = toPrettyPrint(i, operatorSpec);
            Annotation[] operatorAnnotation = operatorAnnotations.get(i);

            Class forgeClass = null;
            try {
                String forgeClassName = operatorSpec.getOperatorName() + "Forge";
                forgeClass = services.getClasspathImportServiceCompileTime().resolveClass(forgeClassName, false);
            } catch (ClasspathImportException e) {

                try {
                    String forgeClassName = operatorSpec.getOperatorName();
                    forgeClass = services.getClasspathImportServiceCompileTime().resolveClass(forgeClassName, false);
                } catch (ClasspathImportException e2) {
                    // expected
                }

                if (forgeClass == null) {
                    throw new ExprValidationException("Failed to resolve forge class for operator '" + operatorSpec.getOperatorName() + "': " + e.getMessage(), e);
                }
            }

            // if the factory implements the interface use that
            if (!JavaClassHelper.isImplementsInterface(forgeClass, DataFlowOperatorForge.class)) {
                throw new ExprValidationException("Forge class for operator '" + operatorSpec.getOperatorName() + "' does not implement interface '" + DataFlowOperatorForge.class.getSimpleName() + "' (class '" + forgeClass.getName() + "')");
            }

            OperatorMetadataDescriptor descriptor = new OperatorMetadataDescriptor(forgeClass, operatorPrettyPrint, operatorAnnotation, numOutputPorts, operatorName);
            operatorClasses.put(i, descriptor);
        }
        return operatorClasses;
    }

    private static ResolveTypesResult resolveTypes(CreateDataFlowDesc desc, String packageName, StatementBaseInfo base, StatementCompileTimeServices services)
            throws ExprValidationException {
        Map<String, EventType> types = new HashMap<String, EventType>();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);
        for (CreateSchemaDesc spec : desc.getSchemas()) {
            EventTypeForgablesPair forgablesPair = EventTypeUtility.createNonVariantType(true, spec, base, services);
            additionalForgeables.addAll(forgablesPair.getAdditionalForgeables());
            EventType eventType = forgablesPair.getEventType();
            types.put(spec.getSchemaName(), eventType);
        }
        return new ResolveTypesResult(types, additionalForgeables);
    }

    private static void validate(CreateDataFlowDesc desc) throws ExprValidationException {
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

    private static String toPrettyPrint(int operatorNum, GraphOperatorSpec spec) {
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

    private static void toPrettyPrintInput(GraphOperatorInputNamesAlias inputItem, StringWriter writer) {
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

    private static void writeTypes(List<GraphOperatorOutputItemType> types, StringWriter writer) {
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

    private static void writeType(GraphOperatorOutputItemType type, StringWriter writer) {
        if (type.isWildcard()) {
            writer.append('?');
            return;
        }
        writer.append(type.getTypeOrClassname());
        writeTypes(type.getTypeParameters(), writer);
    }

    private static Map<Integer, OperatorDependencyEntry> analyzeDependencies(CreateDataFlowDesc graphDesc)
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

    private static Set<Integer> analyzeBuildOrder(Map<Integer, OperatorDependencyEntry> operators) throws ExprValidationException {

        DependencyGraph graph = new DependencyGraph(operators.size(), true);
        for (Map.Entry<Integer, OperatorDependencyEntry> entry : operators.entrySet()) {
            int myOpNum = entry.getKey();
            Set<Integer> incomings = entry.getValue().getIncoming();
            for (int incoming : incomings) {
                if (myOpNum != incoming) {
                    graph.addDependency(myOpNum, incoming);
                }
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

    private static void injectObjectProperties(String dataFlowName, String operatorName, int operatorNum, Map<String, Object> configs, Object instance, EPDataFlowOperatorParameterProvider optionalParameterProvider, Map<String, Object> optionalParameterURIs, ExprValidationContext exprValidationContext)
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

    private static List<LogicalChannelProducingPortDeclared> determineAnnotatedOutputPorts(int operatorNumber, DataFlowOperatorForge forge, GraphOperatorSpec operatorSpec, OperatorMetadataDescriptor descriptor, StatementBaseInfo base, StatementCompileTimeServices services)
            throws ExprValidationException {

        List<LogicalChannelProducingPortDeclared> ports = new ArrayList<LogicalChannelProducingPortDeclared>();

        // See if any @OutputTypes annotations exists
        List<Annotation> annotations = JavaClassHelper.getAnnotations(OutputTypes.class, forge.getClass().getDeclaredAnnotations());

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
                    clazz = JavaClassHelper.getClassForSimpleName(typeName, services.getClasspathImportServiceCompileTime().getClassForNameProvider());
                    if (clazz == null) {
                        try {
                            clazz = services.getClasspathImportServiceCompileTime().resolveClass(typeName, false);
                        } catch (ClasspathImportException e) {
                            throw new RuntimeException("Failed to resolve type '" + typeName + "'");
                        }
                    }
                }
                propertiesRaw.put(outputType.name(), clazz);
            }

            Map<String, Object> propertiesCompiled = EventTypeUtility.compileMapTypeProperties(propertiesRaw, services.getEventTypeCompileTimeResolver());
            String eventTypeName = services.getEventTypeNameGeneratorStatement().getDataflowOperatorTypeName(operatorNumber);
            EventTypeMetadata metadata = new EventTypeMetadata(eventTypeName, base.getModuleName(), EventTypeTypeClass.DBDERIVED, EventTypeApplicationType.OBJECTARR, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
            EventType eventType = BaseNestableEventUtil.makeOATypeCompileTime(metadata, propertiesCompiled, null, null, null, null, services.getBeanEventTypeFactoryPrivate(), services.getEventTypeCompileTimeResolver());
            services.getEventTypeCompileTimeRegistry().newType(eventType);

            // determine output stream name, which must be provided
            List<GraphOperatorOutputItem> declaredOutput = operatorSpec.getOutput().getItems();
            if (declaredOutput.isEmpty()) {
                throw new ExprValidationException("No output stream declared");
            }
            if (declaredOutput.size() < outputTypes.portNumber()) {
                throw new ExprValidationException("No output stream declared for this port");
            }
            String streamName = declaredOutput.get(outputTypes.portNumber()).getStreamName();

            boolean isDeclaredPunctuated = JavaClassHelper.isAnnotationListed(DataFlowOpProvideSignal.class, forge.getClass().getAnnotations());
            LogicalChannelProducingPortDeclared port = new LogicalChannelProducingPortDeclared(operatorNumber, descriptor.getOperatorPrettyPrint(), streamName, outputTypes.portNumber(), new GraphTypeDesc(false, false, eventType), isDeclaredPunctuated);
            ports.add(port);
        }

        return ports;
    }

    private static List<LogicalChannelProducingPortDeclared> determineGraphDeclaredOutputPorts(int producingOpNum, DataFlowOperatorForge operatorForge, GraphOperatorSpec operatorSpec, OperatorMetadataDescriptor metadata, Map<String, EventType> types, StatementCompileTimeServices services)
            throws ExprValidationException {

        List<LogicalChannelProducingPortDeclared> ports = new ArrayList<LogicalChannelProducingPortDeclared>();

        int portNumber = 0;
        for (GraphOperatorOutputItem outputItem : operatorSpec.getOutput().getItems()) {
            if (outputItem.getTypeInfo().size() > 1) {
                throw new ExprValidationException("Multiple parameter types are not supported");
            }

            if (!outputItem.getTypeInfo().isEmpty()) {
                GraphTypeDesc typeDesc = determineTypeOutputPort(outputItem.getTypeInfo().get(0), types, services);
                Class operatorForgeClass = operatorForge.getClass();
                boolean isDeclaredPunctuated = JavaClassHelper.isAnnotationListed(DataFlowOpProvideSignal.class, operatorForgeClass.getAnnotations());
                ports.add(new LogicalChannelProducingPortDeclared(producingOpNum, metadata.getOperatorPrettyPrint(), outputItem.getStreamName(), portNumber, typeDesc, isDeclaredPunctuated));
            }
            portNumber++;
        }

        return ports;
    }

    private static GraphTypeDesc determineTypeOutputPort(GraphOperatorOutputItemType outType, Map<String, EventType> types, StatementCompileTimeServices services)
            throws ExprValidationException {

        EventType eventType = null;
        boolean isWildcard = false;
        boolean isUnderlying = true;

        String typeOrClassname = outType.getTypeOrClassname();
        if (typeOrClassname != null && typeOrClassname.toLowerCase(Locale.ENGLISH).equals(EVENT_WRAPPED_TYPE)) {
            isUnderlying = false;
            if (!outType.getTypeParameters().isEmpty() && !outType.getTypeParameters().get(0).isWildcard()) {
                String typeName = outType.getTypeParameters().get(0).getTypeOrClassname();
                eventType = resolveType(typeName, types, services);
            } else {
                isWildcard = true;
            }
        } else if (typeOrClassname != null) {
            eventType = resolveType(typeOrClassname, types, services);
        } else {
            isWildcard = true;
        }
        return new GraphTypeDesc(isWildcard, isUnderlying, eventType);
    }

    private static EventType resolveType(String typeOrClassname, Map<String, EventType> types, StatementCompileTimeServices services)
            throws ExprValidationException {
        EventType eventType = types.get(typeOrClassname);
        if (eventType == null) {
            eventType = services.getEventTypeCompileTimeResolver().getTypeByName(typeOrClassname);
        }
        if (eventType == null) {
            throw new ExprValidationException("Failed to find event type '" + typeOrClassname + "'");
        }
        return eventType;
    }

    private static DataFlowOpForgeInitializeResult initializeOperatorForge(int operatorNumber,
                                                                           DataFlowOperatorForge forge,
                                                                           Annotation[] operatorAnnotations,
                                                                           OperatorMetadataDescriptor meta,
                                                                           GraphOperatorSpec operatorSpec,
                                                                           Map<Integer, List<LogicalChannelProducingPortDeclared>> declaredOutputPorts,
                                                                           Map<Integer, List<LogicalChannelProducingPortCompiled>> compiledOutputPorts,
                                                                           Map<String, EventType> types,
                                                                           Set<Integer> incomingDependentOpNums,
                                                                           CreateDataFlowDesc desc,
                                                                           DataFlowOpForgeCodegenEnv codegenEnv,
                                                                           StatementBaseInfo base,
                                                                           StatementCompileTimeServices services)
            throws ExprValidationException {
        // determine input ports to build up the input port metadata
        int numDeclared = operatorSpec.getInput().getStreamNamesAndAliases().size();
        Map<Integer, DataFlowOpInputPort> inputPorts = new LinkedHashMap<Integer, DataFlowOpInputPort>();
        for (int inputPortNum = 0; inputPortNum < numDeclared; inputPortNum++) {
            GraphOperatorInputNamesAlias inputItem = operatorSpec.getInput().getStreamNamesAndAliases().get(inputPortNum);
            List<LogicalChannelProducingPortCompiled> producingPorts = LogicalChannelUtil.getOutputPortByStreamName(incomingDependentOpNums, inputItem.getInputStreamNames(), compiledOutputPorts);

            DataFlowOpInputPort port;
            if (producingPorts.isEmpty()) { // this can be when the operator itself is the incoming port, i.e. feedback loop
                List<LogicalChannelProducingPortDeclared> declareds = declaredOutputPorts.get(operatorNumber);
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
                    throw new ExprValidationException("Failed validation for operator '" + operatorSpec.getOperatorName() + "': Failed to find output port for input port " + inputPortNum);
                }
                port = new DataFlowOpInputPort(foundDeclared.getTypeDesc(), new HashSet<String>(Arrays.asList(inputItem.getInputStreamNames())), inputItem.getOptionalAsName(), false);
            } else {
                port = new DataFlowOpInputPort(new GraphTypeDesc(false, false, producingPorts.get(0).getGraphTypeDesc().getEventType()), new HashSet<String>(Arrays.asList(inputItem.getInputStreamNames())), inputItem.getOptionalAsName(), producingPorts.get(0).isHasPunctuation());
            }
            inputPorts.put(inputPortNum, port);
        }

        // determine output ports to build up the output port metadata
        Map<Integer, DataFlowOpOutputPort> outputPorts = getDeclaredOutputPorts(operatorSpec, types, services);

        DataFlowOpForgeInitializeResult initializeResult;
        try {
            DataFlowOpForgeInitializeContext context = new DataFlowOpForgeInitializeContext(desc.getGraphName(),
                    operatorNumber, operatorAnnotations, operatorSpec, inputPorts, outputPorts,
                    codegenEnv, base, services);
            initializeResult = forge.initializeForge(context);
        } catch (Throwable t) {
            throw new ExprValidationException("Failed to obtain operator '" + operatorSpec.getOperatorName() + "': " + t.getMessage(), t);
        }
        return initializeResult;
    }

    private static List<LogicalChannelProducingPortCompiled> determineOutgoingPorts(int myOpNum,
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

    private static boolean determineReceivesPunctuation(Set<Integer> incomingDependentOpNums, GraphOperatorInput input, Map<Integer, List<LogicalChannelProducingPortCompiled>> compiledOutputPorts) {
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

    private static void compareTypeInfo(String operatorName, String firstName, GraphTypeDesc firstType, String otherName, GraphTypeDesc otherType)
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

    private static Map<Integer, DataFlowOpOutputPort> getDeclaredOutputPorts(GraphOperatorSpec operatorSpec, Map<String, EventType> types, StatementCompileTimeServices services)
            throws ExprValidationException {
        Map<Integer, DataFlowOpOutputPort> outputPorts = new LinkedHashMap<Integer, DataFlowOpOutputPort>();
        for (int outputPortNum = 0; outputPortNum < operatorSpec.getOutput().getItems().size(); outputPortNum++) {
            GraphOperatorOutputItem outputItem = operatorSpec.getOutput().getItems().get(outputPortNum);

            GraphTypeDesc typeDesc = null;
            if (!outputItem.getTypeInfo().isEmpty()) {
                typeDesc = determineTypeOutputPort(outputItem.getTypeInfo().get(0), types, services);
            }
            outputPorts.put(outputPortNum, new DataFlowOpOutputPort(outputItem.getStreamName(), typeDesc));
        }

        return outputPorts;
    }

    private static class InitForgesResult {
        private final List<LogicalChannel> logicalChannels;
        private final List<StmtForgeMethodResult> forgables;

        public InitForgesResult(List<LogicalChannel> logicalChannels, List<StmtForgeMethodResult> forgables) {
            this.logicalChannels = logicalChannels;
            this.forgables = forgables;
        }

        public List<LogicalChannel> getLogicalChannels() {
            return logicalChannels;
        }

        public List<StmtForgeMethodResult> getForgables() {
            return forgables;
        }
    }

    private static class ResolveTypesResult {
        private final Map<String, EventType> types;
        private final List<StmtClassForgeableFactory> additionalForgeables;

        public ResolveTypesResult(Map<String, EventType> types, List<StmtClassForgeableFactory> additionalForgeables) {
            this.types = types;
            this.additionalForgeables = additionalForgeables;
        }

        public Map<String, EventType> getTypes() {
            return types;
        }

        public List<StmtClassForgeableFactory> getAdditionalForgeables() {
            return additionalForgeables;
        }
    }
}
