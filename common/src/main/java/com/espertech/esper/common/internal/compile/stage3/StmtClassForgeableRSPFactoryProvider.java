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
package com.espertech.esper.common.internal.compile.stage3;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.*;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.resultset.core.*;
import com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessor;
import com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorCompiler;
import com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorFactory;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessor;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectExprProcessorForge;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.io.StringWriter;
import java.util.*;
import java.util.function.Supplier;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EPS;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorOutputConditionType.POLICY_LASTALL_UNORDERED;

public class StmtClassForgeableRSPFactoryProvider implements StmtClassForgeable {
    private final static String CLASSNAME_RESULTSETPROCESSORFACTORY = "RSPFactory";
    private final static String CLASSNAME_RESULTSETPROCESSOR = "RSP";
    private final static String MEMBERNAME_RESULTSETPROCESSORFACTORY = "rspFactory";
    private final static String MEMBERNAME_AGGREGATIONSVCFACTORY = "aggFactory";
    private final static String MEMBERNAME_ORDERBYFACTORY = "orderByFactory";
    private final static String MEMBERNAME_RESULTEVENTTYPE = "resultEventType";
    private final static CodegenExpressionMember MEMBER_EVENTBEANFACTORY = member("ebfactory");

    private final String className;
    private final ResultSetProcessorDesc spec;
    private final CodegenPackageScope packageScope;
    private final StatementRawInfo statementRawInfo;

    public StmtClassForgeableRSPFactoryProvider(String className, ResultSetProcessorDesc spec, CodegenPackageScope packageScope, StatementRawInfo statementRawInfo) {
        this.className = className;
        this.spec = spec;
        this.packageScope = packageScope;
        this.statementRawInfo = statementRawInfo;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        Supplier<String> debugInformationProvider = new Supplier<String>() {
            public String get() {
                StringWriter writer = new StringWriter();
                statementRawInfo.appendCodeDebugInfo(writer);
                writer.append(" result-set-processor ")
                        .append(spec.getResultSetProcessorFactoryForge().getClass().getName());
                return writer.toString();
            }
        };

        try {
            List<CodegenInnerClass> innerClasses = new ArrayList<>();

            // build ctor
            List<CodegenTypedParam> ctorParms = new ArrayList<>();
            ctorParms.add(new CodegenTypedParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef(), false));
            CodegenCtor providerCtor = new CodegenCtor(StmtClassForgeableRSPFactoryProvider.class, includeDebugSymbols, ctorParms);
            CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);
            List<CodegenTypedParam> providerExplicitMembers = new ArrayList<>(2);

            // add event type
            providerExplicitMembers.add(new CodegenTypedParam(EventType.class, MEMBERNAME_RESULTEVENTTYPE));
            providerCtor.getBlock().assignMember(MEMBERNAME_RESULTEVENTTYPE, EventTypeUtility.resolveTypeCodegen(spec.getResultEventType(), EPStatementInitServices.REF));

            makeResultSetProcessorFactory(classScope, innerClasses, providerExplicitMembers, providerCtor, className);

            makeResultSetProcessor(classScope, innerClasses, providerExplicitMembers, providerCtor, className, spec);

            OrderByProcessorCompiler.makeOrderByProcessors(spec.getOrderByProcessorFactoryForge(), classScope, innerClasses, providerExplicitMembers, providerCtor, className, MEMBERNAME_ORDERBYFACTORY);

            providerExplicitMembers.add(new CodegenTypedParam(AggregationServiceFactory.class, MEMBERNAME_AGGREGATIONSVCFACTORY));
            AggregationClassNames aggregationClassNames = new AggregationClassNames();
            AggregationServiceFactoryMakeResult aggResult = AggregationServiceFactoryCompiler.makeInnerClassesAndInit(spec.isJoin(), spec.getAggregationServiceForgeDesc().getAggregationServiceFactoryForge(), providerCtor, classScope, className, aggregationClassNames);
            providerCtor.getBlock().assignMember(MEMBERNAME_AGGREGATIONSVCFACTORY, localMethod(aggResult.getInitMethod(), EPStatementInitServices.REF));
            innerClasses.addAll(aggResult.getInnerClasses());

            makeSelectExprProcessors(classScope, innerClasses, providerExplicitMembers, providerCtor, className, spec.isRollup(), spec.getSelectExprProcessorForges());

            // make provider methods
            CodegenMethod getResultSetProcessorFactoryMethod = CodegenMethod.makeParentNode(ResultSetProcessorFactory.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getResultSetProcessorFactoryMethod.getBlock().methodReturn(ref(MEMBERNAME_RESULTSETPROCESSORFACTORY));

            CodegenMethod getAggregationServiceFactoryMethod = CodegenMethod.makeParentNode(AggregationServiceFactory.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getAggregationServiceFactoryMethod.getBlock().methodReturn(ref(MEMBERNAME_AGGREGATIONSVCFACTORY));

            CodegenMethod getOrderByProcessorFactoryMethod = CodegenMethod.makeParentNode(OrderByProcessorFactory.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getOrderByProcessorFactoryMethod.getBlock().methodReturn(ref(MEMBERNAME_ORDERBYFACTORY));

            CodegenMethod getResultSetProcessorTypeMethod = CodegenMethod.makeParentNode(ResultSetProcessorType.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getResultSetProcessorTypeMethod.getBlock().methodReturn(enumValue(ResultSetProcessorType.class, spec.getResultSetProcessorType().name()));

            CodegenMethod getResultEventTypeMethod = CodegenMethod.makeParentNode(EventType.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getResultEventTypeMethod.getBlock().methodReturn(ref(MEMBERNAME_RESULTEVENTTYPE));

            CodegenClassMethods methods = new CodegenClassMethods();
            CodegenStackGenerator.recursiveBuildStack(providerCtor, "ctor", methods);
            CodegenStackGenerator.recursiveBuildStack(getResultSetProcessorFactoryMethod, "getResultSetProcessorFactory", methods);
            CodegenStackGenerator.recursiveBuildStack(getAggregationServiceFactoryMethod, "getAggregationServiceFactory", methods);
            CodegenStackGenerator.recursiveBuildStack(getOrderByProcessorFactoryMethod, "getOrderByProcessorFactory", methods);
            CodegenStackGenerator.recursiveBuildStack(getResultSetProcessorTypeMethod, "getResultSetProcessorType", methods);
            CodegenStackGenerator.recursiveBuildStack(getResultEventTypeMethod, "getResultEventType", methods);

            // render and compile
            return new CodegenClass(CodegenClassType.RESULTSETPROCESSORFACTORYPROVIDER, ResultSetProcessorFactoryProvider.class, className, classScope, providerExplicitMembers, providerCtor, methods, innerClasses);
        } catch (Throwable t) {
            throw new EPException("Fatal exception during code-generation for " + debugInformationProvider.get() + " : " + t.getMessage(), t);
        }
    }

    private static void makeResultSetProcessorFactory(CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, List<CodegenTypedParam> providerExplicitMembers, CodegenCtor providerCtor, String providerClassName) {
        CodegenMethod instantiateMethod = CodegenMethod.makeParentNode(ResultSetProcessor.class, StmtClassForgeableRSPFactoryProvider.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(OrderByProcessor.class, NAME_ORDERBYPROCESSOR)
                .addParam(AggregationService.class, NAME_AGGREGATIONSVC)
                .addParam(AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT);
        instantiateMethod.getBlock().methodReturn(CodegenExpressionBuilder.newInstance(CLASSNAME_RESULTSETPROCESSOR, ref("o"), MEMBER_ORDERBYPROCESSOR, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT));
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(instantiateMethod, "instantiate", methods);

        List<CodegenTypedParam> ctorParams = Collections.singletonList(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(StmtClassForgeableRSPFactoryProvider.class, classScope, ctorParams);

        CodegenInnerClass innerClass = new CodegenInnerClass(CLASSNAME_RESULTSETPROCESSORFACTORY, ResultSetProcessorFactory.class, ctor, Collections.emptyList(), methods);
        innerClasses.add(innerClass);

        providerExplicitMembers.add(new CodegenTypedParam(ResultSetProcessorFactory.class, "rspFactory"));
        providerCtor.getBlock().assignMember(MEMBERNAME_RESULTSETPROCESSORFACTORY, CodegenExpressionBuilder.newInstance(CLASSNAME_RESULTSETPROCESSORFACTORY, ref("this")));
    }

    private static void makeResultSetProcessor(CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, List<CodegenTypedParam> factoryExplicitMembers, CodegenCtor factoryCtor, String classNameParent, ResultSetProcessorDesc spec) {

        List<CodegenTypedParam> ctorParams = new ArrayList<>();
        ctorParams.add(new CodegenTypedParam(classNameParent, "o"));
        ctorParams.add(new CodegenTypedParam(OrderByProcessor.class, "orderByProcessor"));
        ctorParams.add(new CodegenTypedParam(AggregationService.class, "aggregationService"));
        ctorParams.add(new CodegenTypedParam(AgentInstanceContext.class, "agentInstanceContext"));

        // make ctor code
        CodegenCtor serviceCtor = new CodegenCtor(StmtClassForgeableRSPFactoryProvider.class, classScope, ctorParams);

        // Get-Result-Type Method
        ResultSetProcessorFactoryForge forge = spec.getResultSetProcessorFactoryForge();
        CodegenMethod getResultEventTypeMethod = CodegenMethod.makeParentNode(EventType.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        getResultEventTypeMethod.getBlock().methodReturn(member("o." + MEMBERNAME_RESULTEVENTTYPE));

        // Instance members and methods
        CodegenInstanceAux instance = new CodegenInstanceAux(serviceCtor);
        forge.instanceCodegen(instance, classScope, factoryCtor, factoryExplicitMembers);

        // Process-View-Result Method
        CodegenMethod processViewResultMethod = CodegenMethod.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EventBean[].class, NAME_NEWDATA).addParam(EventBean[].class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!spec.isJoin()) {
            generateInstrumentedProcessView(forge, classScope, processViewResultMethod, instance);
        } else {
            processViewResultMethod.getBlock().methodThrowUnsupported();
        }

        // Process-Join-Result Method
        CodegenMethod processJoinResultMethod = CodegenMethod.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Set.class, NAME_NEWDATA).addParam(Set.class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!spec.isJoin()) {
            processJoinResultMethod.getBlock().methodThrowUnsupported();
        } else {
            generateInstrumentedProcessJoin(forge, classScope, processJoinResultMethod, instance);
        }

        // Clear-Method
        CodegenMethod clearMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        forge.clearMethodCodegen(classScope, clearMethod);

        // Get-Iterator-View
        CodegenMethod getIteratorMethodView = CodegenMethod.makeParentNode(Iterator.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Viewable.class, NAME_VIEWABLE);
        if (!spec.isJoin()) {
            forge.getIteratorViewCodegen(classScope, getIteratorMethodView, instance);
        } else {
            getIteratorMethodView.getBlock().methodThrowUnsupported();
        }

        // Get-Iterator-Join
        CodegenMethod getIteratorMethodJoin = CodegenMethod.makeParentNode(Iterator.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Set.class, NAME_JOINSET);
        if (!spec.isJoin()) {
            getIteratorMethodJoin.getBlock().methodThrowUnsupported();
        } else {
            forge.getIteratorJoinCodegen(classScope, getIteratorMethodJoin, instance);
        }

        // Process-output-rate-buffered-view
        CodegenMethod processOutputLimitedViewMethod = CodegenMethod.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(List.class, NAME_VIEWEVENTSLIST).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!spec.isJoin() && spec.isHasOutputLimit() && !spec.isHasOutputLimitSnapshot()) {
            forge.processOutputLimitedViewCodegen(classScope, processOutputLimitedViewMethod, instance);
        } else {
            processOutputLimitedViewMethod.getBlock().methodThrowUnsupported();
        }

        // Process-output-rate-buffered-join
        CodegenMethod processOutputLimitedJoinMethod = CodegenMethod.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(List.class, NAME_JOINEVENTSSET).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!spec.isJoin() || !spec.isHasOutputLimit() || spec.isHasOutputLimitSnapshot()) {
            processOutputLimitedJoinMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.processOutputLimitedJoinCodegen(classScope, processOutputLimitedJoinMethod, instance);
        }

        // Set-Agent-Instance is supported for fire-and-forget queries only
        CodegenMethod setAgentInstanceContextMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(AgentInstanceContext.class, "context");
        setAgentInstanceContextMethod.getBlock().assignRef(NAME_AGENTINSTANCECONTEXT, ref("context"));

        // Apply-view
        CodegenMethod applyViewResultMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EventBean[].class, NAME_NEWDATA).addParam(EventBean[].class, NAME_OLDDATA);
        if (!spec.isJoin() && spec.isHasOutputLimit() && spec.isHasOutputLimitSnapshot()) {
            forge.applyViewResultCodegen(classScope, applyViewResultMethod, instance);
        } else {
            applyViewResultMethod.getBlock().methodThrowUnsupported();
        }

        // Apply-join
        CodegenMethod applyJoinResultMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Set.class, NAME_NEWDATA).addParam(Set.class, NAME_OLDDATA);
        if (!spec.isJoin() || !spec.isHasOutputLimit() || !spec.isHasOutputLimitSnapshot()) {
            applyJoinResultMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.applyJoinResultCodegen(classScope, applyJoinResultMethod, instance);
        }

        // Process-output-unbuffered-view
        CodegenMethod processOutputLimitedLastAllNonBufferedViewMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EventBean[].class, NAME_NEWDATA).addParam(EventBean[].class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!spec.isJoin() && spec.isHasOutputLimit() && spec.getOutputConditionType() == POLICY_LASTALL_UNORDERED) {
            forge.processOutputLimitedLastAllNonBufferedViewCodegen(classScope, processOutputLimitedLastAllNonBufferedViewMethod, instance);
        } else {
            processOutputLimitedLastAllNonBufferedViewMethod.getBlock().methodThrowUnsupported();
        }

        // Process-output-unbuffered-join
        CodegenMethod processOutputLimitedLastAllNonBufferedJoinMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Set.class, NAME_NEWDATA).addParam(Set.class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!spec.isJoin() || !spec.isHasOutputLimit() || spec.getOutputConditionType() != POLICY_LASTALL_UNORDERED) {
            processOutputLimitedLastAllNonBufferedJoinMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.processOutputLimitedLastAllNonBufferedJoinCodegen(classScope, processOutputLimitedLastAllNonBufferedJoinMethod, instance);
        }

        // Continue-output-unbuffered-view
        CodegenMethod continueOutputLimitedLastAllNonBufferedViewMethod = CodegenMethod.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!spec.isJoin() && spec.isHasOutputLimit() && spec.getOutputConditionType() == POLICY_LASTALL_UNORDERED) {
            forge.continueOutputLimitedLastAllNonBufferedViewCodegen(classScope, continueOutputLimitedLastAllNonBufferedViewMethod, instance);
        } else {
            continueOutputLimitedLastAllNonBufferedViewMethod.getBlock().methodThrowUnsupported();
        }

        // Continue-output-unbuffered-join
        CodegenMethod continueOutputLimitedLastAllNonBufferedJoinMethod = CodegenMethod.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!spec.isJoin() || !spec.isHasOutputLimit() || spec.getOutputConditionType() != POLICY_LASTALL_UNORDERED) {
            continueOutputLimitedLastAllNonBufferedJoinMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.continueOutputLimitedLastAllNonBufferedJoinCodegen(classScope, continueOutputLimitedLastAllNonBufferedJoinMethod, instance);
        }

        // Accept-Helper-Visitor
        CodegenMethod acceptHelperVisitorMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(ResultSetProcessorOutputHelperVisitor.class, NAME_RESULTSETVISITOR);
        forge.acceptHelperVisitorCodegen(classScope, acceptHelperVisitorMethod, instance);

        // Stop-Method (generates last as other methods may allocate members)
        CodegenMethod stopMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
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
        for (Map.Entry<String, CodegenMethod> methodEntry : instance.getMethods().getMethods().entrySet()) {
            CodegenStackGenerator.recursiveBuildStack(methodEntry.getValue(), methodEntry.getKey(), innerMethods);
        }

        CodegenInnerClass innerClass = new CodegenInnerClass(CLASSNAME_RESULTSETPROCESSOR, forge.getInterfaceClass(), serviceCtor, instance.getMembers(), innerMethods);
        innerClasses.add(innerClass);
    }

    private static void generateInstrumentedProcessJoin(ResultSetProcessorFactoryForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (!classScope.isInstrumented()) {
            forge.processJoinResultCodegen(classScope, method, instance);
            return;
        }

        CodegenMethod instrumented = method.makeChild(UniformPair.class, forge.getClass(), classScope)
                .addParam(Set.class, NAME_NEWDATA).addParam(Set.class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        forge.processJoinResultCodegen(classScope, instrumented, instance);

        method.getBlock()
                .apply(InstrumentationCode.instblock(classScope, "q" + forge.getInstrumentedQName()))
                .declareVar(UniformPair.class, "pair", localMethod(instrumented, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE))
                .apply(InstrumentationCode.instblock(classScope, "a" + forge.getInstrumentedQName(), ref("pair")))
                .methodReturn(ref("pair"));
    }

    private static void generateInstrumentedProcessView(ResultSetProcessorFactoryForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        if (!classScope.isInstrumented()) {
            forge.processViewResultCodegen(classScope, method, instance);
            return;
        }

        CodegenMethod instrumented = method.makeChild(UniformPair.class, forge.getClass(), classScope)
                .addParam(EventBean[].class, NAME_NEWDATA).addParam(EventBean[].class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        forge.processViewResultCodegen(classScope, instrumented, instance);

        method.getBlock()
                .apply(InstrumentationCode.instblock(classScope, "q" + forge.getInstrumentedQName()))
                .declareVar(UniformPair.class, "pair", localMethod(instrumented, REF_NEWDATA, REF_OLDDATA, REF_ISSYNTHESIZE))
                .apply(InstrumentationCode.instblock(classScope, "a" + forge.getInstrumentedQName(), ref("pair")))
                .methodReturn(ref("pair"));
    }

    private static void makeSelectExprProcessors(CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, List<CodegenTypedParam> explicitMembers, CodegenCtor outerClassCtor, String classNameParent, boolean rollup, SelectExprProcessorForge[] forges) {
        // handle single-select
        if (!rollup) {
            String name = "SelectExprProcessorImpl";
            explicitMembers.add(new CodegenTypedParam(SelectExprProcessor.class, "selectExprProcessor"));
            outerClassCtor.getBlock().assignRef("selectExprProcessor", CodegenExpressionBuilder.newInstance(name, ref("this"), EPStatementInitServices.REF));
            CodegenInnerClass innerClass = makeSelectExprProcessor(name, classNameParent, classScope, forges[0]);
            innerClasses.add(innerClass);
            return;
        }

        // handle multi-select
        for (int i = 0; i < forges.length; i++) {
            String name = "SelectExprProcessorImpl" + i;
            SelectExprProcessorForge forge = forges[i];
            CodegenInnerClass innerClass = makeSelectExprProcessor(name, classNameParent, classScope, forge);
            innerClasses.add(innerClass);
        }
        explicitMembers.add(new CodegenTypedParam(SelectExprProcessor[].class, "selectExprProcessorArray"));
        outerClassCtor.getBlock().assignRef("selectExprProcessorArray", newArrayByLength(SelectExprProcessor.class, constant(forges.length)));
        for (int i = 0; i < forges.length; i++) {
            outerClassCtor.getBlock().assignArrayElement("selectExprProcessorArray", constant(i), CodegenExpressionBuilder.newInstance("SelectExprProcessorImpl" + i, ref("this"), EPStatementInitServices.REF));
        }
    }

    private static CodegenInnerClass makeSelectExprProcessor(String className, String classNameParent, CodegenClassScope classScope, SelectExprProcessorForge forge) {
        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        SelectExprProcessorCodegenSymbol selectEnv = new SelectExprProcessorCodegenSymbol();
        CodegenSymbolProvider symbolProvider = new CodegenSymbolProvider() {
            public void provide(Map<String, Class> symbols) {
                exprSymbol.provide(symbols);
                selectEnv.provide(symbols);
            }
        };

        List<CodegenTypedParam> members = new ArrayList<>(2);
        members.add(new CodegenTypedParam(EventBeanTypedEventFactory.class, MEMBER_EVENTBEANFACTORY.getRef()));

        List<CodegenTypedParam> ctorParams = new ArrayList<>(2);
        ctorParams.add(new CodegenTypedParam(classNameParent, "o"));
        ctorParams.add(new CodegenTypedParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef(), false));

        CodegenCtor ctor = new CodegenCtor(StmtClassForgeableRSPFactoryProvider.class, classScope, ctorParams);
        ctor.getBlock().assignRef(MEMBER_EVENTBEANFACTORY, exprDotMethod(EPStatementInitServices.REF, "getEventBeanTypedEventFactory"));

        CodegenMethod processMethod = CodegenMethod.makeParentNode(EventBean.class, StmtClassForgeableRSPFactoryProvider.class, symbolProvider, classScope)
                .addParam(EventBean[].class, ExprForgeCodegenNames.NAME_EPS)
                .addParam(boolean.class, ExprForgeCodegenNames.NAME_ISNEWDATA)
                .addParam(boolean.class, SelectExprProcessorCodegenSymbol.NAME_ISSYNTHESIZE)
                .addParam(ExprEvaluatorContext.class, ExprForgeCodegenNames.NAME_EXPREVALCONTEXT);
        processMethod.getBlock().apply(InstrumentationCode.instblock(classScope, "qSelectClause", REF_EPS, REF_ISNEWDATA, REF_ISSYNTHESIZE, REF_EXPREVALCONTEXT));
        CodegenMethod performMethod = forge.processCodegen(member("o." + MEMBERNAME_RESULTEVENTTYPE), MEMBER_EVENTBEANFACTORY, processMethod, selectEnv, exprSymbol, classScope);
        exprSymbol.derivedSymbolsCodegen(processMethod, processMethod.getBlock(), classScope);
        processMethod.getBlock()
                .declareVar(EventBean.class, "out", localMethod(performMethod))
                .apply(InstrumentationCode.instblock(classScope, "aSelectClause", REF_ISNEWDATA, ref("out"), constantNull()))
                .methodReturn(ref("out"));

        CodegenClassMethods allMethods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(processMethod, "process", allMethods);

        return new CodegenInnerClass(className, SelectExprProcessor.class, ctor, members, allMethods);
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.RSPPROVIDER;
    }
}
