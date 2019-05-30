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
package com.espertech.esper.common.internal.epl.agg.groupbylocal;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.agg.core.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.common.internal.context.module.EPStatementInitServices.GETAGGREGATIONSERVICEFACTORYSERVICE;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceCodegenNames.REF_AGGVISITOR;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceCodegenNames.REF_GROUPKEY;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode.instblock;

public class AggSvcLocalGroupByForge implements AggregationServiceFactoryForgeWMethodGen {

    private final static CodegenExpressionMember MEMBER_CURRENTROW = member("currentRow");
    private final static CodegenExpressionMember MEMBER_AGGREGATORSTOPLEVEL = member("aggregatorsTopLevel");
    private final static CodegenExpressionMember MEMBER_AGGREGATORSPERLEVELANDGROUP = member("aggregatorsPerLevelAndGroup");
    private final static CodegenExpressionMember MEMBER_REMOVEDKEYS = member("removedKeys");

    protected final boolean hasGroupBy;
    protected final AggregationLocalGroupByPlanForge localGroupByPlan;
    protected final AggregationUseFlags useFlags;

    public AggSvcLocalGroupByForge(boolean hasGroupBy, AggregationLocalGroupByPlanForge localGroupByPlan, AggregationUseFlags useFlags) {
        this.hasGroupBy = hasGroupBy;
        this.localGroupByPlan = localGroupByPlan;
        this.useFlags = useFlags;
    }

    public AggregationCodegenRowLevelDesc getRowLevelDesc() {
        AggregationCodegenRowDetailDesc top = null;
        if (localGroupByPlan.getOptionalLevelTopForge() != null) {
            top = mapDesc(true, -1, localGroupByPlan.getColumnsForges(), localGroupByPlan.getOptionalLevelTopForge());
        }
        AggregationCodegenRowDetailDesc[] additional = null;
        if (localGroupByPlan.getAllLevelsForges() != null) {
            additional = new AggregationCodegenRowDetailDesc[localGroupByPlan.getAllLevelsForges().length];
            for (int i = 0; i < localGroupByPlan.getAllLevelsForges().length; i++) {
                additional[i] = mapDesc(false, i, localGroupByPlan.getColumnsForges(), localGroupByPlan.getAllLevelsForges()[i]);
            }
        }
        return new AggregationCodegenRowLevelDesc(top, additional);
    }

    public void rowCtorCodegen(AggregationRowCtorDesc rowCtorDesc) {
        AggregationServiceCodegenUtil.generateIncidentals(true, false, rowCtorDesc);
    }

    public void providerCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        method.getBlock().declareVar(AggregationLocalGroupByLevel.class, "optionalTop", constantNull());
        if (localGroupByPlan.getOptionalLevelTopForge() != null) {
            method.getBlock().assignRef("optionalTop", localGroupByPlan.getOptionalLevelTopForge().toExpression(classNames.getRowFactoryTop(), classNames.getRowSerdeTop(), constantNull(), method, classScope));
        }

        int numLevels = localGroupByPlan.getAllLevelsForges().length;
        method.getBlock().declareVar(AggregationLocalGroupByLevel[].class, "levels", newArrayByLength(AggregationLocalGroupByLevel.class, constant(numLevels)));
        for (int i = 0; i < numLevels; i++) {
            AggregationLocalGroupByLevelForge forge = localGroupByPlan.getAllLevelsForges()[i];
            CodegenExpression eval = MultiKeyCodegen.codegenExprEvaluatorMayMultikey(forge.getPartitionForges(), null, forge.getPartitionMKClasses(), method, classScope);
            method.getBlock().assignArrayElement("levels", constant(i), localGroupByPlan.getAllLevelsForges()[i].toExpression(
                classNames.getRowFactoryPerLevel(i), classNames.getRowSerdePerLevel(i), eval, method, classScope));
        }

        method.getBlock().declareVar(AggregationLocalGroupByColumn[].class, "columns", newArrayByLength(AggregationLocalGroupByColumn.class, constant(localGroupByPlan.getColumnsForges().length)));
        AggregationCodegenRowLevelDesc rowLevelDesc = getRowLevelDesc();
        for (int i = 0; i < localGroupByPlan.getColumnsForges().length; i++) {

            AggregationLocalGroupByColumnForge col = localGroupByPlan.getColumnsForges()[i];
            int fieldNum;
            if (hasGroupBy && col.isDefaultGroupLevel()) {
                AggregationCodegenRowDetailDesc levelDesc = rowLevelDesc.getOptionalAdditionalRows()[col.getLevelNum()];
                fieldNum = getRowFieldNum(col, levelDesc);
            } else if (col.getLevelNum() == -1) {
                AggregationCodegenRowDetailDesc levelDesc = rowLevelDesc.getOptionalTopRow();
                fieldNum = getRowFieldNum(col, levelDesc);
            } else {
                AggregationCodegenRowDetailDesc levelDesc = rowLevelDesc.getOptionalAdditionalRows()[col.getLevelNum()];
                fieldNum = getRowFieldNum(col, levelDesc);
            }

            method.getBlock().assignArrayElement("columns", constant(i), localGroupByPlan.getColumnsForges()[i].toExpression(fieldNum));
        }

        method.getBlock()
            .declareVar(AggregationServiceFactory.class, "svcFactory", CodegenExpressionBuilder.newInstance(classNames.getServiceFactory(), ref("this")))
            .methodReturn(exprDotMethodChain(EPStatementInitServices.REF).add(GETAGGREGATIONSERVICEFACTORYSERVICE).add("groupLocalGroupBy",
                ref("svcFactory"), useFlags.toExpression(), constant(hasGroupBy),
                ref("optionalTop"), ref("levels"), ref("columns")));
    }

    public void makeServiceCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        method.getBlock().methodReturn(CodegenExpressionBuilder.newInstance(classNames.getService(), ref("o")));
    }

    public void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope, AggregationClassNames classNames) {
        explicitMembers.add(new CodegenTypedParam(Map[].class, MEMBER_AGGREGATORSPERLEVELANDGROUP.getRef()));
        ctor.getBlock().assignRef(MEMBER_AGGREGATORSPERLEVELANDGROUP, newArrayByLength(Map.class, constant(localGroupByPlan.getAllLevelsForges().length)));
        for (int i = 0; i < localGroupByPlan.getAllLevelsForges().length; i++) {
            ctor.getBlock().assignArrayElement(MEMBER_AGGREGATORSPERLEVELANDGROUP, constant(i), newInstance(HashMap.class));
        }

        explicitMembers.add(new CodegenTypedParam(AggregationRow.class, MEMBER_AGGREGATORSTOPLEVEL.getRef()));
        if (hasGroupBy) {
            explicitMembers.add(new CodegenTypedParam(AggregationRow.class, MEMBER_CURRENTROW.getRef()));
        }

        explicitMembers.add(new CodegenTypedParam(List.class, MEMBER_REMOVEDKEYS.getRef()));
        ctor.getBlock().assignRef(MEMBER_REMOVEDKEYS, newInstance(ArrayList.class));
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        getterCodegen("getValue", method, classScope, namedMethods);
    }

    public void getCollectionOfEventsCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        getterCodegen("getCollectionOfEvents", method, classScope, namedMethods);
    }

    public void getEventBeanCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        getterCodegen("getEventBean", method, classScope, namedMethods);
    }

    public void getCollectionScalarCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        getterCodegen("getCollectionScalar", method, classScope, namedMethods);
    }

    public void applyEnterCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, AggregationClassNames classNames) {
        method.getBlock().apply(instblock(classScope, "qAggregationGroupedApplyEnterLeave", constantTrue(), constant(-1), constant(-1), REF_GROUPKEY));
        applyCodegen(true, method, classScope, namedMethods, classNames);
        method.getBlock().apply(instblock(classScope, "aAggregationGroupedApplyEnterLeave", constantTrue()));
    }

    public void applyLeaveCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, AggregationClassNames classNames) {
        method.getBlock().apply(instblock(classScope, "qAggregationGroupedApplyEnterLeave", constantFalse(), constant(-1), constant(-1), REF_GROUPKEY));
        applyCodegen(false, method, classScope, namedMethods, classNames);
        method.getBlock().apply(instblock(classScope, "aAggregationGroupedApplyEnterLeave", constantFalse()));
    }

    public void stopMethodCodegen(AggregationServiceFactoryForgeWMethodGen forge, CodegenMethod method) {
        // no code required
    }

    public void setRemovedCallbackCodegen(CodegenMethod method) {
        // not applicable
    }

    public void rowWriteMethodCodegen(CodegenMethod method, int level) {
        if (level != -1) {
            method.getBlock().exprDotMethod(ref("output"), "writeInt", ref("row.refcount"));
        }
    }

    public void rowReadMethodCodegen(CodegenMethod method, int level) {
        if (level != -1) {
            method.getBlock().assignRef("row.refcount", exprDotMethod(ref("input"), "readInt"));
        }
    }

    public void setCurrentAccessCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        if (!hasGroupBy) {
            // not applicable
        } else {
            if (!localGroupByPlan.getAllLevelsForges()[0].isDefaultLevel()) {
                return;
            }
            int indexDefault = -1;
            for (int i = 0; i < localGroupByPlan.getAllLevelsForges().length; i++) {
                if (localGroupByPlan.getAllLevelsForges()[i].isDefaultLevel()) {
                    indexDefault = i;
                }
            }
            method.getBlock().assignRef(MEMBER_CURRENTROW, cast(AggregationRow.class, exprDotMethod(arrayAtIndex(MEMBER_AGGREGATORSPERLEVELANDGROUP, constant(0)), "get", AggregationServiceCodegenNames.REF_GROUPKEY)))
                .ifCondition(equalsNull(MEMBER_CURRENTROW))
                .assignRef(MEMBER_CURRENTROW, CodegenExpressionBuilder.newInstance(classNames.getRowPerLevel(indexDefault)));
        }
    }

    public void clearResultsCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().ifCondition(notEqualsNull(MEMBER_AGGREGATORSTOPLEVEL))
            .exprDotMethod(MEMBER_AGGREGATORSTOPLEVEL, "clear");
        for (int i = 0; i < localGroupByPlan.getAllLevelsForges().length; i++) {
            method.getBlock().exprDotMethod(arrayAtIndex(MEMBER_AGGREGATORSPERLEVELANDGROUP, constant(i)), "clear");
        }
    }

    public void acceptCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitAggregations", getNumGroupsCodegen(method, classScope), MEMBER_AGGREGATORSTOPLEVEL, MEMBER_AGGREGATORSPERLEVELANDGROUP);
    }

    public void getGroupKeysCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodThrowUnsupported();
    }

    public void getGroupKeyCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(constantNull());
    }

    public void acceptGroupDetailCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitGrouped", getNumGroupsCodegen(method, classScope))
            .ifCondition(notEqualsNull(MEMBER_AGGREGATORSTOPLEVEL))
            .exprDotMethod(REF_AGGVISITOR, "visitGroup", constantNull(), MEMBER_AGGREGATORSTOPLEVEL);

        for (int i = 0; i < localGroupByPlan.getAllLevelsForges().length; i++) {
            method.getBlock().forEach(Map.Entry.class, "entry", exprDotMethod(arrayAtIndex(MEMBER_AGGREGATORSPERLEVELANDGROUP, constant(i)), "entrySet"))
                .exprDotMethod(REF_AGGVISITOR, "visitGroup", exprDotMethod(ref("entry"), "getKey"), exprDotMethod(ref("entry"), "getValue"));
        }
    }

    public void isGroupedCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(constantTrue());
    }

    public void getRowCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodThrowUnsupported();
    }

    private CodegenExpression getNumGroupsCodegen(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(int.class, this.getClass(), classScope);
        method.getBlock().declareVar(int.class, "size", constant(0))
            .ifCondition(notEqualsNull(MEMBER_AGGREGATORSTOPLEVEL)).incrementRef("size").blockEnd();
        for (int i = 0; i < localGroupByPlan.getAllLevelsForges().length; i++) {
            method.getBlock().assignCompound("size", "+", exprDotMethod(arrayAtIndex(MEMBER_AGGREGATORSPERLEVELANDGROUP, constant(i)), "size"));
        }
        method.getBlock().methodReturn(ref("size"));
        return localMethod(method);
    }

    private void applyCodegen(boolean enter, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, AggregationClassNames classNames) {
        if (enter) {
            method.getBlock().localMethod(handleRemovedKeysCodegen(method, classScope));
        }

        if (localGroupByPlan.getOptionalLevelTopForge() != null) {
            method.getBlock().ifCondition(equalsNull(MEMBER_AGGREGATORSTOPLEVEL))
                .assignRef(MEMBER_AGGREGATORSTOPLEVEL, CodegenExpressionBuilder.newInstance(classNames.getRowTop()))
                .blockEnd()
                .exprDotMethod(MEMBER_AGGREGATORSTOPLEVEL, enter ? "applyEnter" : "applyLeave", REF_EPS, REF_EXPREVALCONTEXT);
        }

        for (int levelNum = 0; levelNum < localGroupByPlan.getAllLevelsForges().length; levelNum++) {
            AggregationLocalGroupByLevelForge level = localGroupByPlan.getAllLevelsForges()[levelNum];
            ExprNode[] partitionForges = level.getPartitionForges();

            String groupKeyName = "groupKeyLvl_" + levelNum;
            String rowName = "row_" + levelNum;
            CodegenExpression groupKeyExp = hasGroupBy && level.isDefaultLevel() ? AggregationServiceCodegenNames.REF_GROUPKEY : localMethod(AggregationServiceCodegenUtil.computeMultiKeyCodegen(levelNum, partitionForges, level.getPartitionMKClasses(), classScope, namedMethods), REF_EPS, constantTrue(), REF_EXPREVALCONTEXT);
            method.getBlock().declareVar(Object.class, groupKeyName, groupKeyExp)
                .declareVar(AggregationRow.class, rowName, cast(AggregationRow.class, exprDotMethod(arrayAtIndex(MEMBER_AGGREGATORSPERLEVELANDGROUP, constant(levelNum)), "get", ref(groupKeyName))))
                .ifCondition(equalsNull(ref(rowName)))
                .assignRef(rowName, CodegenExpressionBuilder.newInstance(classNames.getRowPerLevel(levelNum)))
                .exprDotMethod(arrayAtIndex(MEMBER_AGGREGATORSPERLEVELANDGROUP, constant(levelNum)), "put", ref(groupKeyName), ref(rowName))
                .blockEnd()
                .exprDotMethod(ref(rowName), enter ? "increaseRefcount" : "decreaseRefcount")
                .exprDotMethod(ref(rowName), enter ? "applyEnter" : "applyLeave", REF_EPS, REF_EXPREVALCONTEXT);

            if (!enter) {
                method.getBlock().ifCondition(relational(exprDotMethod(ref(rowName), "getRefcount"), LE, constant(0)))
                    .exprDotMethod(MEMBER_REMOVEDKEYS, "add", newInstance(AggSvcLocalGroupLevelKeyPair.class, constant(levelNum), ref(groupKeyName)));
            }
        }
    }

    private AggregationCodegenRowDetailDesc mapDesc(boolean top, int levelNum, AggregationLocalGroupByColumnForge[] columns, AggregationLocalGroupByLevelForge level) {
        List<AggregationAccessorSlotPairForge> accessAccessors = new ArrayList<>(4);
        for (int i = 0; i < columns.length; i++) {
            AggregationLocalGroupByColumnForge column = columns[i];
            if (column.getPair() != null) {
                if (top && column.isDefaultGroupLevel()) {
                    accessAccessors.add(column.getPair());
                } else if (column.getLevelNum() == levelNum) {
                    accessAccessors.add(column.getPair());
                }
            }
        }
        AggregationAccessorSlotPairForge[] pairs = accessAccessors.toArray(new AggregationAccessorSlotPairForge[accessAccessors.size()]);
        return new AggregationCodegenRowDetailDesc(new AggregationCodegenRowDetailStateDesc(level.getMethodForges(), level.getMethodFactories(), level.getAccessStateForges()), pairs, level.getPartitionMKClasses());
    }

    private int accessorIndex(AggregationAccessorSlotPairForge[] accessAccessors, AggregationAccessorSlotPairForge pair) {
        for (int i = 0; i < accessAccessors.length; i++) {
            if (accessAccessors[i] == pair) {
                return i;
            }
        }
        throw new IllegalStateException();
    }

    private void getterCodegen(String methodName, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        AggregationCodegenRowLevelDesc rowLevelDesc = getRowLevelDesc();

        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(AggregationServiceCodegenNames.REF_COLUMN, localGroupByPlan.getColumnsForges().length, true);
        for (int i = 0; i < blocks.length; i++) {
            AggregationLocalGroupByColumnForge col = localGroupByPlan.getColumnsForges()[i];

            if (hasGroupBy && col.isDefaultGroupLevel()) {
                AggregationCodegenRowDetailDesc levelDesc = rowLevelDesc.getOptionalAdditionalRows()[col.getLevelNum()];
                int num = getRowFieldNum(col, levelDesc);
                blocks[i].blockReturn(exprDotMethod(MEMBER_CURRENTROW, methodName, constant(num), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
            } else if (col.getLevelNum() == -1) {
                AggregationCodegenRowDetailDesc levelDesc = rowLevelDesc.getOptionalTopRow();
                int num = getRowFieldNum(col, levelDesc);
                blocks[i].blockReturn(exprDotMethod(MEMBER_AGGREGATORSTOPLEVEL, methodName, constant(num), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
            } else {
                AggregationCodegenRowDetailDesc levelDesc = rowLevelDesc.getOptionalAdditionalRows()[col.getLevelNum()];
                int num = getRowFieldNum(col, levelDesc);
                blocks[i].declareVar(Object.class, "groupByKey", localMethod(AggregationServiceCodegenUtil.computeMultiKeyCodegen(col.getLevelNum(), col.getPartitionForges(), levelDesc.getMultiKeyClassRef(), classScope, namedMethods), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT))
                    .declareVar(AggregationRow.class, "row", cast(AggregationRow.class, exprDotMethod(arrayAtIndex(MEMBER_AGGREGATORSPERLEVELANDGROUP, constant(col.getLevelNum())), "get", ref("groupByKey"))))
                    .blockReturn(exprDotMethod(ref("row"), methodName, constant(num), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
            }
        }
    }

    private int getRowFieldNum(AggregationLocalGroupByColumnForge col, AggregationCodegenRowDetailDesc levelDesc) {
        return col.isMethodAgg() ? col.getMethodOffset() : levelDesc.getStateDesc().getMethodFactories().length + accessorIndex(levelDesc.getAccessAccessors(), col.getPair());
    }

    private CodegenMethod handleRemovedKeysCodegen(CodegenMethod scope, CodegenClassScope classScope) {
        CodegenMethod method = scope.makeChild(void.class, this.getClass(), classScope);
        method.getBlock().ifCondition(not(exprDotMethod(MEMBER_REMOVEDKEYS, "isEmpty")))
            .forEach(AggSvcLocalGroupLevelKeyPair.class, "removedKey", MEMBER_REMOVEDKEYS)
            .exprDotMethod(arrayAtIndex(MEMBER_AGGREGATORSPERLEVELANDGROUP, exprDotMethod(ref("removedKey"), "getLevel")), "remove", exprDotMethod(ref("removedKey"), "getKey"))
            .blockEnd()
            .exprDotMethod(MEMBER_REMOVEDKEYS, "clear");
        return method;
    }
}
