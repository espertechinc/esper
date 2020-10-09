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

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationServiceFactoryCompilerSerde {
    private final static CodegenExpressionRef INPUT = ref("input");
    private final static CodegenExpressionRef OUTPUT = ref("output");
    private final static CodegenExpressionRef UNIT_KEY = ref("unitKey");
    private final static CodegenExpressionRef WRITER = ref("writer");

    protected static void makeRowSerde(boolean isTargetHA, AggregationClassAssignmentPerLevel assignments, Class forgeClass, BiConsumer<CodegenMethod, Integer> readConsumer, BiConsumer<CodegenMethod, Integer> writeConsumer, List<CodegenInnerClass> innerClasses, CodegenClassScope classScope,
                                       String providerClassName, AggregationClassNames classNames) {

        if (assignments.getOptionalTop() != null) {
            makeRowSerdeForLevel(isTargetHA, assignments.getOptionalTop(), classNames.getRowTop(), classNames.getRowSerdeTop(), -1, forgeClass, readConsumer, writeConsumer, classScope, innerClasses, providerClassName);
        }

        if (assignments.getOptionalPerLevel() != null) {
            for (int i = 0; i < assignments.getOptionalPerLevel().length; i++) {
                makeRowSerdeForLevel(isTargetHA, assignments.getOptionalPerLevel()[i], classNames.getRowPerLevel(i), classNames.getRowSerdePerLevel(i), i, forgeClass, readConsumer, writeConsumer, classScope, innerClasses, providerClassName);
            }
        }
    }

    private static void makeRowSerdeForLevel(boolean isTargetHA,
                                             AggregationClassAssignment[] assignments,
                                             String classNameRow,
                                             String classNameSerde,
                                             int level,
                                             Class forgeClass,
                                             BiConsumer<CodegenMethod, Integer> readConsumer,
                                             BiConsumer<CodegenMethod, Integer> writeConsumer,
                                             CodegenClassScope classScope,
                                             List<CodegenInnerClass> innerClasses,
                                             String providerClassName) {

        // make flat
        if (assignments.length == 1 || !isTargetHA || forgeClass == AggregationServiceNullFactory.INSTANCE.getClass()) {
            CodegenInnerClass inner = makeRowSerdeForLevel(isTargetHA, assignments[0], classNameRow, classNameSerde, level, forgeClass, readConsumer, writeConsumer, classScope, providerClassName);
            inner.addInterfaceImplemented(DataInputOutputSerde.EPTYPE);
            innerClasses.add(inner);
            return;
        }

        // make leafs
        String[] classNamesSerde = new String[assignments.length];
        for (int i = 0; i < assignments.length; i++) {
            classNamesSerde[i] = classNameSerde + "_" + i;
            CodegenInnerClass inner = makeRowSerdeForLevel(isTargetHA, assignments[i], assignments[i].getClassName(), classNamesSerde[i], level, forgeClass, readConsumer, writeConsumer, classScope, providerClassName);
            innerClasses.add(inner);
        }

        // make members
        List<CodegenTypedParam> members = new ArrayList<>(classNameSerde.length());
        for (int i = 0; i < assignments.length; i++) {
            members.add(new CodegenTypedParam(classNamesSerde[i], "s" + i));
        }

        // make ctor
        CodegenCtor ctor = makeCtor(forgeClass, providerClassName, classScope);
        for (int i = 0; i < assignments.length; i++) {
            ctor.getBlock().assignRef("s" + i, newInstance(classNamesSerde[i], ref("o")));
        }

        // make write
        CodegenMethod writeMethod = makeWriteMethod(classScope);
        for (int i = 0; i < assignments.length; i++) {
            writeMethod.getBlock().exprDotMethod(ref("s" + i), "write", ref("object"), OUTPUT, UNIT_KEY, WRITER);
        }

        // make read
        CodegenMethod readMethod = makeReadMethod(classNameRow, classScope);
        readMethod.getBlock().declareVar(classNameRow, "r", newInstance(classNameRow));
        for (int i = 0; i < assignments.length; i++) {
            readMethod.getBlock().assignRef("r." + "l" + i, exprDotMethod(ref("s" + i), "read", INPUT, UNIT_KEY));
        }
        readMethod.getBlock().methodReturn(ref("r"));

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(writeMethod, "write", methods);
        CodegenStackGenerator.recursiveBuildStack(readMethod, "read", methods);

        CodegenInnerClass serde = new CodegenInnerClass(classNameSerde, DataInputOutputSerde.EPTYPE, ctor, members, methods);
        innerClasses.add(serde);
    }

    private static CodegenInnerClass makeRowSerdeForLevel(boolean isTargetHA,
                                                          AggregationClassAssignment assignment,
                                                          String classNameRow,
                                                          String classNameSerde,
                                                          int level,
                                                          Class forgeClass,
                                                          BiConsumer<CodegenMethod, Integer> readConsumer,
                                                          BiConsumer<CodegenMethod, Integer> writeConsumer,
                                                          CodegenClassScope classScope,
                                                          String providerClassName) {

        CodegenCtor ctor = makeCtor(forgeClass, providerClassName, classScope);

        // generic interface must still cast in Janino
        CodegenMethod writeMethod = makeWriteMethod(classScope);
        CodegenMethod readMethod = makeReadMethod(classNameRow, classScope);

        if (!isTargetHA) {
            String message = "Serde not implemented because the compiler target is not HA";
            readMethod.getBlock().methodThrowUnsupported(message);
            writeMethod.getBlock().methodThrowUnsupported(message);
        } else if (forgeClass == AggregationServiceNullFactory.INSTANCE.getClass()) {
            readMethod.getBlock().methodReturn(constantNull());
        } else {
            readMethod.getBlock().declareVar(classNameRow, "row", CodegenExpressionBuilder.newInstance(classNameRow));
            readConsumer.accept(readMethod, level);

            AggregationForgeFactory[] methodFactories = assignment.getMethodFactories();
            AggregationStateFactoryForge[] accessStates = assignment.getAccessStateFactories();
            writeMethod.getBlock().declareVar(classNameRow, "row", cast(classNameRow, ref("object")));
            writeConsumer.accept(writeMethod, level);

            if (methodFactories != null) {
                for (int i = 0; i < methodFactories.length; i++) {
                    methodFactories[i].getAggregator().writeCodegen(ref("row"), i, OUTPUT, UNIT_KEY, WRITER, writeMethod, classScope);
                }

                for (int i = 0; i < methodFactories.length; i++) {
                    methodFactories[i].getAggregator().readCodegen(ref("row"), i, INPUT, UNIT_KEY, readMethod, classScope);
                }
            }

            if (accessStates != null) {
                for (int i = 0; i < accessStates.length; i++) {
                    accessStates[i].getAggregator().writeCodegen(ref("row"), i, OUTPUT, UNIT_KEY, WRITER, writeMethod, classScope);
                }

                for (int i = 0; i < accessStates.length; i++) {
                    accessStates[i].getAggregator().readCodegen(ref("row"), i, INPUT, readMethod, UNIT_KEY, classScope);
                }
            }

            readMethod.getBlock().methodReturn(ref("row"));
        }

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(writeMethod, "write", methods);
        CodegenStackGenerator.recursiveBuildStack(readMethod, "read", methods);

        return new CodegenInnerClass(classNameSerde, null, ctor, Collections.emptyList(), methods);
    }

    private static CodegenMethod makeReadMethod(String returnType, CodegenClassScope classScope) {
        return CodegenMethod.makeParentNode(returnType, AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(CodegenNamedParam.from(EPTypePremade.DATAINPUT.getEPType(), INPUT.getRef(), EPTypePremade.BYTEPRIMITIVEARRAY.getEPType(), UNIT_KEY.getRef())).addThrown(EPTypePremade.IOEXCEPTION.getEPType());
    }

    private static CodegenMethod makeWriteMethod(CodegenClassScope classScope) {
        return CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), AggregationServiceFactoryCompiler.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(CodegenNamedParam.from(EPTypePremade.OBJECT.getEPType(), "object", EPTypePremade.DATAOUTPUT.getEPType(), OUTPUT.getRef(), EPTypePremade.BYTEPRIMITIVEARRAY.getEPType(), UNIT_KEY.getRef(), EventBeanCollatedWriter.EPTYPE, WRITER.getRef()))
            .addThrown(EPTypePremade.IOEXCEPTION.getEPType());
    }

    private static CodegenCtor makeCtor(Class forgeClass, String providerClassName, CodegenClassScope classScope) {
        CodegenTypedParam param = new CodegenTypedParam(providerClassName, "o");
        return new CodegenCtor(forgeClass, classScope, Collections.singletonList(param));
    }
}
