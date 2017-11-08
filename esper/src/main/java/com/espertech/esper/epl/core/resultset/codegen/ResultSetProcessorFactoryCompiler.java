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
package com.espertech.esper.epl.core.resultset.codegen;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.codegen.compile.CodegenClassGenerator;
import com.espertech.esper.codegen.compile.CodegenMessageUtil;
import com.espertech.esper.codegen.core.*;
import com.espertech.esper.codegen.util.CodegenStackGenerator;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames;
import com.espertech.esper.epl.agg.codegen.AggregationServiceFactoryCompiler;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactory;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactoryDesc;
import com.espertech.esper.epl.agg.service.common.AggregationServiceForgeDesc;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.orderby.OrderByProcessor;
import com.espertech.esper.epl.core.orderby.OrderByProcessorCompiler;
import com.espertech.esper.epl.core.orderby.OrderByProcessorFactory;
import com.espertech.esper.epl.core.orderby.OrderByProcessorFactoryForge;
import com.espertech.esper.epl.core.resultset.core.*;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorCompiler;
import com.espertech.esper.epl.core.select.SelectExprProcessorCompilerResult;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.view.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;
import java.util.function.Supplier;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputConditionType.POLICY_LASTALL_UNORDERED;

public class ResultSetProcessorFactoryCompiler {
    private final static Logger log = LoggerFactory.getLogger(ResultSetProcessorFactoryCompiler.class);

    private final static String CLASSNAME_RESULTSETPROCESSORFACTORY = "RSPFactory";
    private final static String CLASSNAME_RESULTSETPROCESSOR = "RSP";
    private final static String MEMBERNAME_RESULTSETPROCESSORFACTORY = "rspFactory";
    private final static String MEMBERNAME_AGGREGATIONSVCFACTORY = "aggFactory";
    private final static String MEMBERNAME_ORDERBYFACTORY = "orderByFactory";

    public static ResultSetProcessorFactoryDesc allocate(ResultSetProcessorFactoryForge forge, ResultSetProcessorType resultSetProcessorType, EventType resultEventType, StatementContext stmtContext, boolean isFireAndForget, boolean join, boolean hasOutputLimit, ResultSetProcessorOutputConditionType outputConditionType, boolean hasOutputLimitSnapshot, SelectExprProcessorForge[] selectExprProcessorForge, boolean rollup, AggregationServiceForgeDesc aggregationServiceForgeDesc, OrderByProcessorFactoryForge orderByProcessorForge) {
        EngineImportService engineImportService = stmtContext.getEngineImportService();

        if (!engineImportService.getByteCodeGeneration().isEnableResultSet() || isFireAndForget) {
            AggregationServiceFactory aggregationServiceFactory = AggregationServiceFactoryCompiler.allocate(aggregationServiceForgeDesc.getAggregationServiceFactoryForge(), stmtContext, isFireAndForget);
            ResultSetProcessorFactory resultSetProcessorFactory = forge.getResultSetProcessorFactory(stmtContext, isFireAndForget);
            AggregationServiceFactoryDesc aggregationServiceFactoryDesc = new AggregationServiceFactoryDesc(aggregationServiceFactory, aggregationServiceForgeDesc.getExpressions(), aggregationServiceForgeDesc.getGroupKeyExpressions());
            OrderByProcessorFactory orderByProcessorFactory = orderByProcessorForge == null ? null : orderByProcessorForge.make(engineImportService, isFireAndForget, stmtContext.getStatementName());
            return new ResultSetProcessorFactoryDesc(resultSetProcessorFactory, resultSetProcessorType, resultEventType, orderByProcessorFactory, aggregationServiceFactoryDesc);
        }

        Supplier<String> debugInformationProvider = new Supplier<String>() {
            public String get() {
                StringWriter writer = new StringWriter();
                writer.append("statement '")
                        .append(stmtContext.getStatementName())
                        .append("' result-set-processor");
                writer.append(" requestor-class '")
                        .append(ResultSetProcessorFactoryFactory.class.getSimpleName())
                        .append("'");
                return writer.toString();
            }
        };

        try {
            CodegenClassScope classScope = new CodegenClassScope(engineImportService.getByteCodeGeneration().isIncludeComments());
            List<CodegenInnerClass> innerClasses = new ArrayList<>();
            CodegenCtor providerCtor = new CodegenCtor(ResultSetProcessorFactoryCompiler.class, classScope, Collections.emptyList());
            List<CodegenTypedParam> providerExplicitMembers = new ArrayList<>(2);
            String providerClassName = CodeGenerationIDGenerator.generateClassName(ResultSetProcessorFactoryProvider.class);

            makeResultSetProcessorFactory(classScope, innerClasses, providerExplicitMembers, providerCtor, providerClassName);

            makeResultSetProcessor(classScope, innerClasses, providerExplicitMembers, providerCtor, providerClassName, forge, join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, resultEventType);

            OrderByProcessorCompiler.makeOrderByProcessors(orderByProcessorForge, classScope, innerClasses, providerExplicitMembers, providerCtor, providerClassName, MEMBERNAME_ORDERBYFACTORY);

            providerExplicitMembers.add(new CodegenTypedParam(AggregationServiceFactory.class, MEMBERNAME_AGGREGATIONSVCFACTORY));
            if (!engineImportService.getByteCodeGeneration().isEnableAggregation()) {
                AggregationServiceFactory factory = aggregationServiceForgeDesc.getAggregationServiceFactoryForge().getAggregationServiceFactory(stmtContext, isFireAndForget);
                CodegenMember memberAggFactory = classScope.makeAddMember(AggregationServiceFactory.class, factory);
                providerCtor.getBlock().assignRef(MEMBERNAME_AGGREGATIONSVCFACTORY, member(memberAggFactory.getMemberId()));
            } else {
                AggregationServiceFactoryCompiler.makeInnerClasses(aggregationServiceForgeDesc.getAggregationServiceFactoryForge(), classScope, innerClasses, providerClassName, stmtContext, isFireAndForget);
                providerCtor.getBlock().assignRef(MEMBERNAME_AGGREGATIONSVCFACTORY, newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONSERVICEFACTORY, ref("this")));
            }

            makeSelectExprProcessors(classScope, innerClasses, providerExplicitMembers, providerCtor, providerClassName, rollup, selectExprProcessorForge, stmtContext);

            // make provider methods
            CodegenMethodNode getResultSetProcessorFactoryMethod = CodegenMethodNode.makeParentNode(ResultSetProcessorFactory.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getResultSetProcessorFactoryMethod.getBlock().methodReturn(ref(MEMBERNAME_RESULTSETPROCESSORFACTORY));

            CodegenMethodNode getAggregationServiceFactoryMethod = CodegenMethodNode.makeParentNode(AggregationServiceFactory.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getAggregationServiceFactoryMethod.getBlock().methodReturn(ref(MEMBERNAME_AGGREGATIONSVCFACTORY));

            CodegenMethodNode getOrderByProcessorFactoryMethod = CodegenMethodNode.makeParentNode(OrderByProcessorFactory.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getOrderByProcessorFactoryMethod.getBlock().methodReturn(ref(MEMBERNAME_ORDERBYFACTORY));

            CodegenClassMethods methods = new CodegenClassMethods();
            CodegenStackGenerator.recursiveBuildStack(providerCtor, "ctor", methods);
            CodegenStackGenerator.recursiveBuildStack(getResultSetProcessorFactoryMethod, "getResultSetProcessorFactory", methods);
            CodegenStackGenerator.recursiveBuildStack(getAggregationServiceFactoryMethod, "getAggregationServiceFactory", methods);
            CodegenStackGenerator.recursiveBuildStack(getOrderByProcessorFactoryMethod, "getOrderByProcessorFactory", methods);

            // render and compile
            CodegenClass clazz = new CodegenClass(ResultSetProcessorFactoryProvider.class, engineImportService.getCodegenCompiler().getPackageName(), providerClassName, classScope, providerExplicitMembers, providerCtor, methods, innerClasses);
            ResultSetProcessorFactoryProvider factoryProvider = CodegenClassGenerator.compile(clazz, engineImportService, ResultSetProcessorFactoryProvider.class, debugInformationProvider);
            AggregationServiceFactoryDesc aggregationServiceFactoryDesc = new AggregationServiceFactoryDesc(factoryProvider.getAggregationServiceFactory(), aggregationServiceForgeDesc.getExpressions(), aggregationServiceForgeDesc.getGroupKeyExpressions());
            return new ResultSetProcessorFactoryDesc(factoryProvider.getResultSetProcessorFactory(), resultSetProcessorType, resultEventType, factoryProvider.getOrderByProcessorFactory(), aggregationServiceFactoryDesc);
        } catch (Throwable t) {
            boolean fallback = engineImportService.getByteCodeGeneration().isEnableFallback();
            String message = CodegenMessageUtil.getFailedCompileLogMessageWithCode(t, debugInformationProvider, fallback);
            if (fallback) {
                log.warn(message, t);
            } else {
                log.error(message, t);
            }
            return handleThrowable(stmtContext, t, forge, debugInformationProvider, isFireAndForget, stmtContext.getStatementName(), resultEventType, orderByProcessorForge, aggregationServiceForgeDesc, resultSetProcessorType);
        }
    }

    private static void makeResultSetProcessorFactory(CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, List<CodegenTypedParam> providerExplicitMembers, CodegenCtor providerCtor, String providerClassName) {
        CodegenMethodNode instantiateMethod = CodegenMethodNode.makeParentNode(ResultSetProcessor.class, ResultSetProcessorFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(OrderByProcessor.class, NAME_ORDERBYPROCESSOR)
                .addParam(AggregationService.class, NAME_AGGREGATIONSVC)
                .addParam(AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT);
        instantiateMethod.getBlock().methodReturn(newInstanceInnerClass(CLASSNAME_RESULTSETPROCESSOR, ref("o"), REF_ORDERBYPROCESSOR, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT));
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(instantiateMethod, "instantiate", methods);

        List<CodegenTypedParam> ctorParams = Collections.singletonList(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(ResultSetProcessorFactoryCompiler.class, classScope, ctorParams);

        CodegenInnerClass innerClass = new CodegenInnerClass(CLASSNAME_RESULTSETPROCESSORFACTORY, ResultSetProcessorFactory.class, ctor, Collections.emptyList(), Collections.emptyMap(), methods);
        innerClasses.add(innerClass);

        providerExplicitMembers.add(new CodegenTypedParam(ResultSetProcessorFactory.class, "rspFactory"));
        providerCtor.getBlock().assignRef(MEMBERNAME_RESULTSETPROCESSORFACTORY, newInstanceInnerClass(CLASSNAME_RESULTSETPROCESSORFACTORY, ref("this")));
    }

    private static void makeResultSetProcessor(CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, List<CodegenTypedParam> factoryExplicitMembers, CodegenCtor factoryCtor, String classNameParent, ResultSetProcessorFactoryForge forge, boolean join, boolean hasOutputLimit, ResultSetProcessorOutputConditionType outputConditionType, boolean hasOutputLimitSnapshot, EventType resultEventType) {

        List<CodegenTypedParam> ctorParams = new ArrayList<>();
        ctorParams.add(new CodegenTypedParam(classNameParent, "o"));
        ctorParams.add(new CodegenTypedParam(OrderByProcessor.class, "orderByProcessor"));
        ctorParams.add(new CodegenTypedParam(AggregationService.class, "aggregationService"));
        ctorParams.add(new CodegenTypedParam(AgentInstanceContext.class, "agentInstanceContext"));

        // make ctor code
        CodegenCtor serviceCtor = new CodegenCtor(ResultSetProcessorFactoryCompiler.class, classScope, ctorParams);

        // Get-Result-Type Method
        CodegenMethodNode getResultEventTypeMethod = CodegenMethodNode.makeParentNode(EventType.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        CodegenMember memberResultType = classScope.makeAddMember(EventType.class, resultEventType);
        getResultEventTypeMethod.getBlock().methodReturn(member(memberResultType.getMemberId()));

        // Instance members and methods
        CodegenInstanceAux instance = new CodegenInstanceAux(serviceCtor);
        forge.instanceCodegen(instance, classScope, factoryCtor, factoryExplicitMembers);

        // Process-View-Result Method
        CodegenMethodNode processViewResultMethod = CodegenMethodNode.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EventBean[].class, NAME_NEWDATA).addParam(EventBean[].class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join) {
            forge.processViewResultCodegen(classScope, processViewResultMethod, instance);
        } else {
            processViewResultMethod.getBlock().methodThrowUnsupported();
        }

        // Process-Join-Result Method
        CodegenMethodNode processJoinResultMethod = CodegenMethodNode.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Set.class, NAME_NEWDATA).addParam(Set.class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join) {
            processJoinResultMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.processJoinResultCodegen(classScope, processJoinResultMethod, instance);
        }

        // Clear-Method
        CodegenMethodNode clearMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        forge.clearMethodCodegen(classScope, clearMethod);

        // Get-Iterator-View
        CodegenMethodNode getIteratorMethodView = CodegenMethodNode.makeParentNode(Iterator.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Viewable.class, NAME_VIEWABLE);
        if (!join) {
            forge.getIteratorViewCodegen(classScope, getIteratorMethodView, instance);
        } else {
            getIteratorMethodView.getBlock().methodThrowUnsupported();
        }

        // Get-Iterator-Join
        CodegenMethodNode getIteratorMethodJoin = CodegenMethodNode.makeParentNode(Iterator.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Set.class, NAME_JOINSET);
        if (!join) {
            getIteratorMethodJoin.getBlock().methodThrowUnsupported();
        } else {
            forge.getIteratorJoinCodegen(classScope, getIteratorMethodJoin, instance);
        }

        // Process-output-rate-buffered-view
        CodegenMethodNode processOutputLimitedViewMethod = CodegenMethodNode.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(List.class, NAME_VIEWEVENTSLIST).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join && hasOutputLimit && !hasOutputLimitSnapshot) {
            forge.processOutputLimitedViewCodegen(classScope, processOutputLimitedViewMethod, instance);
        } else {
            processOutputLimitedViewMethod.getBlock().methodThrowUnsupported();
        }

        // Process-output-rate-buffered-join
        CodegenMethodNode processOutputLimitedJoinMethod = CodegenMethodNode.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(List.class, NAME_JOINEVENTSSET).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join || !hasOutputLimit || hasOutputLimitSnapshot) {
            processOutputLimitedJoinMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.processOutputLimitedJoinCodegen(classScope, processOutputLimitedJoinMethod, instance);
        }

        // Set-Agent-Instance is supported for fire-and-forget queries only
        CodegenMethodNode setAgentInstanceContextMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(AgentInstanceContext.class, "context");
        // not supported as used only for fire-and-forget queries
        setAgentInstanceContextMethod.getBlock().methodThrowUnsupported();

        // Apply-view
        CodegenMethodNode applyViewResultMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EventBean[].class, NAME_NEWDATA).addParam(EventBean[].class, NAME_OLDDATA);
        if (!join && hasOutputLimit && hasOutputLimitSnapshot) {
            forge.applyViewResultCodegen(classScope, applyViewResultMethod, instance);
        } else {
            applyViewResultMethod.getBlock().methodThrowUnsupported();
        }

        // Apply-join
        CodegenMethodNode applyJoinResultMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Set.class, NAME_NEWDATA).addParam(Set.class, NAME_OLDDATA);
        if (!join || !hasOutputLimit || !hasOutputLimitSnapshot) {
            applyJoinResultMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.applyJoinResultCodegen(classScope, applyJoinResultMethod, instance);
        }

        // Process-output-unbuffered-view
        CodegenMethodNode processOutputLimitedLastAllNonBufferedViewMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EventBean[].class, NAME_NEWDATA).addParam(EventBean[].class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join && hasOutputLimit && outputConditionType == POLICY_LASTALL_UNORDERED) {
            forge.processOutputLimitedLastAllNonBufferedViewCodegen(classScope, processOutputLimitedLastAllNonBufferedViewMethod, instance);
        } else {
            processOutputLimitedLastAllNonBufferedViewMethod.getBlock().methodThrowUnsupported();
        }

        // Process-output-unbuffered-join
        CodegenMethodNode processOutputLimitedLastAllNonBufferedJoinMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Set.class, NAME_NEWDATA).addParam(Set.class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join || !hasOutputLimit || outputConditionType != POLICY_LASTALL_UNORDERED) {
            processOutputLimitedLastAllNonBufferedJoinMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.processOutputLimitedLastAllNonBufferedJoinCodegen(classScope, processOutputLimitedLastAllNonBufferedJoinMethod, instance);
        }

        // Continue-output-unbuffered-view
        CodegenMethodNode continueOutputLimitedLastAllNonBufferedViewMethod = CodegenMethodNode.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join && hasOutputLimit && outputConditionType == POLICY_LASTALL_UNORDERED) {
            forge.continueOutputLimitedLastAllNonBufferedViewCodegen(classScope, continueOutputLimitedLastAllNonBufferedViewMethod, instance);
        } else {
            continueOutputLimitedLastAllNonBufferedViewMethod.getBlock().methodThrowUnsupported();
        }

        // Continue-output-unbuffered-join
        CodegenMethodNode continueOutputLimitedLastAllNonBufferedJoinMethod = CodegenMethodNode.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join || !hasOutputLimit || outputConditionType != POLICY_LASTALL_UNORDERED) {
            continueOutputLimitedLastAllNonBufferedJoinMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.continueOutputLimitedLastAllNonBufferedJoinCodegen(classScope, continueOutputLimitedLastAllNonBufferedJoinMethod, instance);
        }

        // Accept-Helper-Visitor
        CodegenMethodNode acceptHelperVisitorMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(ResultSetProcessorOutputHelperVisitor.class, NAME_RESULTSETVISITOR);
        forge.acceptHelperVisitorCodegen(classScope, acceptHelperVisitorMethod, instance);

        // Stop-Method (generates last as other methods may allocate members)
        CodegenMethodNode stopMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        forge.stopMethodCodegen(classScope, stopMethod, instance);

        CodegenClassMethods innerMethods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(getResultEventTypeMethod, "getResultEventType", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(processViewResultMethod, "processViewResult", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(processJoinResultMethod, "processJoinResult", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getIteratorMethodView, "getIterator", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getIteratorMethodJoin, "getIterator", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(clearMethod, "clear", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(stopMethod, "stop", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(processOutputLimitedJoinMethod, "processOutputLimitedJoin", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(processOutputLimitedViewMethod, "processOutputLimitedView", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(setAgentInstanceContextMethod, "setAgentInstanceContext", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(applyViewResultMethod, "applyViewResult", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(applyJoinResultMethod, "applyJoinResult", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(processOutputLimitedLastAllNonBufferedViewMethod, "processOutputLimitedLastAllNonBufferedView", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(processOutputLimitedLastAllNonBufferedJoinMethod, "processOutputLimitedLastAllNonBufferedJoin", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(continueOutputLimitedLastAllNonBufferedViewMethod, "continueOutputLimitedLastAllNonBufferedView", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(continueOutputLimitedLastAllNonBufferedJoinMethod, "continueOutputLimitedLastAllNonBufferedJoin", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(acceptHelperVisitorMethod, "acceptHelperVisitor", innerMethods);
        for (Map.Entry<String, CodegenMethodNode> methodEntry : instance.getMethods().getMethods().entrySet()) {
            CodegenStackGenerator.recursiveBuildStack(methodEntry.getValue(), methodEntry.getKey(), innerMethods);
        }

        CodegenInnerClass innerClass = new CodegenInnerClass(CLASSNAME_RESULTSETPROCESSOR, forge.getInterfaceClass(), serviceCtor, instance.getMembers(), Collections.emptyMap(), innerMethods);
        innerClasses.add(innerClass);
    }

    private static void makeSelectExprProcessors(CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, List<CodegenTypedParam> explicitMembers, CodegenCtor outerClassCtor, String classNameParent, boolean rollup, SelectExprProcessorForge[] forges, StatementContext stmtContext) {
        // handle single-select
        if (!rollup) {
            String name = "SelectExprProcessorImpl";
            explicitMembers.add(new CodegenTypedParam(SelectExprProcessor.class, "selectExprProcessor"));
            outerClassCtor.getBlock().assignRef("selectExprProcessor", newInstanceInnerClass(name, ref("this")));
            CodegenInnerClass innerClass = makeSelectExprProcessor(name, classNameParent, classScope, forges[0], stmtContext);
            innerClasses.add(innerClass);
            return;
        }

        // handle multi-select
        for (int i = 0; i < forges.length; i++) {
            String name = "SelectExprProcessorImpl" + i;
            SelectExprProcessorForge forge = forges[i];
            CodegenInnerClass innerClass = makeSelectExprProcessor(name, classNameParent, classScope, forge, stmtContext);
            innerClasses.add(innerClass);
        }
        explicitMembers.add(new CodegenTypedParam(SelectExprProcessor[].class, "selectExprProcessorArray"));
        outerClassCtor.getBlock().assignRef("selectExprProcessorArray", newArrayByLength(SelectExprProcessor.class, constant(forges.length)));
        for (int i = 0; i < forges.length; i++) {
            outerClassCtor.getBlock().assignArrayElement("selectExprProcessorArray", constant(i), newInstanceInnerClass("SelectExprProcessorImpl" + i, ref("this")));
        }
    }

    private static CodegenInnerClass makeSelectExprProcessor(String className, String classNameParent, CodegenClassScope classScope, SelectExprProcessorForge forge, StatementContext stmtContext) {
        SelectExprProcessorCompilerResult selectClassCode = SelectExprProcessorCompiler.generate(classScope, forge, stmtContext.getEngineImportService(), stmtContext.getEventAdapterService());
        CodegenClassMethods selectClassMethods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(selectClassCode.getTopNode(), "process", selectClassMethods);
        List<CodegenTypedParam> selectExprCtorParams = Collections.singletonList(new CodegenTypedParam(classNameParent, "o"));
        CodegenCtor selectExprCtor = new CodegenCtor(ResultSetProcessorFactoryCompiler.class, classScope, selectExprCtorParams);
        return new CodegenInnerClass(className, SelectExprProcessor.class, selectExprCtor, Collections.emptyList(), Collections.emptyMap(), selectClassMethods);
    }

    private static ResultSetProcessorFactoryDesc handleThrowable(StatementContext statementContext, Throwable t, ResultSetProcessorFactoryForge forge, Supplier<String> debugInformationProvider, boolean isFireAndForget, String statementName, EventType resultEventType, OrderByProcessorFactoryForge orderByProcessorForge, AggregationServiceForgeDesc aggregationServiceForgeDesc, ResultSetProcessorType resultSetProcessorType) {
        if (statementContext.getEngineImportService().getByteCodeGeneration().isEnableFallback()) {
            AggregationServiceFactory aggregationServiceFactory = aggregationServiceForgeDesc.getAggregationServiceFactoryForge().getAggregationServiceFactory(statementContext, isFireAndForget);
            ResultSetProcessorFactory resultSetProcessorFactory = forge.getResultSetProcessorFactory(statementContext, isFireAndForget);
            AggregationServiceFactoryDesc aggregationServiceFactoryDesc = new AggregationServiceFactoryDesc(aggregationServiceFactory, aggregationServiceForgeDesc.getExpressions(), aggregationServiceForgeDesc.getGroupKeyExpressions());
            OrderByProcessorFactory orderByProcessorFactory = orderByProcessorForge == null ? null : orderByProcessorForge.make(statementContext.getEngineImportService(), isFireAndForget, statementName);
            return new ResultSetProcessorFactoryDesc(resultSetProcessorFactory, resultSetProcessorType, resultEventType, orderByProcessorFactory, aggregationServiceFactoryDesc);
        }
        throw new EPException("Fatal exception during code-generation for " + debugInformationProvider.get() + " (see error log for further details): " + t.getMessage(), t);
    }
}
