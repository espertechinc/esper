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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMemberWCol;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceCodegenNames.NAME_GROUPID;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.NAME_AGENTINSTANCECONTEXT;
import static com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode.instblock;

public class AggregationServiceFactoryCompiler {

    private final static List<CodegenNamedParam> UPDPARAMS = CodegenNamedParam.from(EventBean[].class, NAME_EPS, ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
    private final static List<CodegenNamedParam> GETPARAMS = CodegenNamedParam.from(int.class, AggregationServiceCodegenNames.NAME_COLUMN, EventBean[].class, NAME_EPS, boolean.class, ExprForgeCodegenNames.NAME_ISNEWDATA, ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
    private final static List<CodegenNamedParam> MAKESERVICEPARAMS = CodegenNamedParam.from(AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT, ClasspathImportServiceRuntime.class, AggregationServiceCodegenNames.NAME_ENGINEIMPORTSVC, boolean.class, AggregationServiceCodegenNames.NAME_ISSUBQUERY, Integer.class, AggregationServiceCodegenNames.NAME_SUBQUERYNUMBER, int[].class, NAME_GROUPID);

    public static AggregationServiceFactoryMakeResult makeInnerClassesAndInit(boolean join, AggregationServiceFactoryForge forge, CodegenMethodScope parent, CodegenClassScope classScope, String providerClassName, AggregationClassNames classNames) {

        if (forge instanceof AggregationServiceFactoryForgeWMethodGen) {
            CodegenMethod initMethod = parent.makeChild(AggregationServiceFactory.class, AggregationServiceFactoryCompiler.class, classScope).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef());
            AggregationServiceFactoryForgeWMethodGen generator = (AggregationServiceFactoryForgeWMethodGen) forge;
            generator.providerCodegen(initMethod, classScope, classNames);

            List<CodegenInnerClass> innerClasses = new ArrayList<>();

            Consumer<AggregationRowCtorDesc> rowCtorDescConsumer = rowCtorDesc -> generator.rowCtorCodegen(rowCtorDesc);
            makeRow(false, join, generator.getRowLevelDesc(), generator.getClass(), rowCtorDescConsumer, classScope, innerClasses, classNames);

            makeRowFactory(generator.getRowLevelDesc(), generator.getClass(), classScope, innerClasses, providerClassName, classNames);

            BiConsumer<CodegenMethod, Integer> readConsumer = (method, level) -> generator.rowReadMethodCodegen(method, level);
            BiConsumer<CodegenMethod, Integer> writeConsumer = (method, level) -> generator.rowWriteMethodCodegen(method, level);
            makeRowSerde(generator.getRowLevelDesc(), generator.getClass(), readConsumer, writeConsumer, innerClasses, classScope, providerClassName, classNames);

            makeService(generator, innerClasses, classScope, providerClassName, classNames);

            makeFactory(generator, classScope, innerClasses, providerClassName, classNames);

            return new AggregationServiceFactoryMakeResult(initMethod, innerClasses);
        }

        SAIFFInitializeSymbol symbols = new SAIFFInitializeSymbol();
        CodegenMethod initMethod = parent.makeChildWithScope(AggregationServiceFactory.class, AggregationServiceFactoryCompiler.class, symbols, classScope).addParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef());
        AggregationServiceFactoryForgeWProviderGen generator = (AggregationServiceFactoryForgeWProviderGen) forge;
        initMethod.getBlock().methodReturn(generator.makeProvider(initMethod, symbols, classScope));
        return new AggregationServiceFactoryMakeResult(initMethod, Collections.emptyList());
    }

    public static List<CodegenInnerClass> makeTable(AggregationCodegenRowLevelDesc rowLevelDesc, Class forgeClass, CodegenClassScope classScope, AggregationClassNames classNames, String providerClassName) {
        List<CodegenInnerClass> innerClasses = new ArrayList<>();
        makeRow(true, false, rowLevelDesc, forgeClass, rowCtorDesc -> AggregationServiceCodegenUtil.generateIncidentals(false, false, rowCtorDesc), classScope, innerClasses, classNames);

        makeRowFactory(rowLevelDesc, forgeClass, classScope, innerClasses, providerClassName, classNames);

        BiConsumer<CodegenMethod, Integer> readConsumer = (method, level) -> {
        };
        BiConsumer<CodegenMethod, Integer> writeConsumer = (method, level) -> {
        };
        makeRowSerde(rowLevelDesc, forgeClass, readConsumer, writeConsumer, innerClasses, classScope, providerClassName, classNames);
        return innerClasses;
    }

    private static void makeRowFactory(AggregationCodegenRowLevelDesc rowLevelDesc, Class forgeClass, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, String providerClassName, AggregationClassNames classNames) {

        if (rowLevelDesc.getOptionalTopRow() != null) {
            makeRowFactoryForLevel(classNames.getRowTop(), classNames.getRowFactoryTop(), forgeClass, classScope, innerClasses, providerClassName);
        }

        if (rowLevelDesc.getOptionalAdditionalRows() != null) {
            for (int i = 0; i < rowLevelDesc.getOptionalAdditionalRows().length; i++) {
                makeRowFactoryForLevel(classNames.getRowPerLevel(i), classNames.getRowFactoryPerLevel(i), forgeClass, classScope, innerClasses, providerClassName);
            }
        }
    }

    private static void makeRowFactoryForLevel(String classNameRow, String classNameFactory, Class forgeClass, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, String providerClassName) {
        CodegenMethod makeMethod = CodegenMethod.makeParentNode(AggregationRow.class, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);

        List<CodegenTypedParam> rowCtorParams = new ArrayList<>();
        rowCtorParams.add(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(forgeClass, classScope, rowCtorParams);

        if (forgeClass == AggregationServiceNullFactory.INSTANCE.getClass()) {
            makeMethod.getBlock().methodReturn(constantNull());
        } else {
            makeMethod.getBlock().methodReturn(CodegenExpressionBuilder.newInstance(classNameRow));
        }

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(makeMethod, "make", methods);
        CodegenInnerClass innerClass = new CodegenInnerClass(classNameFactory, AggregationRowFactory.class, ctor, Collections.emptyList(), methods);
        innerClasses.add(innerClass);
    }

    private static void makeRowSerde(AggregationCodegenRowLevelDesc levels, Class forgeClass, BiConsumer<CodegenMethod, Integer> readConsumer, BiConsumer<CodegenMethod, Integer> writeConsumer, List<CodegenInnerClass> innerClasses, CodegenClassScope classScope,
                                     String providerClassName, AggregationClassNames classNames) {

        if (levels.getOptionalTopRow() != null) {
            makeRowSerdeForLevel(classNames.getRowTop(), classNames.getRowSerdeTop(), -1, levels.getOptionalTopRow(), forgeClass, readConsumer, writeConsumer, classScope, innerClasses, providerClassName);
        }

        if (levels.getOptionalAdditionalRows() != null) {
            for (int i = 0; i < levels.getOptionalAdditionalRows().length; i++) {
                makeRowSerdeForLevel(classNames.getRowPerLevel(i), classNames.getRowSerdePerLevel(i), i, levels.getOptionalAdditionalRows()[i], forgeClass, readConsumer, writeConsumer, classScope, innerClasses, providerClassName);
            }
        }
    }

    private static void makeRowSerdeForLevel(String classNameRow,
                                             String classNameSerde,
                                             int level,
                                             AggregationCodegenRowDetailDesc levelDesc,
                                             Class forgeClass,
                                             BiConsumer<CodegenMethod, Integer> readConsumer,
                                             BiConsumer<CodegenMethod, Integer> writeConsumer,
                                             CodegenClassScope classScope,
                                             List<CodegenInnerClass> innerClasses,
                                             String providerClassName) {

        List<CodegenTypedParam> ctorParams = new ArrayList<>();
        ctorParams.add(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(forgeClass, classScope, ctorParams);

        // generic interface must still cast in Janino
        CodegenExpressionRef input = ref("input");
        CodegenExpressionRef output = ref("output");
        CodegenExpressionRef unitKey = ref("unitKey");
        CodegenExpressionRef writer = ref("writer");
        CodegenMethod writeMethod = CodegenMethod.makeParentNode(void.class, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(CodegenNamedParam.from(Object.class, "object", DataOutput.class, output.getRef(), byte[].class, unitKey.getRef(), EventBeanCollatedWriter.class, writer.getRef()))
                .addThrown(IOException.class);

        CodegenMethod readMethod = CodegenMethod.makeParentNode(Object.class, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .addParam(CodegenNamedParam.from(DataInput.class, input.getRef(), byte[].class, unitKey.getRef())).addThrown(IOException.class);

        if (forgeClass == AggregationServiceNullFactory.INSTANCE.getClass()) {
            readMethod.getBlock().methodReturn(constantNull());
        } else {
            readMethod.getBlock().declareVar(classNameRow, "row", CodegenExpressionBuilder.newInstance(classNameRow));
            readConsumer.accept(readMethod, level);

            AggregationForgeFactory[] methodFactories = levelDesc.getStateDesc().getMethodFactories();
            AggregationStateFactoryForge[] accessStates = levelDesc.getStateDesc().getAccessStateForges();
            writeMethod.getBlock().declareVar(classNameRow, "row", cast(classNameRow, ref("object")));
            writeConsumer.accept(writeMethod, level);

            if (methodFactories != null) {
                for (int i = 0; i < methodFactories.length; i++) {
                    methodFactories[i].getAggregator().writeCodegen(ref("row"), i, output, unitKey, writer, writeMethod, classScope);
                }

                for (int i = 0; i < methodFactories.length; i++) {
                    methodFactories[i].getAggregator().readCodegen(ref("row"), i, input, unitKey, readMethod, classScope);
                }
            }

            if (accessStates != null) {
                for (int i = 0; i < accessStates.length; i++) {
                    accessStates[i].getAggregator().writeCodegen(ref("row"), i, output, unitKey, writer, writeMethod, classScope);
                }

                for (int i = 0; i < accessStates.length; i++) {
                    accessStates[i].getAggregator().readCodegen(ref("row"), i, input, readMethod, unitKey, classScope);
                }
            }

            readMethod.getBlock().methodReturn(ref("row"));
        }

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(writeMethod, "write", methods);
        CodegenStackGenerator.recursiveBuildStack(readMethod, "read", methods);

        CodegenInnerClass innerClass = new CodegenInnerClass(classNameSerde, DataInputOutputSerde.class, ctor, Collections.emptyList(), methods);
        innerClasses.add(innerClass);
    }

    private static void makeRow(boolean isGenerateTableEnter, boolean isJoin, AggregationCodegenRowLevelDesc rowLevelDesc, Class forgeClass, Consumer<AggregationRowCtorDesc> rowCtorConsumer, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, AggregationClassNames classNames) {

        if (rowLevelDesc.getOptionalTopRow() != null) {
            makeRowForLevel(isGenerateTableEnter, isJoin, classNames.getRowTop(), rowLevelDesc.getOptionalTopRow(), forgeClass, rowCtorConsumer, classScope, innerClasses);
        }

        if (rowLevelDesc.getOptionalAdditionalRows() != null) {
            for (int i = 0; i < rowLevelDesc.getOptionalAdditionalRows().length; i++) {
                String className = classNames.getRowPerLevel(i);
                makeRowForLevel(isGenerateTableEnter, isJoin, className, rowLevelDesc.getOptionalAdditionalRows()[i], forgeClass, rowCtorConsumer, classScope, innerClasses);
            }
        }
    }

    private static void makeRowForLevel(boolean isGenerateTableEnter, boolean isJoin, String className, AggregationCodegenRowDetailDesc detail, Class forgeClass, Consumer<AggregationRowCtorDesc> rowCtorConsumer, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses) {
        ExprForge[][] methodForges = detail.getStateDesc().getOptionalMethodForges();
        AggregationForgeFactory[] methodFactories = detail.getStateDesc().getMethodFactories();
        AggregationStateFactoryForge[] accessFactories = detail.getStateDesc().getAccessStateForges();
        AggregationAccessorSlotPairForge[] accessAccessors = detail.getAccessAccessors();
        int numMethodFactories = methodFactories == null ? 0 : methodFactories.length;

        // make member+ctor
        List<CodegenTypedParam> rowCtorParams = new ArrayList<>();
        CodegenCtor rowCtor = new CodegenCtor(forgeClass, classScope, rowCtorParams);
        CodegenNamedMethods namedMethods = new CodegenNamedMethods();
        List<CodegenTypedParam> rowMembers = new ArrayList<>();
        rowCtorConsumer.accept(new AggregationRowCtorDesc(classScope, rowCtor, rowMembers, namedMethods));
        CodegenMemberCol membersColumnized = initForgesMakeRowCtor(isJoin, rowCtor, classScope, methodFactories, accessFactories, methodForges);

        // make state-update
        CodegenMethod applyEnterMethod = makeStateUpdate(!isGenerateTableEnter, AggregationCodegenUpdateType.APPLYENTER, methodForges, methodFactories, accessFactories, classScope, namedMethods);
        CodegenMethod applyLeaveMethod = makeStateUpdate(!isGenerateTableEnter, AggregationCodegenUpdateType.APPLYLEAVE, methodForges, methodFactories, accessFactories, classScope, namedMethods);
        CodegenMethod clearMethod = makeStateUpdate(true, AggregationCodegenUpdateType.CLEAR, methodForges, methodFactories, accessFactories, classScope, namedMethods);

        // get-access-state
        CodegenMethod getAccessStateMethod = makeGetAccessState(numMethodFactories, accessFactories, classScope);

        // make state-update for tables
        CodegenMethod enterAggMethod = makeTableMethod(isGenerateTableEnter, AggregationCodegenTableUpdateType.ENTER, methodFactories, classScope);
        CodegenMethod leaveAggMethod = makeTableMethod(isGenerateTableEnter, AggregationCodegenTableUpdateType.LEAVE, methodFactories, classScope);
        CodegenMethod resetAggMethod = makeTableResetMethod(isGenerateTableEnter, methodFactories, accessFactories, classScope);
        CodegenMethod enterAccessMethod = makeTableAccess(isGenerateTableEnter, AggregationCodegenTableUpdateType.ENTER, numMethodFactories, accessFactories, classScope, namedMethods);
        CodegenMethod leaveAccessMethod = makeTableAccess(isGenerateTableEnter, AggregationCodegenTableUpdateType.LEAVE, numMethodFactories, accessFactories, classScope, namedMethods);

        // make getters
        CodegenMethod getValueMethod = makeGet(AggregationCodegenGetType.GETVALUE, methodFactories, accessAccessors, accessFactories, classScope, namedMethods);
        CodegenMethod getEventBeanMethod = makeGet(AggregationCodegenGetType.GETEVENTBEAN, methodFactories, accessAccessors, accessFactories, classScope, namedMethods);
        CodegenMethod getCollectionScalarMethod = makeGet(AggregationCodegenGetType.GETCOLLECTIONSCALAR, methodFactories, accessAccessors, accessFactories, classScope, namedMethods);
        CodegenMethod getCollectionOfEventsMethod = makeGet(AggregationCodegenGetType.GETCOLLECTIONOFEVENTS, methodFactories, accessAccessors, accessFactories, classScope, namedMethods);

        CodegenClassMethods innerMethods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(applyEnterMethod, "applyEnter", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(applyLeaveMethod, "applyLeave", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(clearMethod, "clear", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(enterAggMethod, "enterAgg", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(leaveAggMethod, "leaveAgg", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(resetAggMethod, "reset", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(enterAccessMethod, "enterAccess", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(leaveAccessMethod, "leaveAccess", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getAccessStateMethod, "getAccessState", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getValueMethod, "getValue", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getEventBeanMethod, "getEventBean", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getCollectionScalarMethod, "getCollectionScalar", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getCollectionOfEventsMethod, "getCollectionOfEvents", innerMethods);
        for (Map.Entry<String, CodegenMethod> methodEntry : namedMethods.getMethods().entrySet()) {
            CodegenStackGenerator.recursiveBuildStack(methodEntry.getValue(), methodEntry.getKey(), innerMethods);
        }

        for (Map.Entry<CodegenExpressionMemberWCol, Class> entry : membersColumnized.getMembers().entrySet()) {
            rowMembers.add(new CodegenTypedParam(entry.getValue(), entry.getKey().getRef(), false, true));
        }

        CodegenInnerClass innerClass = new CodegenInnerClass(className, AggregationRow.class, rowCtor, rowMembers, innerMethods);
        innerClasses.add(innerClass);
    }

    private static CodegenMethod makeTableMethod(boolean isGenerateTableEnter, AggregationCodegenTableUpdateType type, AggregationForgeFactory[] methodFactories, CodegenClassScope classScope) {
        CodegenMethod method = CodegenMethod.makeParentNode(void.class, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, "column").addParam(Object.class, "value");
        if (!isGenerateTableEnter) {
            method.getBlock().methodThrowUnsupported();
            return method;
        }

        CodegenExpressionRef value = ref("value");
        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(ref("column"), methodFactories.length, true);
        for (int i = 0; i < methodFactories.length; i++) {
            AggregationForgeFactory factory = methodFactories[i];
            Class[] evaluationTypes = ExprNodeUtilityQuery.getExprResultTypes(factory.getAggregationExpression().getPositionalParams());
            CodegenMethod updateMethod = method.makeChild(void.class, factory.getAggregator().getClass(), classScope).addParam(Object.class, "value");
            if (type == AggregationCodegenTableUpdateType.ENTER) {
                factory.getAggregator().applyTableEnterCodegen(value, evaluationTypes, updateMethod, classScope);
            } else {
                factory.getAggregator().applyTableLeaveCodegen(value, evaluationTypes, updateMethod, classScope);
            }
            blocks[i].localMethod(updateMethod, value).blockReturnNoValue();
        }

        return method;
    }

    private static CodegenMethod makeTableResetMethod(boolean isGenerateTableEnter, AggregationForgeFactory[] methodFactories, AggregationStateFactoryForge[] accessFactories, CodegenClassScope classScope) {
        CodegenMethod method = CodegenMethod.makeParentNode(void.class, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, "column");
        if (!isGenerateTableEnter) {
            method.getBlock().methodThrowUnsupported();
            return method;
        }

        List<CodegenMethod> methods = new ArrayList<>();

        if (methodFactories != null) {
            for (AggregationForgeFactory factory : methodFactories) {
                CodegenMethod resetMethod = method.makeChild(void.class, factory.getAggregator().getClass(), classScope);
                factory.getAggregator().clearCodegen(resetMethod, classScope);
                methods.add(resetMethod);
            }
        }

        if (accessFactories != null) {
            for (AggregationStateFactoryForge accessFactory: accessFactories) {
                CodegenMethod resetMethod = method.makeChild(void.class, accessFactory.getAggregator().getClass(), classScope);
                accessFactory.getAggregator().clearCodegen(resetMethod, classScope);
                methods.add(resetMethod);
            }
        }

        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(ref("column"), methods.size(), false);
        int count = 0;
        for (CodegenMethod getValue : methods) {
            blocks[count++].expression(localMethod(getValue));
        }

        return method;
    }

    private static CodegenMethod makeTableAccess(boolean isGenerateTableEnter, AggregationCodegenTableUpdateType type, int offset, AggregationStateFactoryForge[] accessStateFactories, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod method = CodegenMethod.makeParentNode(void.class, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, "column").addParam(EventBean[].class, NAME_EPS).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        if (!isGenerateTableEnter) {
            method.getBlock().methodThrowUnsupported();
            return method;
        }

        int[] colums = new int[accessStateFactories.length];
        for (int i = 0; i < accessStateFactories.length; i++) {
            colums[i] = offset + i;
        }

        CodegenBlock[] blocks = method.getBlock().switchBlockOptions(ref("column"), colums, true);
        for (int i = 0; i < accessStateFactories.length; i++) {
            AggregationStateFactoryForge stateFactoryForge = accessStateFactories[i];
            AggregatorAccess aggregator = stateFactoryForge.getAggregator();

            ExprForgeCodegenSymbol symbols = new ExprForgeCodegenSymbol(false, null);
            CodegenMethod updateMethod = method.makeChildWithScope(void.class, stateFactoryForge.getClass(), symbols, classScope).addParam(ExprForgeCodegenNames.PARAMS);
            if (type == AggregationCodegenTableUpdateType.ENTER) {
                aggregator.applyEnterCodegen(updateMethod, symbols, classScope, namedMethods);
                blocks[i].localMethod(updateMethod, REF_EPS, constantTrue(), REF_EXPREVALCONTEXT);
            } else {
                aggregator.applyLeaveCodegen(updateMethod, symbols, classScope, namedMethods);
                blocks[i].localMethod(updateMethod, REF_EPS, constantFalse(), REF_EXPREVALCONTEXT);
            }
            blocks[i].blockReturnNoValue();
        }

        return method;
    }

    private static CodegenMethod makeGetAccessState(int offset, AggregationStateFactoryForge[] accessStateFactories, CodegenClassScope classScope) {
        CodegenMethod method = CodegenMethod.makeParentNode(Object.class, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, "column");

        int[] colums = new int[accessStateFactories == null ? 0 : accessStateFactories.length];
        for (int i = 0; i < colums.length; i++) {
            colums[i] = offset + i;
        }

        CodegenBlock[] blocks = method.getBlock().switchBlockOptions(ref("column"), colums, true);
        for (int i = 0; i < colums.length; i++) {
            AggregationStateFactoryForge stateFactoryForge = accessStateFactories[i];
            CodegenExpression expr = stateFactoryForge.codegenGetAccessTableState(i + offset, method, classScope);
            blocks[i].blockReturn(expr);
        }

        return method;
    }

    private static CodegenMethod makeGet(AggregationCodegenGetType getType, AggregationForgeFactory[] methodFactories, AggregationAccessorSlotPairForge[] accessAccessors, AggregationStateFactoryForge[] accessFactories, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod parent = CodegenMethod.makeParentNode(getType.getReturnType(), AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(GETPARAMS);

        // for non-get-value we can simply return null if this has no access aggs
        if (getType != AggregationCodegenGetType.GETVALUE && accessFactories == null) {
            parent.getBlock().methodReturn(constantNull());
            return parent;
        }

        List<CodegenMethod> methods = new ArrayList<>();

        int count = 0;
        int numMethodStates = 0;
        if (methodFactories != null) {
            for (AggregationForgeFactory factory : methodFactories) {
                CodegenMethod method = parent.makeChild(getType.getReturnType(), factory.getClass(), classScope).addParam(CodegenNamedParam.from(EventBean[].class, NAME_EPS, boolean.class, ExprForgeCodegenNames.NAME_ISNEWDATA, ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT));
                methods.add(method);

                if (getType == AggregationCodegenGetType.GETVALUE) {
                    factory.getAggregator().getValueCodegen(method, classScope);
                } else {
                    method.getBlock().methodReturn(constantNull()); // method aggs don't do others
                }
                count++;
                numMethodStates++;
            }
        }

        if (accessAccessors != null) {
            for (AggregationAccessorSlotPairForge accessorSlotPair : accessAccessors) {
                CodegenMethod method = parent.makeChild(getType.getReturnType(), accessorSlotPair.getAccessorForge().getClass(), classScope).addParam(CodegenNamedParam.from(EventBean[].class, NAME_EPS, boolean.class, ExprForgeCodegenNames.NAME_ISNEWDATA, ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT));
                int stateNumber = numMethodStates + accessorSlotPair.getSlot();

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
                methods.add(method);
                count++;
            }
        }

        CodegenBlock[] blocks = parent.getBlock().switchBlockOfLength(ref("column"), count, true);
        count = 0;
        for (CodegenMethod getValue : methods) {
            blocks[count++].blockReturn(localMethod(getValue, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
        }

        return parent;
    }

    private static CodegenMethod makeStateUpdate(boolean isGenerate, AggregationCodegenUpdateType updateType, ExprForge[][] methodForges, AggregationForgeFactory[] methodFactories, AggregationStateFactoryForge[] accessFactories, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        ExprForgeCodegenSymbol symbols = new ExprForgeCodegenSymbol(true, updateType == AggregationCodegenUpdateType.APPLYENTER);
        CodegenMethod parent = CodegenMethod.makeParentNode(void.class, AggregationServiceFactoryCompiler.class, symbols, classScope).addParam(updateType.getParams());

        int count = 0;
        List<CodegenMethod> methods = new ArrayList<>();

        if (methodFactories != null && isGenerate) {
            for (AggregationForgeFactory factory : methodFactories) {
                String exprText = null;
                CodegenExpression getValue = null;
                if (classScope.isInstrumented()) {
                    exprText = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(factory.getAggregationExpression());
                    getValue = exprDotMethod(ref("this"), "getValue", constant(count), constantNull(), constantTrue(), constantNull());
                }

                CodegenMethod method = parent.makeChild(void.class, factory.getClass(), classScope);
                methods.add(method);
                switch (updateType) {
                    case APPLYENTER:
                        method.getBlock().apply(instblock(classScope, "qAggNoAccessEnterLeave", constantTrue(), constant(count), getValue, constant(exprText)));
                        factory.getAggregator().applyEvalEnterCodegen(method, symbols, methodForges[count], classScope);
                        method.getBlock().apply(instblock(classScope, "aAggNoAccessEnterLeave", constantTrue(), constant(count), getValue));
                        break;
                    case APPLYLEAVE:
                        method.getBlock().apply(instblock(classScope, "qAggNoAccessEnterLeave", constantFalse(), constant(count), getValue, constant(exprText)));
                        factory.getAggregator().applyEvalLeaveCodegen(method, symbols, methodForges[count], classScope);
                        method.getBlock().apply(instblock(classScope, "aAggNoAccessEnterLeave", constantFalse(), constant(count), getValue));
                        break;
                    case CLEAR:
                        factory.getAggregator().clearCodegen(method, classScope);
                        break;
                }
                count++;
            }
        }

        if (accessFactories != null && isGenerate) {
            for (AggregationStateFactoryForge factory : accessFactories) {
                String exprText = null;
                if (classScope.isInstrumented()) {
                    exprText = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(factory.getExpression());
                }

                CodegenMethod method = parent.makeChild(void.class, factory.getClass(), classScope);
                methods.add(method);
                switch (updateType) {
                    case APPLYENTER:
                        method.getBlock().apply(instblock(classScope, "qAggAccessEnterLeave", constantTrue(), constant(count), constant(exprText)));
                        factory.getAggregator().applyEnterCodegen(method, symbols, classScope, namedMethods);
                        method.getBlock().apply(instblock(classScope, "aAggAccessEnterLeave", constantTrue(), constant(count)));
                        break;
                    case APPLYLEAVE:
                        method.getBlock().apply(instblock(classScope, "qAggAccessEnterLeave", constantFalse(), constant(count), constant(exprText)));
                        factory.getAggregator().applyLeaveCodegen(method, symbols, classScope, namedMethods);
                        method.getBlock().apply(instblock(classScope, "aAggAccessEnterLeave", constantFalse(), constant(count)));
                        break;
                    case CLEAR:
                        factory.getAggregator().clearCodegen(method, classScope);
                        break;
                }
                count++;
            }
        }

        // code for enter
        symbols.derivedSymbolsCodegen(parent, parent.getBlock(), classScope);
        for (CodegenMethod method : methods) {
            parent.getBlock().localMethod(method);
        }
        return parent;
    }

    private static CodegenMemberCol initForgesMakeRowCtor(boolean join, CodegenCtor rowCtor, CodegenClassScope classScope, AggregationForgeFactory[] methodFactories, AggregationStateFactoryForge[] accessFactories, ExprForge[][] methodForges) {
        CodegenMemberCol membersColumnized = new CodegenMemberCol();
        int count = 0;
        if (methodFactories != null) {
            for (AggregationForgeFactory factory : methodFactories) {
                factory.initMethodForge(count, rowCtor, membersColumnized, classScope);
                count++;
            }
        }
        if (accessFactories != null) {
            for (AggregationStateFactoryForge factory : accessFactories) {
                factory.initAccessForge(count, join, rowCtor, membersColumnized, classScope);
                count++;
            }
        }
        return membersColumnized;
    }

    private static void makeService(AggregationServiceFactoryForgeWMethodGen forge, List<CodegenInnerClass> innerClasses, CodegenClassScope classScope, String providerClassName, AggregationClassNames classNames) {
        CodegenNamedMethods namedMethods = new CodegenNamedMethods();

        CodegenMethod applyEnterMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean[].class, NAME_EPS).addParam(Object.class, AggregationServiceCodegenNames.NAME_GROUPKEY).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.applyEnterCodegen(applyEnterMethod, classScope, namedMethods, classNames);

        CodegenMethod applyLeaveMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean[].class, NAME_EPS).addParam(Object.class, AggregationServiceCodegenNames.NAME_GROUPKEY).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.applyLeaveCodegen(applyLeaveMethod, classScope, namedMethods, classNames);

        CodegenMethod setCurrentAccessMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(Object.class, AggregationServiceCodegenNames.NAME_GROUPKEY).addParam(int.class, AggregationServiceCodegenNames.NAME_AGENTINSTANCEID).addParam(AggregationGroupByRollupLevel.class, AggregationServiceCodegenNames.NAME_ROLLUPLEVEL);
        forge.setCurrentAccessCodegen(setCurrentAccessMethod, classScope, classNames);

        CodegenMethod clearResultsMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.clearResultsCodegen(clearResultsMethod, classScope);

        CodegenMethod setRemovedCallbackMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AggregationRowRemovedCallback.class, AggregationServiceCodegenNames.NAME_CALLBACK);
        forge.setRemovedCallbackCodegen(setRemovedCallbackMethod);

        CodegenMethod acceptMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AggregationServiceVisitor.class, AggregationServiceCodegenNames.NAME_AGGVISITOR);
        forge.acceptCodegen(acceptMethod, classScope);

        CodegenMethod acceptGroupDetailMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AggregationServiceVisitorWGroupDetail.class, AggregationServiceCodegenNames.NAME_AGGVISITOR);
        forge.acceptGroupDetailCodegen(acceptGroupDetailMethod, classScope);

        CodegenMethod isGroupedMethod = CodegenMethod.makeParentNode(boolean.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        forge.isGroupedCodegen(isGroupedMethod, classScope);

        CodegenMethod getContextPartitionAggregationServiceMethod = CodegenMethod.makeParentNode(AggregationService.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_AGENTINSTANCEID);
        getContextPartitionAggregationServiceMethod.getBlock().methodReturn(ref("this"));

        CodegenMethod getValueMethod = CodegenMethod.makeParentNode(Object.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_COLUMN).addParam(int.class, AggregationServiceCodegenNames.NAME_AGENTINSTANCEID).addParam(EventBean[].class, NAME_EPS).addParam(boolean.class, NAME_ISNEWDATA).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.getValueCodegen(getValueMethod, classScope, namedMethods);

        CodegenMethod getCollectionOfEventsMethod = CodegenMethod.makeParentNode(Collection.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_COLUMN).addParam(EventBean[].class, NAME_EPS).addParam(boolean.class, NAME_ISNEWDATA).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.getCollectionOfEventsCodegen(getCollectionOfEventsMethod, classScope, namedMethods);

        CodegenMethod getEventBeanMethod = CodegenMethod.makeParentNode(EventBean.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_COLUMN).addParam(EventBean[].class, NAME_EPS).addParam(boolean.class, NAME_ISNEWDATA).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.getEventBeanCodegen(getEventBeanMethod, classScope, namedMethods);

        CodegenMethod getRowMethod = CodegenMethod.makeParentNode(AggregationRow.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_AGENTINSTANCEID).addParam(EventBean[].class, NAME_EPS).addParam(boolean.class, NAME_ISNEWDATA).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.getRowCodegen(getRowMethod, classScope, namedMethods);

        CodegenMethod getGroupKeyMethod = CodegenMethod.makeParentNode(Object.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_AGENTINSTANCEID);
        forge.getGroupKeyCodegen(getGroupKeyMethod, classScope);

        CodegenMethod getGroupKeysMethod = CodegenMethod.makeParentNode(Collection.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.getGroupKeysCodegen(getGroupKeysMethod, classScope);

        CodegenMethod getCollectionScalarMethod = CodegenMethod.makeParentNode(Collection.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(int.class, AggregationServiceCodegenNames.NAME_COLUMN).addParam(EventBean[].class, NAME_EPS).addParam(boolean.class, NAME_ISNEWDATA).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        forge.getCollectionScalarCodegen(getCollectionScalarMethod, classScope, namedMethods);

        CodegenMethod stopMethod = CodegenMethod.makeParentNode(void.class, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        forge.stopMethodCodegen(forge, stopMethod);

        List<CodegenTypedParam> members = new ArrayList<>();
        List<CodegenTypedParam> ctorParams = new ArrayList<>();
        ctorParams.add(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(AggregationServiceFactoryCompiler.class, classScope, ctorParams);
        forge.ctorCodegen(ctor, members, classScope, classNames);

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
        CodegenStackGenerator.recursiveBuildStack(getRowMethod, "getAggregationRow", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getGroupKeyMethod, "getGroupKey", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getGroupKeysMethod, "getGroupKeys", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getCollectionScalarMethod, "getCollectionScalar", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(stopMethod, "stop", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(ctor, "ctor", innerMethods);
        for (Map.Entry<String, CodegenMethod> methodEntry : namedMethods.getMethods().entrySet()) {
            CodegenStackGenerator.recursiveBuildStack(methodEntry.getValue(), methodEntry.getKey(), innerMethods);
        }

        CodegenInnerClass innerClass = new CodegenInnerClass(classNames.getService(), AggregationService.class, ctor, members, innerMethods);
        innerClasses.add(innerClass);
    }

    private static void makeFactory(AggregationServiceFactoryForgeWMethodGen forge, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, String providerClassName, AggregationClassNames classNames) {
        CodegenMethod makeServiceMethod = CodegenMethod.makeParentNode(AggregationService.class, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(MAKESERVICEPARAMS);
        forge.makeServiceCodegen(makeServiceMethod, classScope, classNames);

        List<CodegenTypedParam> ctorParams = Collections.singletonList(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(AggregationServiceFactoryCompiler.class, classScope, ctorParams);

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(makeServiceMethod, "makeService", methods);
        CodegenInnerClass innerClass = new CodegenInnerClass(classNames.getServiceFactory(), AggregationServiceFactory.class, ctor, Collections.emptyList(), methods);
        innerClasses.add(innerClass);
    }

    private enum AggregationCodegenTableUpdateType {
        ENTER,
        LEAVE;
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
