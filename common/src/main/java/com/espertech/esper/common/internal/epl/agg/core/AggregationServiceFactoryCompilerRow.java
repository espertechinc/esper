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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMemberWCol;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.util.IntArrayUtil;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod.makeParentNode;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceCodegenNames.*;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryCompiler.GETPARAMS;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryCompiler.UPDPARAMS;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode.instblock;

public class AggregationServiceFactoryCompilerRow {
    private final static String NAME_ASSIGNMENT = "ASSIGNMENTS";

    protected static AggregationClassAssignmentPerLevel makeRow(boolean isGenerateTableEnter, AggregationCodegenRowLevelDesc rowLevelDesc, Class forgeClass, Consumer<AggregationRowCtorDesc> rowCtorConsumer, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses, AggregationClassNames classNames) {

        AggregationClassAssignment[] topAssignments = null;
        if (rowLevelDesc.getOptionalTopRow() != null) {
            topAssignments = makeRowForLevel(isGenerateTableEnter, classNames.getRowTop(), rowLevelDesc.getOptionalTopRow(), forgeClass, rowCtorConsumer, classScope, innerClasses);
        }

        AggregationClassAssignment[][] leafAssignments = null;
        if (rowLevelDesc.getOptionalAdditionalRows() != null) {
            leafAssignments = new AggregationClassAssignment[rowLevelDesc.getOptionalAdditionalRows().length][];
            for (int i = 0; i < rowLevelDesc.getOptionalAdditionalRows().length; i++) {
                String className = classNames.getRowPerLevel(i);
                leafAssignments[i] = makeRowForLevel(isGenerateTableEnter, className, rowLevelDesc.getOptionalAdditionalRows()[i], forgeClass, rowCtorConsumer, classScope, innerClasses);
            }
        }

        return new AggregationClassAssignmentPerLevel(topAssignments, leafAssignments);
    }

    private static AggregationClassAssignment[] makeRowForLevel(boolean table, String className, AggregationCodegenRowDetailDesc detail, Class forgeClass, Consumer<AggregationRowCtorDesc> rowCtorConsumer, CodegenClassScope classScope, List<CodegenInnerClass> innerClasses) {
        // determine column to inner-class assignment to prevent too-many-fields per class
        AggregationClassAssignment currentAssignment = new AggregationClassAssignment(0, forgeClass, classScope);
        int countStates = 0;
        int countVcols = 0;
        int indexAssignment = 0;
        LinkedHashMap<Integer, AggregationClassAssignment> assignments = new LinkedHashMap<>();
        Map<Integer, AggregationClassAssignment> slotToAssignment = new HashMap<>();
        int maxMembersPerClass = classScope.getPackageScope().getConfig().getInternalUseOnlyMaxMembersPerClass();
        int methodFactoryCount = 0;

        // determine number of fields and field-to-class assignment
        if (detail.getStateDesc().getMethodFactories() != null) {
            methodFactoryCount = detail.getStateDesc().getMethodFactories().length;
            for (int methodIndex = 0; methodIndex < detail.getStateDesc().getMethodFactories().length; methodIndex++) {
                if (currentAssignment.getMemberSize() != 0 && currentAssignment.getMemberSize() >= maxMembersPerClass) {
                    assignments.put(indexAssignment++, currentAssignment);
                    currentAssignment = new AggregationClassAssignment(countStates, forgeClass, classScope);
                }

                AggregationForgeFactory factory = detail.getStateDesc().getMethodFactories()[methodIndex];
                currentAssignment.add(factory, detail.getStateDesc().getOptionalMethodForges() == null ? new ExprForge[0] : detail.getStateDesc().getOptionalMethodForges()[methodIndex]);
                currentAssignment.addMethod(new AggregationVColMethod(countVcols, factory));
                factory.getAggregator().initForge(countStates, currentAssignment.getCtor(), currentAssignment.getMembers(), classScope);
                countStates++;
                countVcols++;
            }
        }
        if (detail.getStateDesc().getAccessStateForges() != null) {
            for (int accessIndex = 0; accessIndex < detail.getStateDesc().getAccessStateForges().length; accessIndex++) {
                if (currentAssignment.getMemberSize() != 0 && currentAssignment.getMemberSize() >= maxMembersPerClass) {
                    assignments.put(indexAssignment++, currentAssignment);
                    currentAssignment = new AggregationClassAssignment(countStates, forgeClass, classScope);
                }

                AggregationStateFactoryForge factory = detail.getStateDesc().getAccessStateForges()[accessIndex];
                currentAssignment.add(factory);
                factory.getAggregator().initAccessForge(countStates, currentAssignment.getCtor(), currentAssignment.getMembers(), classScope);
                countStates++;
                slotToAssignment.put(accessIndex, currentAssignment);
            }

            // loop through accessors last
            for (int i = 0; i < detail.getAccessAccessors().length; i++) {
                AggregationAccessorSlotPairForge slotPair = detail.getAccessAccessors()[i];
                int slot = slotPair.getSlot();
                AggregationClassAssignment assignment = slotToAssignment.get(slot);
                int stateNumber = methodFactoryCount + slot;
                assignment.addAccess(new AggregationVColAccess(countVcols, slotPair.getAccessorForge(), stateNumber, detail.getStateDesc().getAccessStateForges()[slot]));
                countVcols++;
            }
        }

        // handle the simpler case of flat-row
        if (assignments.isEmpty()) {
            currentAssignment.setClassName(className);
            CodegenInnerClass innerClass = makeRowForLevelFlat(table, false, currentAssignment, rowCtorConsumer, classScope);
            innerClass.addInterfaceImplemented(AggregationRow.EPTYPE);
            innerClasses.add(innerClass);
            return new AggregationClassAssignment[]{currentAssignment};
        }

        // add current
        assignments.put(indexAssignment, currentAssignment);

        // make leaf-row classes, assign class name and member name
        AggregationClassAssignment[] assignmentArray = new AggregationClassAssignment[assignments.size()];
        int index = 0;
        for (Map.Entry<Integer, AggregationClassAssignment> entry : assignments.entrySet()) {
            AggregationClassAssignment assignment = entry.getValue();
            assignment.setClassName(className + "_" + entry.getKey());
            assignment.setMemberName("l" + entry.getKey());
            CodegenInnerClass innerClass = makeRowForLevelFlat(table, true, assignment, ctor -> {
            }, classScope);
            innerClasses.add(innerClass);
            assignmentArray[index++] = entry.getValue();
        }

        CodegenInnerClass composite = produceComposite(detail, assignmentArray, table, forgeClass, className, rowCtorConsumer, classScope);
        innerClasses.add(composite);
        return assignmentArray;
    }

    private static CodegenInnerClass produceComposite(AggregationCodegenRowDetailDesc detail, AggregationClassAssignment[] leafs, boolean table, Class forgeClass, String className, Consumer<AggregationRowCtorDesc> rowCtorConsumer, CodegenClassScope classScope) {
        // fill enter and leave
        CodegenMethod applyEnterMethod = makeMethodReturnVoid(AggregationCodegenUpdateType.APPLYENTER.params, classScope);
        CodegenMethod applyLeaveMethod = makeMethodReturnVoid(AggregationCodegenUpdateType.APPLYLEAVE.params, classScope);
        if (!table) {
            for (AggregationClassAssignment leaf : leafs) {
                applyEnterMethod.getBlock().exprDotMethod(ref(leaf.getMemberName()), "applyEnter", REF_EPS, REF_EXPREVALCONTEXT);
                applyLeaveMethod.getBlock().exprDotMethod(ref(leaf.getMemberName()), "applyLeave", REF_EPS, REF_EXPREVALCONTEXT);
            }
        }

        CodegenMethod clearMethod = makeMethodReturnVoid(AggregationCodegenUpdateType.CLEAR.params, classScope);
        for (AggregationClassAssignment leaf : leafs) {
            clearMethod.getBlock().exprDotMethod(ref(leaf.getMemberName()), "clear");
        }

        // get-access-state
        CodegenMethod getAccessStateMethod = makeMethodGetAccess(classScope);
        populateSwitchByRange(leafs, getAccessStateMethod, true, (index, block) -> block.blockReturn(exprDotMethod(ref(leafs[index].getMemberName()), "getAccessState", REF_SCOL)));

        // make state-update for tables
        CodegenMethod enterAggMethod = makeMethodTableEnterLeave(classScope);
        CodegenMethod leaveAggMethod = makeMethodTableEnterLeave(classScope);
        CodegenMethod resetAggMethod = makeMethodTableReset(classScope);
        CodegenMethod enterAccessMethod = makeMethodTableAccess(classScope);
        CodegenMethod leaveAccessMethod = makeMethodTableAccess(classScope);
        if (table) {
            populateSwitchByRange(leafs, enterAggMethod, false, (index, block) -> block.exprDotMethod(ref(leafs[index].getMemberName()), "enterAgg", REF_SCOL, REF_VALUE));
            populateSwitchByRange(leafs, leaveAggMethod, false, (index, block) -> block.exprDotMethod(ref(leafs[index].getMemberName()), "leaveAgg", REF_SCOL, REF_VALUE));
            populateSwitchByRange(leafs, resetAggMethod, false, (index, block) -> block.exprDotMethod(ref(leafs[index].getMemberName()), "reset", REF_SCOL));
            populateSwitchByRange(leafs, enterAccessMethod, false, (index, block) -> block.exprDotMethod(ref(leafs[index].getMemberName()), "enterAccess", REF_SCOL, REF_EPS, REF_EXPREVALCONTEXT));
            populateSwitchByRange(leafs, leaveAccessMethod, false, (index, block) -> block.exprDotMethod(ref(leafs[index].getMemberName()), "leaveAccess", REF_SCOL, REF_EPS, REF_EXPREVALCONTEXT));
        }

        // make getters
        int[] vcolIndexes = getVcolIndexes(detail, leafs);
        CodegenMethod getValueMethod = makeMethodGet(AggregationCodegenGetType.GETVALUE, classScope);
        populateSwitchByIndex(leafs, getValueMethod, true, (index, block) -> block.blockReturn(exprDotMethod(ref(leafs[index].getMemberName()), "getValue", REF_VCOL, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT)));
        CodegenMethod getEventBeanMethod = makeMethodGet(AggregationCodegenGetType.GETEVENTBEAN, classScope);
        populateSwitchByIndex(leafs, getEventBeanMethod, true, (index, block) -> block.blockReturn(exprDotMethod(ref(leafs[index].getMemberName()), "getEventBean", REF_VCOL, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT)));
        CodegenMethod getCollectionScalarMethod = makeMethodGet(AggregationCodegenGetType.GETCOLLECTIONSCALAR, classScope);
        populateSwitchByIndex(leafs, getCollectionScalarMethod, true, (index, block) -> block.blockReturn(exprDotMethod(ref(leafs[index].getMemberName()), "getCollectionScalar", REF_VCOL, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT)));
        CodegenMethod getCollectionOfEventsMethod = makeMethodGet(AggregationCodegenGetType.GETCOLLECTIONOFEVENTS, classScope);
        populateSwitchByIndex(leafs, getCollectionOfEventsMethod, true, (index, block) -> block.blockReturn(exprDotMethod(ref(leafs[index].getMemberName()), "getCollectionOfEvents", REF_VCOL, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT)));

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(applyEnterMethod, "applyEnter", methods);
        CodegenStackGenerator.recursiveBuildStack(applyLeaveMethod, "applyLeave", methods);
        CodegenStackGenerator.recursiveBuildStack(clearMethod, "clear", methods);
        CodegenStackGenerator.recursiveBuildStack(enterAggMethod, "enterAgg", methods);
        CodegenStackGenerator.recursiveBuildStack(leaveAggMethod, "leaveAgg", methods);
        CodegenStackGenerator.recursiveBuildStack(resetAggMethod, "reset", methods);
        CodegenStackGenerator.recursiveBuildStack(enterAccessMethod, "enterAccess", methods);
        CodegenStackGenerator.recursiveBuildStack(leaveAccessMethod, "leaveAccess", methods);
        CodegenStackGenerator.recursiveBuildStack(getAccessStateMethod, "getAccessState", methods);
        CodegenStackGenerator.recursiveBuildStack(getValueMethod, "getValue", methods);
        CodegenStackGenerator.recursiveBuildStack(getEventBeanMethod, "getEventBean", methods);
        CodegenStackGenerator.recursiveBuildStack(getCollectionScalarMethod, "getCollectionScalar", methods);
        CodegenStackGenerator.recursiveBuildStack(getCollectionOfEventsMethod, "getCollectionOfEvents", methods);

        // make ctor
        CodegenCtor ctor = new CodegenCtor(forgeClass, classScope, Collections.emptyList());
        for (AggregationClassAssignment leaf : leafs) {
            ctor.getBlock().assignRef(leaf.getMemberName(), newInstance(leaf.getClassName()));
        }

        // make members
        List<CodegenTypedParam> members = new ArrayList<>();
        CodegenTypedParam assignment = new CodegenTypedParam(EPTypePremade.INTEGERPRIMITIVEARRAY.getEPType(), NAME_ASSIGNMENT).setFinal(true).setStatic(true).setInitializer(constant(vcolIndexes));
        members.add(assignment);

        // named methods
        CodegenNamedMethods namedMethods = new CodegenNamedMethods();
        rowCtorConsumer.accept(new AggregationRowCtorDesc(classScope, ctor, members, namedMethods));

        // add named methods from aggregation desc
        for (Map.Entry<String, CodegenMethod> methodEntry : namedMethods.getMethods().entrySet()) {
            CodegenStackGenerator.recursiveBuildStack(methodEntry.getValue(), methodEntry.getKey(), methods);
        }

        // add row members
        for (AggregationClassAssignment leaf : leafs) {
            members.add(new CodegenTypedParam(leaf.getClassName(), leaf.getMemberName()));
        }

        // add composite class
        return new CodegenInnerClass(className, AggregationRow.EPTYPE, ctor, members, methods);
    }

    private static int[] getVcolIndexes(AggregationCodegenRowDetailDesc detail, AggregationClassAssignment[] leafs) {
        int size = detail.getStateDesc().getMethodFactories() == null ? 0 : detail.getStateDesc().getMethodFactories().length;
        size += detail.getAccessAccessors() == null ? 0 : detail.getAccessAccessors().length;
        int[] vcols = new int[size];
        for (int i = 0; i < leafs.length; i++) {
            for (AggregationVColMethod method : leafs[i].getVcolMethods()) {
                vcols[method.getVcol()] = i;
            }
            for (AggregationVColAccess access : leafs[i].getVcolAccess()) {
                vcols[access.getVcol()] = i;
            }
        }
        return vcols;
    }

    private static void populateSwitchByRange(AggregationClassAssignment[] leafs, CodegenMethod method, boolean blocksReturnValue, BiConsumer<Integer, CodegenBlock> blockConsumer) {
        method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "i", constant(0));
        CodegenBlock ifBlock = null;
        for (int i = leafs.length - 1; i > 0; i--) {
            CodegenExpression rangeCheck = relational(REF_SCOL, CodegenExpressionRelational.CodegenRelational.GE, constant(leafs[i].getOffset()));
            if (ifBlock == null) {
                ifBlock = method.getBlock().ifCondition(rangeCheck).assignRef(ref("i"), constant(i));
            } else {
                ifBlock.ifElseIf(rangeCheck).assignRef(ref("i"), constant(i));
            }
        }
        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(ref("i"), leafs.length, blocksReturnValue);
        for (int i = 0; i < leafs.length; i++) {
            blockConsumer.accept(i, blocks[i]);
        }
    }

    private static void populateSwitchByIndex(AggregationClassAssignment[] leafs, CodegenMethod method, boolean blocksReturnValue, BiConsumer<Integer, CodegenBlock> blockConsumer) {
        method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "i", arrayAtIndex(ref(NAME_ASSIGNMENT), ref(NAME_VCOL)));
        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(ref("i"), leafs.length, blocksReturnValue);
        for (int i = 0; i < leafs.length; i++) {
            blockConsumer.accept(i, blocks[i]);
        }
    }

    private static CodegenMethod makeMethodReturnVoid(List<CodegenNamedParam> params, CodegenClassScope classScope) {
        return makeParentNode(EPTypePremade.VOID.getEPType(), AggregationServiceFactoryCompilerRow.class, classScope).addParam(params);
    }

    private static CodegenInnerClass makeRowForLevelFlat(boolean table, boolean standalone, AggregationClassAssignment assignment, Consumer<AggregationRowCtorDesc> rowCtorConsumer, CodegenClassScope classScope) {
        int numMethodFactories = assignment.getMethodFactories() == null ? 0 : assignment.getMethodFactories().length;
        int offset = assignment.getOffset();

        // make member+ctor
        CodegenNamedMethods namedMethods = new CodegenNamedMethods();
        List<CodegenTypedParam> rowMembers = new ArrayList<>();
        for (Map.Entry<CodegenExpressionMemberWCol, EPTypeClass> entry : assignment.getMembers().getMembers().entrySet()) {
            rowMembers.add(new CodegenTypedParam(entry.getValue(), entry.getKey().getRef(), false, true));
        }
        rowCtorConsumer.accept(new AggregationRowCtorDesc(classScope, assignment.getCtor(), rowMembers, namedMethods));

        // make state-update
        CodegenMethod applyEnterMethod = produceStateUpdate(!table, AggregationCodegenUpdateType.APPLYENTER, assignment, classScope, namedMethods);
        CodegenMethod applyLeaveMethod = produceStateUpdate(!table, AggregationCodegenUpdateType.APPLYLEAVE, assignment, classScope, namedMethods);
        CodegenMethod clearMethod = produceStateUpdate(true, AggregationCodegenUpdateType.CLEAR, assignment, classScope, namedMethods);

        // get-access-state
        CodegenMethod getAccessStateMethod = produceGetAccessState(assignment.getOffset() + numMethodFactories, assignment.getAccessStateFactories(), classScope);

        // make state-update for tables
        CodegenMethod enterAggMethod = produceTableMethod(offset, table, AggregationCodegenTableUpdateType.ENTER, assignment.getMethodFactories(), classScope);
        CodegenMethod leaveAggMethod = produceTableMethod(offset, table, AggregationCodegenTableUpdateType.LEAVE, assignment.getMethodFactories(), classScope);
        CodegenMethod resetAggMethod = produceTableResetMethod(offset, table, assignment.getMethodFactories(), assignment.getAccessStateFactories(), classScope);
        CodegenMethod enterAccessMethod = produceTableAccess(offset + numMethodFactories, table, AggregationCodegenTableUpdateType.ENTER, assignment.getAccessStateFactories(), classScope, namedMethods);
        CodegenMethod leaveAccessMethod = produceTableAccess(offset + numMethodFactories, table, AggregationCodegenTableUpdateType.LEAVE, assignment.getAccessStateFactories(), classScope, namedMethods);

        // make getters
        CodegenMethod getValueMethod = produceGet(AggregationCodegenGetType.GETVALUE, assignment, classScope, namedMethods);
        CodegenMethod getEventBeanMethod = produceGet(AggregationCodegenGetType.GETEVENTBEAN, assignment, classScope, namedMethods);
        CodegenMethod getCollectionScalarMethod = produceGet(AggregationCodegenGetType.GETCOLLECTIONSCALAR, assignment, classScope, namedMethods);
        CodegenMethod getCollectionOfEventsMethod = produceGet(AggregationCodegenGetType.GETCOLLECTIONOFEVENTS, assignment, classScope, namedMethods);

        CodegenClassMethods innerMethods = new CodegenClassMethods();
        if (!standalone || !table) {
            CodegenStackGenerator.recursiveBuildStack(applyEnterMethod, "applyEnter", innerMethods);
            CodegenStackGenerator.recursiveBuildStack(applyLeaveMethod, "applyLeave", innerMethods);
        }
        CodegenStackGenerator.recursiveBuildStack(clearMethod, "clear", innerMethods);
        if (table || !standalone) {
            CodegenStackGenerator.recursiveBuildStack(enterAggMethod, "enterAgg", innerMethods);
            CodegenStackGenerator.recursiveBuildStack(leaveAggMethod, "leaveAgg", innerMethods);
            CodegenStackGenerator.recursiveBuildStack(resetAggMethod, "reset", innerMethods);
            CodegenStackGenerator.recursiveBuildStack(enterAccessMethod, "enterAccess", innerMethods);
            CodegenStackGenerator.recursiveBuildStack(leaveAccessMethod, "leaveAccess", innerMethods);
        }
        CodegenStackGenerator.recursiveBuildStack(getAccessStateMethod, "getAccessState", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getValueMethod, "getValue", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getEventBeanMethod, "getEventBean", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getCollectionScalarMethod, "getCollectionScalar", innerMethods);
        CodegenStackGenerator.recursiveBuildStack(getCollectionOfEventsMethod, "getCollectionOfEvents", innerMethods);
        for (Map.Entry<String, CodegenMethod> methodEntry : namedMethods.getMethods().entrySet()) {
            CodegenStackGenerator.recursiveBuildStack(methodEntry.getValue(), methodEntry.getKey(), innerMethods);
        }

        return new CodegenInnerClass(assignment.getClassName(), null, assignment.getCtor(), rowMembers, innerMethods);
    }

    private static CodegenMethod produceTableMethod(int offset, boolean isGenerateTableEnter, AggregationCodegenTableUpdateType type, AggregationForgeFactory[] methodFactories, CodegenClassScope classScope) {
        CodegenMethod method = makeMethodTableEnterLeave(classScope);
        if (!isGenerateTableEnter) {
            method.getBlock().methodThrowUnsupported();
            return method;
        }

        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(REF_SCOL, methodFactories.length, true, offset);
        for (int i = 0; i < methodFactories.length; i++) {
            AggregationForgeFactory factory = methodFactories[i];
            EPType[] evaluationTypes = ExprNodeUtilityQuery.getExprResultTypes(factory.getAggregationExpression().getPositionalParams());
            CodegenMethod updateMethod = method.makeChild(EPTypePremade.VOID.getEPType(), factory.getAggregator().getClass(), classScope).addParam(EPTypePremade.OBJECT.getEPType(), "value");
            if (type == AggregationCodegenTableUpdateType.ENTER) {
                factory.getAggregator().applyTableEnterCodegen(REF_VALUE, evaluationTypes, updateMethod, classScope);
            } else {
                factory.getAggregator().applyTableLeaveCodegen(REF_VALUE, evaluationTypes, updateMethod, classScope);
            }
            blocks[i].localMethod(updateMethod, REF_VALUE).blockReturnNoValue();
        }

        return method;
    }

    private static CodegenMethod makeMethodTableEnterLeave(CodegenClassScope classScope) {
        return makeParentNode(EPTypePremade.VOID.getEPType(), AggregationServiceFactoryCompilerRow.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), NAME_SCOL).addParam(EPTypePremade.OBJECT.getEPType(), NAME_VALUE);
    }

    private static CodegenMethod produceTableResetMethod(int offset, boolean isGenerateTableEnter, AggregationForgeFactory[] methodFactories, AggregationStateFactoryForge[] accessFactories, CodegenClassScope classScope) {
        CodegenMethod method = makeMethodTableReset(classScope);
        if (!isGenerateTableEnter) {
            method.getBlock().methodThrowUnsupported();
            return method;
        }

        List<CodegenMethod> methods = new ArrayList<>();

        if (methodFactories != null) {
            for (AggregationForgeFactory factory : methodFactories) {
                CodegenMethod resetMethod = method.makeChild(EPTypePremade.VOID.getEPType(), factory.getAggregator().getClass(), classScope);
                factory.getAggregator().clearCodegen(resetMethod, classScope);
                methods.add(resetMethod);
            }
        }

        if (accessFactories != null) {
            for (AggregationStateFactoryForge accessFactory : accessFactories) {
                CodegenMethod resetMethod = method.makeChild(EPTypePremade.VOID.getEPType(), accessFactory.getAggregator().getClass(), classScope);
                accessFactory.getAggregator().clearCodegen(resetMethod, classScope);
                methods.add(resetMethod);
            }
        }

        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(REF_SCOL, methods.size(), false, offset);
        int count = 0;
        for (CodegenMethod getValue : methods) {
            blocks[count++].expression(localMethod(getValue));
        }

        return method;
    }

    private static CodegenMethod makeMethodTableReset(CodegenClassScope classScope) {
        return makeParentNode(EPTypePremade.VOID.getEPType(), AggregationServiceFactoryCompilerRow.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), NAME_SCOL);
    }

    private static CodegenMethod produceTableAccess(int offset, boolean isGenerateTableEnter, AggregationCodegenTableUpdateType type, AggregationStateFactoryForge[] accessStateFactories, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod method = makeMethodTableAccess(classScope);
        if (!isGenerateTableEnter) {
            method.getBlock().methodThrowUnsupported();
            return method;
        }

        int[] colums = new int[accessStateFactories.length];
        for (int i = 0; i < accessStateFactories.length; i++) {
            colums[i] = offset + i;
        }

        CodegenBlock[] blocks = method.getBlock().switchBlockOptions(REF_SCOL, colums, true);
        for (int i = 0; i < accessStateFactories.length; i++) {
            AggregationStateFactoryForge stateFactoryForge = accessStateFactories[i];
            AggregatorAccess aggregator = stateFactoryForge.getAggregator();

            ExprForgeCodegenSymbol symbols = new ExprForgeCodegenSymbol(false, null);
            CodegenMethod updateMethod = method.makeChildWithScope(EPTypePremade.VOID.getEPType(), stateFactoryForge.getClass(), symbols, classScope).addParam(ExprForgeCodegenNames.PARAMS);
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

    private static CodegenMethod makeMethodTableAccess(CodegenClassScope classScope) {
        return makeParentNode(EPTypePremade.VOID.getEPType(), AggregationServiceFactoryCompilerRow.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), NAME_SCOL).addParam(EventBean.EPTYPEARRAY, NAME_EPS).addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
    }

    private static CodegenMethod produceGetAccessState(int offset, AggregationStateFactoryForge[] accessStateFactories, CodegenClassScope classScope) {
        CodegenMethod method = makeMethodGetAccess(classScope);

        int[] colums = new int[accessStateFactories == null ? 0 : accessStateFactories.length];
        for (int i = 0; i < colums.length; i++) {
            colums[i] = offset + i;
        }

        CodegenBlock[] blocks = method.getBlock().switchBlockOptions(REF_SCOL, colums, true);
        for (int i = 0; i < colums.length; i++) {
            AggregationStateFactoryForge stateFactoryForge = accessStateFactories[i];
            CodegenExpression expr = stateFactoryForge.codegenGetAccessTableState(i + offset, method, classScope);
            blocks[i].blockReturn(expr);
        }

        return method;
    }

    private static CodegenMethod makeMethodGetAccess(CodegenClassScope classScope) {
        return makeParentNode(EPTypePremade.OBJECT.getEPType(), AggregationServiceFactoryCompilerRow.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), NAME_SCOL);
    }

    private static CodegenMethod produceGet(AggregationCodegenGetType getType, AggregationClassAssignment assignment, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod parent = makeMethodGet(getType, classScope);

        // for non-get-value we can simply return null if this has no access aggs
        if (getType != AggregationCodegenGetType.GETVALUE && (assignment.getAccessStateFactories() == null || assignment.getAccessStateFactories().length == 0)) {
            parent.getBlock().methodReturn(constantNull());
            return parent;
        }

        List<CodegenMethod> methods = new ArrayList<>();
        List<Integer> vcols = new ArrayList<>(8);
        for (AggregationVColMethod vcolMethod : assignment.getVcolMethods()) {
            CodegenMethod method = parent.makeChild(getType.getReturnType(), vcolMethod.getForge().getClass(), classScope).addParam(CodegenNamedParam.from(EventBean.EPTYPEARRAY, NAME_EPS, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), ExprForgeCodegenNames.NAME_ISNEWDATA, ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT));
            methods.add(method);

            if (getType == AggregationCodegenGetType.GETVALUE) {
                vcolMethod.getForge().getAggregator().getValueCodegen(method, classScope);
            } else {
                method.getBlock().methodReturn(constantNull()); // method aggs don't do others
            }
            vcols.add(vcolMethod.getVcol());
        }

        for (AggregationVColAccess vcolAccess : assignment.getVcolAccess()) {
            CodegenMethod method = parent.makeChild(getType.getReturnType(), vcolAccess.getAccessorForge().getClass(), classScope).addParam(CodegenNamedParam.from(EventBean.EPTYPEARRAY, NAME_EPS, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), ExprForgeCodegenNames.NAME_ISNEWDATA, ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT));

            AggregationAccessorForgeGetCodegenContext ctx = new AggregationAccessorForgeGetCodegenContext(vcolAccess.getStateNumber(), classScope, vcolAccess.getStateForge(), method, namedMethods);
            switch (getType) {
                case GETVALUE:
                    vcolAccess.getAccessorForge().getValueCodegen(ctx);
                    break;
                case GETEVENTBEAN:
                    vcolAccess.getAccessorForge().getEnumerableEventCodegen(ctx);
                    break;
                case GETCOLLECTIONSCALAR:
                    vcolAccess.getAccessorForge().getEnumerableScalarCodegen(ctx);
                    break;
                case GETCOLLECTIONOFEVENTS:
                    vcolAccess.getAccessorForge().getEnumerableEventsCodegen(ctx);
                    break;
            }
            methods.add(method);
            vcols.add(vcolAccess.getVcol());
        }

        int[] options = IntArrayUtil.toArray(vcols);
        CodegenBlock[] blocks = parent.getBlock().switchBlockOptions(REF_VCOL, options, true);
        int count = 0;
        for (CodegenMethod getValue : methods) {
            blocks[count++].blockReturn(localMethod(getValue, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
        }

        return parent;
    }

    private static CodegenMethod makeMethodGet(AggregationCodegenGetType getType, CodegenClassScope classScope) {
        return makeParentNode(getType.getReturnType(), AggregationServiceFactoryCompilerRow.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(GETPARAMS);
    }

    private static CodegenMethod produceStateUpdate(boolean isGenerate, AggregationCodegenUpdateType updateType, AggregationClassAssignment assignment, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        ExprForgeCodegenSymbol symbols = new ExprForgeCodegenSymbol(true, updateType == AggregationCodegenUpdateType.APPLYENTER);
        CodegenMethod parent = makeParentNode(EPTypePremade.VOID.getEPType(), AggregationServiceFactoryCompilerRow.class, symbols, classScope).addParam(updateType.getParams());

        int count = 0;
        List<CodegenMethod> methods = new ArrayList<>();

        if (assignment.getMethodFactories() != null && isGenerate) {
            for (AggregationForgeFactory factory : assignment.getMethodFactories()) {
                String exprText = null;
                CodegenExpression getValue = null;
                if (classScope.isInstrumented()) {
                    exprText = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(factory.getAggregationExpression());
                    getValue = exprDotMethod(ref("this"), "getValue", constant(count), constantNull(), constantTrue(), constantNull());
                }

                CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), factory.getClass(), classScope);
                methods.add(method);
                switch (updateType) {
                    case APPLYENTER:
                        method.getBlock().apply(instblock(classScope, "qAggNoAccessEnterLeave", constantTrue(), constant(count), getValue, constant(exprText)));
                        factory.getAggregator().applyEvalEnterCodegen(method, symbols, assignment.getMethodForges()[count], classScope);
                        method.getBlock().apply(instblock(classScope, "aAggNoAccessEnterLeave", constantTrue(), constant(count), getValue));
                        break;
                    case APPLYLEAVE:
                        method.getBlock().apply(instblock(classScope, "qAggNoAccessEnterLeave", constantFalse(), constant(count), getValue, constant(exprText)));
                        factory.getAggregator().applyEvalLeaveCodegen(method, symbols, assignment.getMethodForges()[count], classScope);
                        method.getBlock().apply(instblock(classScope, "aAggNoAccessEnterLeave", constantFalse(), constant(count), getValue));
                        break;
                    case CLEAR:
                        factory.getAggregator().clearCodegen(method, classScope);
                        break;
                }
                count++;
            }
        }

        if (assignment.getAccessStateFactories() != null && isGenerate) {
            for (AggregationStateFactoryForge factory : assignment.getAccessStateFactories()) {
                String exprText = null;
                if (classScope.isInstrumented()) {
                    exprText = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(factory.getExpression());
                }

                CodegenMethod method = parent.makeChild(EPTypePremade.VOID.getEPType(), factory.getClass(), classScope);
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

        GETVALUE("getValue", EPTypePremade.OBJECT.getEPType()),
        GETEVENTBEAN("getEnumerableEvent", EventBean.EPTYPE),
        GETCOLLECTIONSCALAR("getEnumerableScalar", EPTypePremade.COLLECTION.getEPType()),
        GETCOLLECTIONOFEVENTS("getEnumerableEvents", EPTypePremade.COLLECTION.getEPType());

        private final String accessorMethodName;
        private final EPTypeClass returnType;

        AggregationCodegenGetType(String accessorMethodName, EPTypeClass returnType) {
            this.accessorMethodName = accessorMethodName;
            this.returnType = returnType;
        }

        public EPTypeClass getReturnType() {
            return returnType;
        }

        public String getAccessorMethodName() {
            return accessorMethodName;
        }
    }

}
