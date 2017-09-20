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
import com.espertech.esper.codegen.compile.CodegenCompilerException;
import com.espertech.esper.codegen.compile.CodegenMessageUtil;
import com.espertech.esper.codegen.core.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.util.CodegenStackGenerator;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.orderby.OrderByProcessor;
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
    private final static String INNER_CLASS_NAME = "RSP";

    private static Logger log = LoggerFactory.getLogger(ResultSetProcessorFactoryCompiler.class);

    public static ResultSetProcessorFactory allocate(ResultSetProcessorFactoryForge forge, EventType resultEventType, StatementContext stmtContext, boolean isFireAndForget, boolean join, boolean hasOutputLimit, ResultSetProcessorOutputConditionType outputConditionType, boolean hasOutputLimitSnapshot, SelectExprProcessorForge[] selectExprProcessorForge, boolean rollup) {
        EngineImportService engineImportService = stmtContext.getEngineImportService();

        if (!engineImportService.getCodeGeneration().isEnableResultSet() || isFireAndForget) {
            return forge.getResultSetProcessorFactory(stmtContext, isFireAndForget);
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
            String className = CodeGenerationIDGenerator.generateClassName(ResultSetProcessorFactory.class);

            CodegenClassScope classScope = new CodegenClassScope(engineImportService.getCodeGeneration().isIncludeComments());

            CodegenMethodNode instantiateMethod = CodegenMethodNode.makeParentNode(ResultSetProcessor.class, ResultSetProcessorFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
                    .addParam(OrderByProcessor.class, NAME_ORDERBYPROCESSOR)
                    .addParam(AggregationService.class, NAME_AGGREGATIONSVC)
                    .addParam(AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT);
            instantiateMethod.getBlock().methodReturn(newInstanceInnerClass(INNER_CLASS_NAME, ref("this"), REF_ORDERBYPROCESSOR, REF_AGGREGATIONSVC, REF_AGENTINSTANCECONTEXT));
            CodegenClassMethods methods = new CodegenClassMethods();
            CodegenStackGenerator.recursiveBuildStack(instantiateMethod, "instantiate", methods);

            // ctor instantiates select expression processor
            CodegenCtor factoryCtor = new CodegenCtor(ResultSetProcessorFactoryCompiler.class, classScope, Collections.emptyList());
            List<CodegenNamedParam> factoryExplicitMembers = new ArrayList<>(2);

            List<CodegenInnerClass> innerClasses = new ArrayList<>();
            makeSelectExprProcessors(rollup, innerClasses, factoryCtor, factoryExplicitMembers, className, classScope, selectExprProcessorForge, stmtContext);
            innerClasses.add(makeResultSetProcessor(factoryCtor, factoryExplicitMembers, className, classScope, forge, join, hasOutputLimit, outputConditionType, hasOutputLimitSnapshot, resultEventType));

            CodegenStackGenerator.recursiveBuildStack(factoryCtor, "ctor", methods);

            // render and compile
            CodegenClass clazz = new CodegenClass(engineImportService.getEngineURI(), ResultSetProcessorFactory.class, className, classScope, factoryExplicitMembers, factoryCtor, methods, innerClasses);
            return CodegenClassGenerator.compile(clazz, engineImportService, ResultSetProcessorFactory.class, debugInformationProvider);
        } catch (CodegenCompilerException ex) {
            boolean fallback = engineImportService.getCodeGeneration().isEnableFallback();
            String message = CodegenMessageUtil.getFailedCompileLogMessageWithCode(ex, debugInformationProvider, fallback);
            if (fallback) {
                log.warn(message, ex);
            } else {
                log.error(message, ex);
            }
            return handleThrowable(stmtContext, ex, forge, debugInformationProvider, isFireAndForget, stmtContext.getStatementName());
        } catch (Throwable t) {
            return handleThrowable(stmtContext, t, forge, debugInformationProvider, isFireAndForget, stmtContext.getStatementName());
        }
    }

    private static CodegenInnerClass makeResultSetProcessor(CodegenCtor factoryCtor, List<CodegenNamedParam> factoryExplicitMembers, String classNameParent, CodegenClassScope classScope, ResultSetProcessorFactoryForge forge, boolean join, boolean hasOutputLimit, ResultSetProcessorOutputConditionType outputConditionType, boolean hasOutputLimitSnapshot, EventType resultEventType) {
        // Get-Result-Type Method
        CodegenMethodNode getResultEventTypeMethod = CodegenMethodNode.makeParentNode(EventType.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        CodegenMember memberResultType = classScope.makeAddMember(EventType.class, resultEventType);
        getResultEventTypeMethod.getBlock().methodReturn(member(memberResultType.getMemberId()));

        // Instance members and methods
        ResultSetProcessorCodegenInstance instance = new ResultSetProcessorCodegenInstance(factoryCtor, factoryExplicitMembers);
        forge.instanceCodegen(instance, classScope);

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

        // Stop-Method
        CodegenMethodNode stopMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        forge.stopMethodCodegen(classScope, stopMethod, instance);

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

        CodegenMethodNode processOutputLimitedViewMethod = CodegenMethodNode.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(List.class, NAME_VIEWEVENTSLIST).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join && hasOutputLimit && !hasOutputLimitSnapshot) {
            forge.processOutputLimitedViewCodegen(classScope, processOutputLimitedViewMethod, instance);
        } else {
            processOutputLimitedViewMethod.getBlock().methodThrowUnsupported();
        }

        CodegenMethodNode processOutputLimitedJoinMethod = CodegenMethodNode.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(List.class, NAME_JOINEVENTSSET).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join || !hasOutputLimit || hasOutputLimitSnapshot) {
            processOutputLimitedJoinMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.processOutputLimitedJoinCodegen(classScope, processOutputLimitedJoinMethod, instance);
        }

        CodegenMethodNode setAgentInstanceContextMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(AgentInstanceContext.class, "context");
        // not supported as used only for fire-and-forget queries
        setAgentInstanceContextMethod.getBlock().methodThrowUnsupported();

        CodegenMethodNode applyViewResultMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EventBean[].class, NAME_NEWDATA).addParam(EventBean[].class, NAME_OLDDATA);
        if (!join && hasOutputLimit && hasOutputLimitSnapshot) {
            forge.applyViewResultCodegen(classScope, applyViewResultMethod, instance);
        } else {
            applyViewResultMethod.getBlock().methodThrowUnsupported();
        }

        CodegenMethodNode applyJoinResultMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Set.class, NAME_NEWDATA).addParam(Set.class, NAME_OLDDATA);
        if (!join || !hasOutputLimit || !hasOutputLimitSnapshot) {
            applyJoinResultMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.applyJoinResultCodegen(classScope, applyJoinResultMethod, instance);
        }

        CodegenMethodNode processOutputLimitedLastAllNonBufferedViewMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(EventBean[].class, NAME_NEWDATA).addParam(EventBean[].class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join && hasOutputLimit && outputConditionType == POLICY_LASTALL_UNORDERED) {
            forge.processOutputLimitedLastAllNonBufferedViewCodegen(classScope, processOutputLimitedLastAllNonBufferedViewMethod, instance);
        } else {
            processOutputLimitedLastAllNonBufferedViewMethod.getBlock().methodThrowUnsupported();
        }

        CodegenMethodNode processOutputLimitedLastAllNonBufferedJoinMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(Set.class, NAME_NEWDATA).addParam(Set.class, NAME_OLDDATA).addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join || !hasOutputLimit || outputConditionType != POLICY_LASTALL_UNORDERED) {
            processOutputLimitedLastAllNonBufferedJoinMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.processOutputLimitedLastAllNonBufferedJoinCodegen(classScope, processOutputLimitedLastAllNonBufferedJoinMethod, instance);
        }

        CodegenMethodNode continueOutputLimitedLastAllNonBufferedViewMethod = CodegenMethodNode.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join && hasOutputLimit && outputConditionType == POLICY_LASTALL_UNORDERED) {
            forge.continueOutputLimitedLastAllNonBufferedViewCodegen(classScope, continueOutputLimitedLastAllNonBufferedViewMethod, instance);
        } else {
            continueOutputLimitedLastAllNonBufferedViewMethod.getBlock().methodThrowUnsupported();
        }

        CodegenMethodNode continueOutputLimitedLastAllNonBufferedJoinMethod = CodegenMethodNode.makeParentNode(UniformPair.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(boolean.class, NAME_ISSYNTHESIZE);
        if (!join || !hasOutputLimit || outputConditionType != POLICY_LASTALL_UNORDERED) {
            continueOutputLimitedLastAllNonBufferedJoinMethod.getBlock().methodThrowUnsupported();
        } else {
            forge.continueOutputLimitedLastAllNonBufferedJoinCodegen(classScope, continueOutputLimitedLastAllNonBufferedJoinMethod, instance);
        }

        CodegenMethodNode acceptHelperVisitorMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(ResultSetProcessorOutputHelperVisitor.class, NAME_VISITOR);
        forge.acceptHelperVisitorCodegen(classScope, acceptHelperVisitorMethod, instance);

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
        for (Map.Entry<String, CodegenMethodNode> methodEntry : instance.getMethods().entrySet()) {
            CodegenStackGenerator.recursiveBuildStack(methodEntry.getValue(), methodEntry.getKey(), innerMethods);
        }

        // compile explicit members
        List<CodegenNamedParam> explicitMembers = instance.getMembers().isEmpty() ? Collections.emptyList() : new ArrayList<>(instance.getMembers().size());
        for (ResultSetProcessorMemberEntry member : instance.getMembers()) {
            explicitMembers.add(new CodegenNamedParam(member.getClazz(), member.getName()));
        }

        List<CodegenCtorParam> ctorParams = new ArrayList<>();
        ctorParams.add(new CodegenCtorParam(classNameParent, "o"));
        ctorParams.add(new CodegenCtorParam(OrderByProcessor.class, "orderByProcessor"));
        ctorParams.add(new CodegenCtorParam(AggregationService.class, "aggregationService"));
        ctorParams.add(new CodegenCtorParam(AgentInstanceContext.class, "agentInstanceContext"));

        // make ctor code
        CodegenCtor ctor = new CodegenCtor(ResultSetProcessorFactoryCompiler.class, classScope, ctorParams);
        for (ResultSetProcessorMemberEntry member : instance.getMembers()) {
            ctor.getBlock().assignRef(member.getName(), member.getInitializer());
        }
        for (CodegenExpression ctorCode : instance.getCtorExpressions()) {
            ctor.getBlock().expression(ctorCode);
        }

        return new CodegenInnerClass(INNER_CLASS_NAME, forge.getInterfaceClass(), ctor, explicitMembers, Collections.emptyMap(), innerMethods);
    }

    private static void makeSelectExprProcessors(boolean rollup, List<CodegenInnerClass> innerClasses, CodegenCtor outerClassCtor, List<CodegenNamedParam> explicitMembers, String classNameParent, CodegenClassScope classScope, SelectExprProcessorForge[] forges, StatementContext stmtContext) {
        // handle single-select
        if (!rollup) {
            String name = "SelectExprProcessorImpl";
            explicitMembers.add(new CodegenNamedParam(SelectExprProcessor.class, "selectExprProcessor"));
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
        explicitMembers.add(new CodegenNamedParam(SelectExprProcessor[].class, "selectExprProcessorArray"));
        outerClassCtor.getBlock().assignRef("selectExprProcessorArray", newArrayByLength(SelectExprProcessor.class, constant(forges.length)));
        for (int i = 0; i < forges.length; i++) {
            outerClassCtor.getBlock().assignArrayElement("selectExprProcessorArray", constant(i), newInstanceInnerClass("SelectExprProcessorImpl" + i, ref("this")));
        }
    }

    private static CodegenInnerClass makeSelectExprProcessor(String className, String classNameParent, CodegenClassScope classScope, SelectExprProcessorForge forge, StatementContext stmtContext) {
        SelectExprProcessorCompilerResult selectClassCode = SelectExprProcessorCompiler.generate(classScope, forge, stmtContext.getEngineImportService(), stmtContext.getEventAdapterService());
        CodegenClassMethods selectClassMethods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(selectClassCode.getTopNode(), "process", selectClassMethods);
        List<CodegenCtorParam> selectExprCtorParams = Collections.singletonList(new CodegenCtorParam(classNameParent, "o"));
        CodegenCtor selectExprCtor = new CodegenCtor(ResultSetProcessorFactoryCompiler.class, classScope, selectExprCtorParams);
        return new CodegenInnerClass(className, SelectExprProcessor.class, selectExprCtor, Collections.emptyList(), Collections.emptyMap(), selectClassMethods);
    }

    private static ResultSetProcessorFactory handleThrowable(StatementContext statementContext, Throwable t, ResultSetProcessorFactoryForge forge, Supplier<String> debugInformationProvider, boolean isFireAndForget, String statementName) {
        if (statementContext.getEngineImportService().getCodeGeneration().isEnableFallback()) {
            return forge.getResultSetProcessorFactory(statementContext, isFireAndForget);
        }
        throw new EPException("Fatal exception during code-generation for " + debugInformationProvider.get() + " (see error log for further details): " + t.getMessage(), t);
    }
}
