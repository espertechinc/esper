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
package com.espertech.esper.epl.agg.codegen;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.compile.CodegenClassGenerator;
import com.espertech.esper.codegen.compile.CodegenCompilerException;
import com.espertech.esper.codegen.compile.CodegenMessageUtil;
import com.espertech.esper.codegen.core.*;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRefWCol;
import com.espertech.esper.codegen.util.CodegenStackGenerator;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAccessorForgeGetCodegenContext;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPairForge;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorFactoryFactory;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionCodegenType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;
import java.util.function.Supplier;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.agg.codegen.AggregationRowCodegenUtil.classnameForLevel;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.NAME_AGENTINSTANCECONTEXT;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.NAME_ISNEWDATA;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.*;

public class AggregationServiceFactoryCompiler {

    private final static Logger log = LoggerFactory.getLogger(AggregationServiceFactoryCompiler.class);

    private final static String MEMBERNAME_AGGREGATIONSVCFACTORY = "aggFactory";
    private final static List<CodegenNamedParam> UPDPARAMS = CodegenNamedParam.from(EventBean[].class, NAME_EPS, ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
    private final static List<CodegenNamedParam> GETPARAMS = CodegenNamedParam.from(int.class, AggregationServiceCodegenNames.NAME_COLUMN, EventBean[].class, NAME_EPS, boolean.class, ExprForgeCodegenNames.NAME_ISNEWDATA, ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
    private final static List<CodegenNamedParam> MAKESERVICEPARAMS = CodegenNamedParam.from(AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT, EngineImportService.class, AggregationServiceCodegenNames.NAME_ENGINEIMPORTSVC, boolean.class, AggregationServiceCodegenNames.NAME_ISSUBQUERY, Integer.class, AggregationServiceCodegenNames.NAME_SUBQUERYNUMBER);

    public static AggregationServiceFactory allocate(AggregationServiceFactoryForge forge, StatementContext stmtContext, boolean isFireAndForget) {
        EngineImportService engineImportService = stmtContext.getEngineImportService();

        if (!engineImportService.getByteCodeGeneration().isEnableAggregation() || isFireAndForget) {
            return forge.getAggregationServiceFactory(stmtContext, isFireAndForget);
        }

        Supplier<String> debugInformationProvider = new Supplier<String>() {
            public String get() {
                StringWriter writer = new StringWriter();
                writer.append("statement '")
                        .append(stmtContext.getStatementName())
                        .append("' aggregation-service");
                writer.append(" requestor-class '")
                        .append(ResultSetProcessorFactoryFactory.class.getSimpleName())
                        .append("'");
                return writer.toString();
            }
        };

        try {
            CodegenClassScope classScope = new CodegenClassScope(engineImportService.getByteCodeGeneration().isIncludeComments());

            List<CodegenInnerClass> innerClasses = new ArrayList<>();
            String providerClassName = CodeGenerationIDGenerator.generateClassName(AggregationServiceFactoryProvider.class);
            AggregationServiceFactoryCompiler.makeInnerClasses(forge, classScope, innerClasses, providerClassName, stmtContext, isFireAndForget);

            CodegenMethodNode getAggregationServiceFactoryMethod = CodegenMethodNode.makeParentNode(AggregationServiceFactory.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            getAggregationServiceFactoryMethod.getBlock().methodReturn(ref(MEMBERNAME_AGGREGATIONSVCFACTORY));

            CodegenClassMethods methods = new CodegenClassMethods();
            CodegenStackGenerator.recursiveBuildStack(getAggregationServiceFactoryMethod, "getAggregationServiceFactory", methods);

            CodegenCtor providerCtor = new CodegenCtor(AggregationServiceFactoryCompiler.class, classScope, Collections.emptyList());
            List<CodegenTypedParam> providerExplicitMembers = new ArrayList<>(2);
            providerExplicitMembers.add(new CodegenTypedParam(AggregationServiceFactory.class, MEMBERNAME_AGGREGATIONSVCFACTORY));
            providerCtor.getBlock().assignRef(MEMBERNAME_AGGREGATIONSVCFACTORY, newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONSERVICEFACTORY, ref("this")));

            CodegenClass clazz = new CodegenClass(AggregationServiceFactoryProvider.class, engineImportService.getCodegenCompiler().getPackageName(), providerClassName, classScope, providerExplicitMembers, providerCtor, methods, innerClasses);
            AggregationServiceFactoryProvider provider = CodegenClassGenerator.compile(clazz, engineImportService, AggregationServiceFactoryProvider.class, debugInformationProvider);
            return provider.getAggregationServiceFactory();
        } catch (CodegenCompilerException ex) {
            boolean fallback = engineImportService.getByteCodeGeneration().isEnableFallback();
            String message = CodegenMessageUtil.getFailedCompileLogMessageWithCode(ex, debugInformationProvider, fallback);
            if (fallback) {
                log.warn(message, ex);
            } else {
                log.error(message, ex);
            }
            return handleThrowable(stmtContext, ex, forge, debugInformationProvider, isFireAndForget);
        } catch (Throwable t) {
            return handleThrowable(stmtContext, t, forge, debugInformationProvider, isFireAndForget);
        }
    }

    public static void makeInnerClasses(AggregationServiceFactoryForge forge, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, String providerClassName, StatementContext statementContext, boolean isFireAndForget) {
        makeRow(forge, classScope, innerClasses, providerClassName, statementContext, isFireAndForget);

        makeFactory(forge, classScope, innerClasses, providerClassName);

        makeService(forge, innerClasses, classScope, providerClassName);
    }

    private static void makeRow(AggregationServiceFactoryForge forge, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, String providerClassName, StatementContext statementContext, boolean isFireAndForget) {

        AggregationCodegenRowLevelDesc levels = forge.getRowLevelDesc();

        if (levels.getOptionalTopRow() != null) {
            makeRowForLevel(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, levels.getOptionalTopRow(), forge, classScope, innerClasses, providerClassName, statementContext, isFireAndForget);
        }

        if (levels.getOptionalAdditionalRows() != null) {
            for (int i = 0; i < levels.getOptionalAdditionalRows().length; i++) {
                String className = classnameForLevel(i);
                makeRowForLevel(className, levels.getOptionalAdditionalRows()[i], forge, classScope, innerClasses, providerClassName, statementContext, isFireAndForget);
            }
        }
    }

    private static void makeRowForLevel(String className, AggregationCodegenRowDetailDesc detail, AggregationServiceFactoryForge forge, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, String providerClassName, StatementContext statementContext, boolean isFireAndForget) {
        ExprForge[][] methodForges = detail.getStateDesc().getMethodForges();
        AggregationMethodFactory[] methodFactories = detail.getStateDesc().getMethodFactories();
        AggregationStateFactoryForge[] accessFactories = detail.getStateDesc().getAccessStateForges();
        AggregationAccessorSlotPairForge[] accessAccessors = detail.getAccessAccessors();

        // make member+ctor
        List<CodegenTypedParam> rowCtorParams = new ArrayList<>();
        rowCtorParams.add(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor rowCtor = new CodegenCtor(forge.getClass(), classScope, rowCtorParams);
        CodegenNamedMethods namedMethods = new CodegenNamedMethods();
        List<CodegenTypedParam> rowMembers = new ArrayList<>();
        forge.rowCtorCodegen(classScope, rowCtor, rowMembers, namedMethods);
        CodegenMembersColumnized membersColumnized = makeRowCtor(rowCtor, classScope, methodFactories, accessFactories, methodForges);

        // make state-update
        CodegenMethodNode applyEnterMethod = makeStateUpdate(AggregationCodegenUpdateType.APPLYENTER, methodForges, methodFactories, accessFactories, classScope, namedMethods);
        CodegenMethodNode applyLeaveMethod = makeStateUpdate(AggregationCodegenUpdateType.APPLYLEAVE, methodForges, methodFactories, accessFactories, classScope, namedMethods);
        CodegenMethodNode clearMethod = makeStateUpdate(AggregationCodegenUpdateType.CLEAR, methodForges, methodFactories, accessFactories, classScope, namedMethods);

        // make getters
        CodegenMethodNode getValueMethod = makeGet(AggregationCodegenGetType.GETVALUE, methodFactories, accessAccessors, accessFactories, classScope, namedMethods, statementContext, isFireAndForget);
        CodegenMethodNode getEventBeanMethod = makeGet(AggregationCodegenGetType.GETEVENTBEAN, methodFactories, accessAccessors, accessFactories, classScope, namedMethods, statementContext, isFireAndForget);
        CodegenMethodNode getCollectionScalarMethod = makeGet(AggregationCodegenGetType.GETCOLLECTIONSCALAR, methodFactories, accessAccessors, accessFactories, classScope, namedMethods, statementContext, isFireAndForget);
        CodegenMethodNode getCollectionOfEventsMethod = makeGet(AggregationCodegenGetType.GETCOLLECTIONOFEVENTS, methodFactories, accessAccessors, accessFactories, classScope, namedMethods, statementContext, isFireAndForget);

        CodegenClassMethods innerMethods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(applyEnterMethod, "applyEnter", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(applyLeaveMethod, "applyLeave", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(clearMethod, "clear", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getValueMethod, "getValue", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getEventBeanMethod, "getEventBean", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getCollectionScalarMethod, "getCollectionScalar", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getCollectionOfEventsMethod, "getCollectionOfEvents", innerMethods);
        for (Map.Entry<String, CodegenMethodNode> methodEntry : namedMethods.getMethods().entrySet()) {
            CodegenStackGenerator.recursiveBuildStack(methodEntry.getValue(), methodEntry.getKey(), innerMethods);
        }

        for (Map.Entry<CodegenExpressionRefWCol, Class> entry : membersColumnized.getMembers().entrySet()) {
            rowMembers.add(new CodegenTypedParam(entry.getValue(), entry.getKey().toName()));
        }

        CodegenInnerClass innerClass = new CodegenInnerClass(className, AggregationRowGenerated.class, rowCtor, rowMembers, Collections.emptyMap(), innerMethods);
        innerClasses.add(innerClass);
    }

    private static CodegenMethodNode makeGet(AggregationCodegenGetType getType, AggregationMethodFactory[] methodFactories, AggregationAccessorSlotPairForge[] accessAccessors, AggregationStateFactoryForge[] accessFactories, CodegenClassScope classScope, CodegenNamedMethods namedMethods, StatementContext statementContext, boolean isFireAndForget) {
        CodegenMethodNode parent = CodegenMethodNode.makeParentNode(getType.getReturnType(), AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(GETPARAMS);

        // for non-get-value we can simply return null if this has no access aggs
        if (getType != AggregationCodegenGetType.GETVALUE && accessFactories == null) {
            parent.getBlock().methodReturn(constantNull());
            return parent;
        }

        List<CodegenMethodNode> methods = new ArrayList<>();

        int count = 0;
        int numMethodStates = 0;
        if (methodFactories != null) {
            for (AggregationMethodFactory factory : methodFactories) {
                CodegenMethodNode method = parent.makeChild(getType.getReturnType(), factory.getClass(), classScope).addParam(CodegenNamedParam.from(EventBean[].class, NAME_EPS, boolean.class, ExprForgeCodegenNames.NAME_ISNEWDATA, ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT));
                methods.add(method);

                if (getType == AggregationCodegenGetType.GETVALUE) {
                    factory.getValueCodegen(count, method, classScope);
                } else {
                    method.getBlock().methodReturn(constantNull()); // method aggs don't do others
                }
                count++;
                numMethodStates++;
            }
        }

        if (accessAccessors != null) {
            for (AggregationAccessorSlotPairForge accessorSlotPair : accessAccessors) {
                CodegenMethodNode method = parent.makeChild(getType.getReturnType(), accessorSlotPair.getAccessorForge().getClass(), classScope).addParam(CodegenNamedParam.from(EventBean[].class, NAME_EPS, boolean.class, ExprForgeCodegenNames.NAME_ISNEWDATA, ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT));
                int stateNumber = numMethodStates + accessorSlotPair.getSlot();

                if (accessorSlotPair.getAccessorForge().getPluginCodegenType() == PlugInAggregationMultiFunctionCodegenType.CODEGEN_NONE) {
                    AggregationAccessor accessor = accessorSlotPair.getAccessorForge().getAccessor(statementContext.getEngineImportService(), isFireAndForget, statementContext.getStatementName());
                    CodegenMember member = classScope.makeAddMember(AggregationAccessor.class, accessor);
                    method.getBlock().methodReturn(exprDotMethod(member(member.getMemberId()), getType.getAccessorMethodName(), refCol("state", stateNumber), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
                } else {
                    AggregationAccessorForgeGetCodegenContext ctx = new AggregationAccessorForgeGetCodegenContext(stateNumber, classScope, accessFactories[accessorSlotPair.getSlot()], method, namedMethods);
                    switch (getType) {
                        case GETVALUE:
                            accessorSlotPair.getAccessorForge().getValueCodegen(ctx);
                            break;
                        case GETEVENTBEAN:
                            accessorSlotPair.getAccessorForge().getEnumerableEventCodegen(ctx);
                            break;
                        case GETCOLLECTIONSCALAR:
                            accessorSlotPair.getAccessorForge().getEnumerableScalarCodegen(ctx);
                            break;
                        case GETCOLLECTIONOFEVENTS:
                            accessorSlotPair.getAccessorForge().getEnumerableEventsCodegen(ctx);
                            break;
                    }
                }
                methods.add(method);
                count++;
            }
        }

        CodegenBlock[] blocks = parent.getBlock().switchBlockOfLength("column", count, true);
        count = 0;
        for (CodegenMethodNode getValue : methods) {
            blocks[count++].blockReturn(localMethod(getValue, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
        }

        return parent;
    }

    private static CodegenMethodNode makeStateUpdate(AggregationCodegenUpdateType updateType, ExprForge[][] methodForges, AggregationMethodFactory[] methodFactories, AggregationStateFactoryForge[] accessFactories, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        ExprForgeCodegenSymbol symbols = new ExprForgeCodegenSymbol(true, updateType == AggregationCodegenUpdateType.APPLYENTER);
        CodegenMethodNode parent = CodegenMethodNode.makeParentNode(void.class, AggregationServiceFactoryCompiler.class, symbols, classScope).addParam(updateType.getParams());

        int count = 0;
        List<CodegenMethodNode> methods = new ArrayList<>();

        if (methodFactories != null) {
            for (AggregationMethodFactory factory : methodFactories) {
                CodegenMethodNode method = parent.makeChild(void.class, factory.getClass(), classScope);
                methods.add(method);
                switch (updateType) {
                    case APPLYENTER:
                        factory.applyEnterCodegen(count, method, symbols, methodForges[count], classScope);
                        break;
                    case APPLYLEAVE:
                        factory.applyLeaveCodegen(count, method, symbols, methodForges[count], classScope);
                        break;
                    case CLEAR:
                        factory.clearCodegen(count, method, classScope);
                        break;
                }
                count++;
            }
        }

        if (accessFactories != null) {
            for (AggregationStateFactoryForge factory : accessFactories) {
                CodegenMethodNode method = parent.makeChild(void.class, factory.getClass(), classScope);
                methods.add(method);
                switch (updateType) {
                    case APPLYENTER:
                        factory.applyEnterCodegen(count, method, symbols, classScope, namedMethods);
                        break;
                    case APPLYLEAVE:
                        factory.applyLeaveCodegen(count, method, symbols, classScope, namedMethods);
                        break;
                    case CLEAR:
                        factory.clearCodegen(count, method, classScope, namedMethods);
                        break;
                }
                count++;
            }
        }

        // code for enter
        symbols.derivedSymbolsCodegen(parent, parent.getBlock(), classScope);
        for (CodegenMethodNode method : methods) {
            parent.getBlock().localMethod(method);
        }
        return parent;
    }

    private static CodegenMembersColumnized makeRowCtor(CodegenCtor rowCtor, CodegenClassScope classScope, AggregationMethodFactory[] methodFactories, AggregationStateFactoryForge[] accessFactories, ExprForge[][] methodForges) {
        CodegenMembersColumnized membersColumnized = new CodegenMembersColumnized();
        int count = 0;
        if (methodFactories != null) {
            for (AggregationMethodFactory factory : methodFactories) {
                factory.rowMemberCodegen(count, rowCtor, membersColumnized, methodForges[count], classScope);
                count++;
            }
        }
        if (accessFactories != null) {
            for (AggregationStateFactoryForge factory : accessFactories) {
                factory.rowMemberCodegen(count, rowCtor, membersColumnized, classScope);
                count++;
            }
        }
        return membersColumnized;
    }

    private static void makeService(AggregationServiceFactoryForge forge, List<CodegenInnerClass> innerClasses, CodegenClassScope classScope, String providerClassName) {
        CodegenNamedMethods namedMethods = new CodegenNamedMethods();

        CodegenMethodNode applyEnterMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean[].class, NAME_EPS).addParam(Object.class, AggregationServiceCodegenNames.NAME_GROUPKEY).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.applyEnterCodegen(applyEnterMethod, classScope, namedMethods);

        CodegenMethodNode applyLeaveMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean[].class, NAME_EPS).addParam(Object.class, AggregationServiceCodegenNames.NAME_GROUPKEY).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.applyLeaveCodegen(applyLeaveMethod, classScope, namedMethods);

        CodegenMethodNode setCurrentAccessMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(Object.class, AggregationServiceCodegenNames.NAME_GROUPKEY).addParam(int.class, AggregationServiceCodegenNames.NAME_AGENTINSTANCEID).addParam(AggregationGroupByRollupLevel.class, AggregationServiceCodegenNames.NAME_ROLLUPLEVEL);
        forge.setCurrentAccessCodegen(setCurrentAccessMethod, classScope);

        CodegenMethodNode clearResultsMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.clearResultsCodegen(clearResultsMethod, classScope);

        CodegenMethodNode setRemovedCallbackMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AggregationRowRemovedCallback.class, AggregationServiceCodegenNames.NAME_CALLBACK);
        forge.setRemovedCallbackCodegen(setRemovedCallbackMethod);

        CodegenMethodNode acceptMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AggregationServiceVisitor.class, AggregationServiceCodegenNames.NAME_AGGVISITOR);
        forge.acceptCodegen(acceptMethod, classScope);

        CodegenMethodNode acceptGroupDetailMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AggregationServiceVisitorWGroupDetail.class, AggregationServiceCodegenNames.NAME_AGGVISITOR);
        forge.acceptGroupDetailCodegen(acceptGroupDetailMethod, classScope);

        CodegenMethodNode isGroupedMethod = CodegenMethodNode.makeParentNode(boolean.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        forge.isGroupedCodegen(isGroupedMethod, classScope);

        CodegenMethodNode getContextPartitionAggregationServiceMethod = CodegenMethodNode.makeParentNode(AggregationService.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_AGENTINSTANCEID);
        getContextPartitionAggregationServiceMethod.getBlock().methodReturn(ref("this"));

        CodegenMethodNode getValueMethod = CodegenMethodNode.makeParentNode(Object.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_COLUMN).addParam(int.class, AggregationServiceCodegenNames.NAME_AGENTINSTANCEID).addParam(EventBean[].class, NAME_EPS).addParam(boolean.class, NAME_ISNEWDATA).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.getValueCodegen(getValueMethod, classScope, namedMethods);

        CodegenMethodNode getCollectionOfEventsMethod = CodegenMethodNode.makeParentNode(Collection.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_COLUMN).addParam(EventBean[].class, NAME_EPS).addParam(boolean.class, NAME_ISNEWDATA).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.getCollectionOfEventsCodegen(getCollectionOfEventsMethod, classScope, namedMethods);

        CodegenMethodNode getEventBeanMethod = CodegenMethodNode.makeParentNode(EventBean.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_COLUMN).addParam(EventBean[].class, NAME_EPS).addParam(boolean.class, NAME_ISNEWDATA).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.getEventBeanCodegen(getEventBeanMethod, classScope, namedMethods);

        CodegenMethodNode getGroupKeyMethod = CodegenMethodNode.makeParentNode(Object.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_AGENTINSTANCEID);
        forge.getGroupKeyCodegen(getGroupKeyMethod, classScope);

        CodegenMethodNode getGroupKeysMethod = CodegenMethodNode.makeParentNode(Collection.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.getGroupKeysCodegen(getGroupKeysMethod, classScope);

        CodegenMethodNode getCollectionScalarMethod = CodegenMethodNode.makeParentNode(Collection.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_COLUMN).addParam(EventBean[].class, NAME_EPS).addParam(boolean.class, NAME_ISNEWDATA).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.getCollectionScalarCodegen(getCollectionScalarMethod, classScope, namedMethods);

        CodegenMethodNode stopMethod = CodegenMethodNode.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        forge.stopMethodCodegen(forge, stopMethod);

        List<CodegenTypedParam> members = new ArrayList<>();
        List<CodegenTypedParam> ctorParams = new ArrayList<>();
        ctorParams.add(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(AggregationServiceFactoryCompiler.class, classScope, ctorParams);
        forge.ctorCodegen(ctor, members, classScope);

        CodegenClassMethods innerMethods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(applyEnterMethod, "applyEnter", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(applyLeaveMethod, "applyLeave", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(setCurrentAccessMethod, "setCurrentAccess", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(clearResultsMethod, "clearResults", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(setRemovedCallbackMethod, "setRemovedCallback", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(acceptMethod, "accept", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(acceptGroupDetailMethod, "acceptGroupDetail", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(isGroupedMethod, "isGrouped", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getContextPartitionAggregationServiceMethod, "getContextPartitionAggregationService", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getValueMethod, "getValue", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getCollectionOfEventsMethod, "getCollectionOfEvents", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getEventBeanMethod, "getEventBean", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getGroupKeyMethod, "getGroupKey", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getGroupKeysMethod, "getGroupKeys", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getCollectionScalarMethod, "getCollectionScalar", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(stopMethod, "stop", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(ctor, "ctor", innerMethods);
        for (Map.Entry<String, CodegenMethodNode> methodEntry : namedMethods.getMethods().entrySet()) {
            CodegenStackGenerator.recursiveBuildStack(methodEntry.getValue(), methodEntry.getKey(), innerMethods);
        }

        CodegenInnerClass innerClass = new CodegenInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONSERVICE, AggregationService.class, ctor, members, Collections.emptyMap(), innerMethods);
        innerClasses.add(innerClass);
    }

    private static AggregationServiceFactory handleThrowable(StatementContext statementContext, Throwable t, AggregationServiceFactoryForge forge, Supplier<String> debugInformationProvider, boolean isFireAndForget) {
        if (statementContext.getEngineImportService().getByteCodeGeneration().isEnableFallback()) {
            return forge.getAggregationServiceFactory(statementContext, isFireAndForget);
        }
        throw new EPException("Fatal exception during code-generation for " + debugInformationProvider.get() + " (see error log for further details): " + t.getMessage(), t);
    }

    private static void makeFactory(AggregationServiceFactoryForge forge, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, String providerClassName) {
        CodegenMethodNode makeServiceMethod = CodegenMethodNode.makeParentNode(AggregationService.class, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(MAKESERVICEPARAMS);
        forge.makeServiceCodegen(makeServiceMethod, classScope);

        List<CodegenTypedParam> ctorParams = Collections.singletonList(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(AggregationServiceFactoryCompiler.class, classScope, ctorParams);

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(makeServiceMethod, "makeService", methods);
        CodegenInnerClass innerClass = new CodegenInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONSERVICEFACTORY, AggregationServiceFactory.class, ctor, Collections.emptyList(), Collections.emptyMap(), methods);
        innerClasses.add(innerClass);
    }

    private enum AggregationCodegenUpdateType {

        APPLYENTER(UPDPARAMS),
        APPLYLEAVE(UPDPARAMS),
        CLEAR(Collections.emptyList());

        private final List<CodegenNamedParam> params;

        AggregationCodegenUpdateType(List<CodegenNamedParam> params) {
            this.params = params;
        }

        public List<CodegenNamedParam> getParams() {
            return params;
        }
    }

    private enum AggregationCodegenGetType {

        GETVALUE("getValue", Object.class),
        GETEVENTBEAN("getEnumerableEvent", EventBean.class),
        GETCOLLECTIONSCALAR("getEnumerableScalar", Collection.class),
        GETCOLLECTIONOFEVENTS("getEnumerableEvents", Collection.class);

        private final String accessorMethodName;
        private final Class returnType;

        AggregationCodegenGetType(String accessorMethodName, Class returnType) {
            this.accessorMethodName = accessorMethodName;
            this.returnType = returnType;
        }

        public Class getReturnType() {
            return returnType;
        }

        public String getAccessorMethodName() {
            return accessorMethodName;
        }
    }
}
