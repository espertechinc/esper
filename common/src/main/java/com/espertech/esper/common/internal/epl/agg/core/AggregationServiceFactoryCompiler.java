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
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceCodegenNames.NAME_GROUPID;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryCompilerRow.makeRow;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryCompilerSerde.makeRowSerde;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;

public class AggregationServiceFactoryCompiler {

    protected final static List<CodegenNamedParam> MAKESERVICEPARAMS = CodegenNamedParam.from(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), AggregationServiceCodegenNames.NAME_ISSUBQUERY, EPTypePremade.INTEGERBOXED.getEPType(), AggregationServiceCodegenNames.NAME_SUBQUERYNUMBER, EPTypePremade.INTEGERPRIMITIVEARRAY.getEPType(), NAME_GROUPID);
    protected final static List<CodegenNamedParam> UPDPARAMS = CodegenNamedParam.from(EventBean.EPTYPEARRAY, NAME_EPS, ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
    protected final static List<CodegenNamedParam> GETPARAMS = CodegenNamedParam.from(EPTypePremade.INTEGERPRIMITIVE.getEPType(), AggregationServiceCodegenNames.NAME_VCOL, EventBean.EPTYPEARRAY, NAME_EPS, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), ExprForgeCodegenNames.NAME_ISNEWDATA, ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);

    public static AggregationServiceFactoryMakeResult makeInnerClassesAndInit(boolean join, AggregationServiceFactoryForge forge, CodegenMethodScope parent, CodegenClassScope classScope, String providerClassName, AggregationClassNames classNames, boolean isTargetHA) {

        if (forge instanceof AggregationServiceFactoryForgeWMethodGen) {
            CodegenMethod initMethod = parent.makeChild(AggregationServiceFactory.EPTYPE, AggregationServiceFactoryCompiler.class, classScope).addParam(EPStatementInitServices.EPTYPE, EPStatementInitServices.REF.getRef());
            AggregationServiceFactoryForgeWMethodGen generator = (AggregationServiceFactoryForgeWMethodGen) forge;
            generator.providerCodegen(initMethod, classScope, classNames);

            List<CodegenInnerClass> innerClasses = new ArrayList<>();

            Consumer<AggregationRowCtorDesc> rowCtorDescConsumer = rowCtorDesc -> generator.rowCtorCodegen(rowCtorDesc);
            AggregationClassAssignmentPerLevel assignments = makeRow(false, join, generator.getRowLevelDesc(), generator.getClass(), rowCtorDescConsumer, classScope, innerClasses, classNames);

            makeRowFactory(generator.getRowLevelDesc(), generator.getClass(), classScope, innerClasses, providerClassName, classNames);

            BiConsumer<CodegenMethod, Integer> readConsumer = (method, level) -> generator.rowReadMethodCodegen(method, level);
            BiConsumer<CodegenMethod, Integer> writeConsumer = (method, level) -> generator.rowWriteMethodCodegen(method, level);
            makeRowSerde(isTargetHA, assignments, generator.getClass(), readConsumer, writeConsumer, innerClasses, classScope, providerClassName, classNames);

            makeService(generator, innerClasses, classScope, providerClassName, classNames);

            makeFactory(generator, classScope, innerClasses, providerClassName, classNames);

            return new AggregationServiceFactoryMakeResult(initMethod, innerClasses);
        }

        SAIFFInitializeSymbol symbols = new SAIFFInitializeSymbol();
        CodegenMethod initMethod = parent.makeChildWithScope(AggregationServiceFactory.EPTYPE, AggregationServiceFactoryCompiler.class, symbols, classScope).addParam(EPStatementInitServices.EPTYPE, EPStatementInitServices.REF.getRef());
        AggregationServiceFactoryForgeWProviderGen generator = (AggregationServiceFactoryForgeWProviderGen) forge;
        initMethod.getBlock().methodReturn(generator.makeProvider(initMethod, symbols, classScope));
        return new AggregationServiceFactoryMakeResult(initMethod, Collections.emptyList());
    }

    public static List<CodegenInnerClass> makeTable(AggregationCodegenRowLevelDesc rowLevelDesc, Class forgeClass, CodegenClassScope classScope, AggregationClassNames classNames, String providerClassName, boolean isTargetHA) {
        List<CodegenInnerClass> innerClasses = new ArrayList<>();
        AggregationClassAssignmentPerLevel assignments = makeRow(true, false, rowLevelDesc, forgeClass, rowCtorDesc -> AggregationServiceCodegenUtil.generateIncidentals(false, false, rowCtorDesc), classScope, innerClasses, classNames);

        makeRowFactory(rowLevelDesc, forgeClass, classScope, innerClasses, providerClassName, classNames);

        BiConsumer<CodegenMethod, Integer> readConsumer = (method, level) -> {
        };
        BiConsumer<CodegenMethod, Integer> writeConsumer = (method, level) -> {
        };
        makeRowSerde(isTargetHA, assignments, forgeClass, readConsumer, writeConsumer, innerClasses, classScope, providerClassName, classNames);
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
        CodegenMethod makeMethod = CodegenMethod.makeParentNode(AggregationRow.EPTYPE, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);

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
        CodegenInnerClass innerClass = new CodegenInnerClass(classNameFactory, AggregationRowFactory.EPTYPE, ctor, Collections.emptyList(), methods);
        innerClasses.add(innerClass);
    }

    private static void makeService(AggregationServiceFactoryForgeWMethodGen forge, List<CodegenInnerClass> innerClasses, CodegenClassScope classScope, String providerClassName, AggregationClassNames classNames) {
        CodegenNamedMethods namedMethods = new CodegenNamedMethods();

        CodegenMethod applyEnterMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean.EPTYPEARRAY, NAME_EPS).addParam(EPTypePremade.OBJECT.getEPType(), AggregationServiceCodegenNames.NAME_GROUPKEY).addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        forge.applyEnterCodegen(applyEnterMethod, classScope, namedMethods, classNames);

        CodegenMethod applyLeaveMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EventBean.EPTYPEARRAY, NAME_EPS).addParam(EPTypePremade.OBJECT.getEPType(), AggregationServiceCodegenNames.NAME_GROUPKEY).addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        forge.applyLeaveCodegen(applyLeaveMethod, classScope, namedMethods, classNames);

        CodegenMethod setCurrentAccessMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.OBJECT.getEPType(), AggregationServiceCodegenNames.NAME_GROUPKEY).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), AggregationServiceCodegenNames.NAME_AGENTINSTANCEID).addParam(AggregationGroupByRollupLevel.EPTYPE, AggregationServiceCodegenNames.NAME_ROLLUPLEVEL);
        forge.setCurrentAccessCodegen(setCurrentAccessMethod, classScope, classNames);

        CodegenMethod clearResultsMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        forge.clearResultsCodegen(clearResultsMethod, classScope);

        CodegenMethod setRemovedCallbackMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AggregationRowRemovedCallback.EPTYPE, AggregationServiceCodegenNames.NAME_CALLBACK);
        forge.setRemovedCallbackCodegen(setRemovedCallbackMethod);

        CodegenMethod acceptMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AggregationServiceVisitor.EPTYPE, AggregationServiceCodegenNames.NAME_AGGVISITOR);
        forge.acceptCodegen(acceptMethod, classScope);

        CodegenMethod acceptGroupDetailMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(AggregationServiceVisitorWGroupDetail.EPTYPE, AggregationServiceCodegenNames.NAME_AGGVISITOR);
        forge.acceptGroupDetailCodegen(acceptGroupDetailMethod, classScope);

        CodegenMethod isGroupedMethod = CodegenMethod.makeParentNode(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        forge.isGroupedCodegen(isGroupedMethod, classScope);

        CodegenMethod getContextPartitionAggregationServiceMethod = CodegenMethod.makeParentNode(AggregationService.EPTYPE, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), AggregationServiceCodegenNames.NAME_AGENTINSTANCEID);
        getContextPartitionAggregationServiceMethod.getBlock().methodReturn(ref("this"));

        CodegenMethod getValueMethod = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), AggregationServiceCodegenNames.NAME_VCOL).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), AggregationServiceCodegenNames.NAME_AGENTINSTANCEID).addParam(EventBean.EPTYPEARRAY, NAME_EPS).addParam(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA).addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        forge.getValueCodegen(getValueMethod, classScope, namedMethods);

        CodegenMethod getCollectionOfEventsMethod = CodegenMethod.makeParentNode(EPTypePremade.COLLECTION.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), AggregationServiceCodegenNames.NAME_VCOL).addParam(EventBean.EPTYPEARRAY, NAME_EPS).addParam(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA).addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        forge.getCollectionOfEventsCodegen(getCollectionOfEventsMethod, classScope, namedMethods);

        CodegenMethod getEventBeanMethod = CodegenMethod.makeParentNode(EventBean.EPTYPE, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), AggregationServiceCodegenNames.NAME_VCOL).addParam(EventBean.EPTYPEARRAY, NAME_EPS).addParam(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA).addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        forge.getEventBeanCodegen(getEventBeanMethod, classScope, namedMethods);

        CodegenMethod getRowMethod = CodegenMethod.makeParentNode(AggregationRow.EPTYPE, forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), AggregationServiceCodegenNames.NAME_AGENTINSTANCEID).addParam(EventBean.EPTYPEARRAY, NAME_EPS).addParam(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA).addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        forge.getRowCodegen(getRowMethod, classScope, namedMethods);

        CodegenMethod getGroupKeyMethod = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), AggregationServiceCodegenNames.NAME_AGENTINSTANCEID);
        forge.getGroupKeyCodegen(getGroupKeyMethod, classScope);

        CodegenMethod getGroupKeysMethod = CodegenMethod.makeParentNode(EPTypePremade.COLLECTION.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        forge.getGroupKeysCodegen(getGroupKeysMethod, classScope);

        CodegenMethod getCollectionScalarMethod = CodegenMethod.makeParentNode(EPTypePremade.COLLECTION.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), AggregationServiceCodegenNames.NAME_VCOL).addParam(EventBean.EPTYPEARRAY, NAME_EPS).addParam(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA).addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        forge.getCollectionScalarCodegen(getCollectionScalarMethod, classScope, namedMethods);

        CodegenMethod stopMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), forge.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
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

        CodegenInnerClass innerClass = new CodegenInnerClass(classNames.getService(), AggregationService.EPTYPE, ctor, members, innerMethods);
        innerClasses.add(innerClass);
    }

    private static void makeFactory(AggregationServiceFactoryForgeWMethodGen forge, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, String providerClassName, AggregationClassNames classNames) {
        CodegenMethod makeServiceMethod = CodegenMethod.makeParentNode(AggregationService.EPTYPE, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(MAKESERVICEPARAMS);
        forge.makeServiceCodegen(makeServiceMethod, classScope, classNames);

        List<CodegenTypedParam> ctorParams = Collections.singletonList(new CodegenTypedParam(providerClassName, "o"));
        CodegenCtor ctor = new CodegenCtor(AggregationServiceFactoryCompiler.class, classScope, ctorParams);

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(makeServiceMethod, "makeService", methods);
        CodegenInnerClass innerClass = new CodegenInnerClass(classNames.getServiceFactory(), AggregationServiceFactory.EPTYPE, ctor, Collections.emptyList(), methods);
        innerClasses.add(innerClass);
    }
}
