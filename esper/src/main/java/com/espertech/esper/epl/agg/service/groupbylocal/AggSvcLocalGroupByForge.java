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
package com.espertech.esper.epl.agg.service.groupbylocal;

import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.codegen.core.CodegenTypedParam;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.access.AggregationAccessorSlotPairForge;
import com.espertech.esper.epl.agg.codegen.*;
import com.espertech.esper.epl.agg.service.common.AggregationServiceCodegenUtil;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactory;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactoryForge;
import com.espertech.esper.epl.agg.service.groupby.AggSvcGroupByForge;
import com.espertech.esper.epl.agg.service.table.AggSvcGroupAllWTableImpl;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByColumnForge;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByLevelForge;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlan;
import com.espertech.esper.epl.agg.util.AggregationLocalGroupByPlanForge;
import com.espertech.esper.epl.expression.core.ExprForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames.REF_AGGVISITOR;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.*;

public class AggSvcLocalGroupByForge implements AggregationServiceFactoryForge {

    private final static CodegenExpressionRef REF_CURRENTROW = new CodegenExpressionRef("currentRow");
    private final static CodegenExpressionRef REF_AGGREGATORSTOPLEVEL = new CodegenExpressionRef("aggregatorsTopLevel");
    private final static CodegenExpressionRef REF_AGGREGATORSPERLEVELANDGROUP = new CodegenExpressionRef("aggregatorsPerLevelAndGroup");
    private final static CodegenExpressionRef REF_REMOVEDKEYS = ref("removedKeys");

    protected final boolean hasGroupBy;
    protected final boolean join;
    protected final AggregationLocalGroupByPlanForge localGroupByPlan;

    public AggSvcLocalGroupByForge(boolean hasGroupBy, boolean join, AggregationLocalGroupByPlanForge localGroupByPlan) {
        this.hasGroupBy = hasGroupBy;
        this.join = join;
        this.localGroupByPlan = localGroupByPlan;
    }

    public AggregationServiceFactory getAggregationServiceFactory(StatementContext stmtContext, boolean isFireAndForget) {
        AggregationLocalGroupByPlan plan = localGroupByPlan.toEvaluators(stmtContext, isFireAndForget);
        if (!hasGroupBy) {
            return new AggSvcGroupAllLocalGroupByFactory(join, plan);
        } else {
            return new AggSvcGroupByLocalGroupByFactory(join, plan);
        }
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

    public void rowCtorCodegen(CodegenClassScope classScope, CodegenCtor rowCtor, List<CodegenTypedParam> rowMembers, CodegenNamedMethods namedMethods) {
        AggregationServiceCodegenUtil.generateRefCount(true, namedMethods, rowCtor, rowMembers, classScope);
    }

    public void makeServiceCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONSERVICE, ref("o")));
    }

    public void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope) {
        explicitMembers.add(new CodegenTypedParam(Map[].class, REF_AGGREGATORSPERLEVELANDGROUP.getRef()));
        ctor.getBlock().assignRef(REF_AGGREGATORSPERLEVELANDGROUP, newArrayByLength(Map.class, constant(localGroupByPlan.getAllLevelsForges().length)));
        for (int i = 0; i < localGroupByPlan.getAllLevelsForges().length; i++) {
            ctor.getBlock().assignArrayElement(REF_AGGREGATORSPERLEVELANDGROUP, constant(i), newInstance(HashMap.class));
        }

        explicitMembers.add(new CodegenTypedParam(AggregationRowGenerated.class, REF_AGGREGATORSTOPLEVEL.getRef()));
        if (hasGroupBy) {
            explicitMembers.add(new CodegenTypedParam(AggregationRowGenerated.class, REF_CURRENTROW.getRef()));
        }

        explicitMembers.add(new CodegenTypedParam(List.class, REF_REMOVEDKEYS.getRef()));
        ctor.getBlock().assignRef(REF_REMOVEDKEYS, newInstance(ArrayList.class));
    }

    public void getValueCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        getterCodegen("getValue", method, classScope, namedMethods);
    }

    public void getCollectionOfEventsCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        getterCodegen("getCollectionOfEvents", method, classScope, namedMethods);
    }

    public void getEventBeanCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        getterCodegen("getEventBean", method, classScope, namedMethods);
    }

    public void getCollectionScalarCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        getterCodegen("getCollectionScalar", method, classScope, namedMethods);
    }

    public void applyEnterCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        applyCodegen(true, method, classScope, namedMethods);
    }

    public void applyLeaveCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        applyCodegen(false, method, classScope, namedMethods);
    }

    public void stopMethodCodegen(AggregationServiceFactoryForge forge, CodegenMethodNode method) {
        // no code required
    }

    public void setRemovedCallbackCodegen(CodegenMethodNode method) {
        // not applicable
    }

    public void setCurrentAccessCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
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
            method.getBlock().assignRef(REF_CURRENTROW, cast(AggregationRowGenerated.class, exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERLEVELANDGROUP, constant(0)), "get", AggregationServiceCodegenNames.REF_GROUPKEY)))
                    .ifCondition(equalsNull(REF_CURRENTROW))
                    .assignRef(REF_CURRENTROW, newInstanceInnerClass(AggregationRowCodegenUtil.classnameForLevel(indexDefault), ref("o")));
        }
    }

    public void clearResultsCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().ifCondition(notEqualsNull(REF_AGGREGATORSTOPLEVEL))
                .exprDotMethod(REF_AGGREGATORSTOPLEVEL, "clear");
        for (int i = 0; i < localGroupByPlan.getAllLevelsForges().length; i++) {
            method.getBlock().exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERLEVELANDGROUP, constant(i)), "clear");
        }
    }

    public void acceptCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitAggregations", getNumGroupsCodegen(method, classScope), REF_AGGREGATORSTOPLEVEL, REF_AGGREGATORSPERLEVELANDGROUP);
    }

    public void getGroupKeysCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodThrowUnsupported();
    }

    public void getGroupKeyCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(constantNull());
    }

    public void acceptGroupDetailCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitGrouped", getNumGroupsCodegen(method, classScope))
                .ifCondition(notEqualsNull(REF_AGGREGATORSTOPLEVEL))
                .exprDotMethod(REF_AGGVISITOR, "visitGroup", constantNull(), REF_AGGREGATORSTOPLEVEL);

        for (int i = 0; i < localGroupByPlan.getAllLevelsForges().length; i++) {
            method.getBlock().forEach(Map.Entry.class, "entry", exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERLEVELANDGROUP, constant(i)), "entrySet"))
                    .exprDotMethod(REF_AGGVISITOR, "visitGroup", exprDotMethod(ref("entry"), "getKey"), exprDotMethod(ref("entry"), "getValue"));
        }
    }

    public void isGroupedCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(constantTrue());
    }

    private CodegenExpression getNumGroupsCodegen(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethodNode method = parent.makeChild(int.class, AggSvcGroupAllWTableImpl.class, classScope);
        method.getBlock().declareVar(int.class, "size", constant(0))
                .ifCondition(notEqualsNull(REF_AGGREGATORSTOPLEVEL)).increment("size").blockEnd();
        for (int i = 0; i < localGroupByPlan.getAllLevelsForges().length; i++) {
            method.getBlock().assignCompound("size", "+", exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERLEVELANDGROUP, constant(i)), "size"));
        }
        method.getBlock().methodReturn(ref("size"));
        return localMethod(method);
    }

    private void applyCodegen(boolean enter, CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (enter) {
            method.getBlock().localMethod(handleRemovedKeysCodegen(method, classScope));
        }

        if (localGroupByPlan.getOptionalLevelTopForge() != null) {
            method.getBlock().ifCondition(equalsNull(REF_AGGREGATORSTOPLEVEL))
                    .assignRef(REF_AGGREGATORSTOPLEVEL, newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, ref("o")))
                    .blockEnd()
                    .exprDotMethod(REF_AGGREGATORSTOPLEVEL, enter ? "applyEnter" : "applyLeave", REF_EPS, REF_EXPREVALCONTEXT);
        }

        for (int levelNum = 0; levelNum < localGroupByPlan.getAllLevelsForges().length; levelNum++) {
            AggregationLocalGroupByLevelForge level = localGroupByPlan.getAllLevelsForges()[levelNum];
            ExprForge[] partitionForges = level.getPartitionForges();

            String groupKeyName = "groupKeyLvl_" + levelNum;
            String rowName = "row_" + levelNum;
            CodegenExpression groupKeyExp = hasGroupBy && level.isDefaultLevel() ? AggregationServiceCodegenNames.REF_GROUPKEY : localMethod(AggregationServiceCodegenUtil.computeMultiKeyCodegen(levelNum, partitionForges, classScope, namedMethods), REF_EPS, constantTrue(), REF_EXPREVALCONTEXT);
            method.getBlock().declareVar(Object.class, groupKeyName, groupKeyExp)
                    .declareVar(AggregationRowGenerated.class, rowName, cast(AggregationRowGenerated.class, exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERLEVELANDGROUP, constant(levelNum)), "get", ref(groupKeyName))))
                    .ifCondition(equalsNull(ref(rowName)))
                    .assignRef(rowName, newInstanceInnerClass(AggregationRowCodegenUtil.classnameForLevel(levelNum), ref("o")))
                    .exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERLEVELANDGROUP, constant(levelNum)), "put", ref(groupKeyName), ref(rowName))
                    .ifElse()
                    .exprDotMethod(ref(rowName), enter ? "increaseRefcount" : "decreaseRefcount")
                    .blockEnd()
                    .exprDotMethod(ref(rowName), enter ? "applyEnter" : "applyLeave", REF_EPS, REF_EXPREVALCONTEXT);

            if (!enter) {
                method.getBlock().ifCondition(relational(exprDotMethod(ref(rowName), "getRefcount"), LE, constant(0)))
                        .exprDotMethod(REF_REMOVEDKEYS, "add", newInstance(AggSvcLocalGroupLevelKeyPair.class, constant(levelNum), ref(groupKeyName)));
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
        return new AggregationCodegenRowDetailDesc(new AggregationCodegenRowDetailStateDesc(level.getMethodForges(), level.getMethodFactories(), level.getAccessStateForges()), pairs);
    }

    private int accessorIndex(AggregationAccessorSlotPairForge[] accessAccessors, AggregationAccessorSlotPairForge pair) {
        for (int i = 0; i < accessAccessors.length; i++) {
            if (accessAccessors[i] == pair) {
                return i;
            }
        }
        throw new IllegalStateException();
    }

    private void getterCodegen(String methodName, CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        AggregationCodegenRowLevelDesc rowLevelDesc = getRowLevelDesc();

        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(AggregationServiceCodegenNames.NAME_COLUMN, localGroupByPlan.getColumnsForges().length, true);
        for (int i = 0; i < blocks.length; i++) {
            AggregationLocalGroupByColumnForge col = localGroupByPlan.getColumnsForges()[i];

            if (hasGroupBy && col.isDefaultGroupLevel()) {
                AggregationCodegenRowDetailDesc levelDesc = rowLevelDesc.getOptionalAdditionalRows()[col.getLevelNum()];
                int num = col.isMethodAgg() ? col.getMethodOffset() : levelDesc.getStateDesc().getMethodFactories().length + accessorIndex(levelDesc.getAccessAccessors(), col.getPair());
                blocks[i].blockReturn(exprDotMethod(REF_CURRENTROW, methodName, constant(num), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
            } else if (col.getLevelNum() == -1) {
                AggregationCodegenRowDetailDesc levelDesc = rowLevelDesc.getOptionalTopRow();
                int num = col.isMethodAgg() ? col.getMethodOffset() : levelDesc.getStateDesc().getMethodFactories().length + accessorIndex(levelDesc.getAccessAccessors(), col.getPair());
                blocks[i].blockReturn(exprDotMethod(REF_AGGREGATORSTOPLEVEL, methodName, constant(num), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
            } else {
                AggregationCodegenRowDetailDesc levelDesc = rowLevelDesc.getOptionalAdditionalRows()[col.getLevelNum()];
                int num = col.isMethodAgg() ? col.getMethodOffset() : levelDesc.getStateDesc().getMethodFactories().length + accessorIndex(levelDesc.getAccessAccessors(), col.getPair());
                blocks[i].declareVar(Object.class, "groupByKey", localMethod(AggregationServiceCodegenUtil.computeMultiKeyCodegen(col.getLevelNum(), col.getPartitionForges(), classScope, namedMethods), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT))
                        .declareVar(AggregationRowGenerated.class, "row", cast(AggregationRowGenerated.class, exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERLEVELANDGROUP, constant(col.getLevelNum())), "get", ref("groupByKey"))))
                        .blockReturn(exprDotMethod(ref("row"), methodName, constant(num), REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
            }
        }
    }

    private CodegenMethodNode handleRemovedKeysCodegen(CodegenMethodNode scope, CodegenClassScope classScope) {
        CodegenMethodNode method = scope.makeChild(void.class, AggSvcGroupByForge.class, classScope);
        method.getBlock().ifCondition(not(exprDotMethod(REF_REMOVEDKEYS, "isEmpty")))
                .forEach(AggSvcLocalGroupLevelKeyPair.class, "removedKey", REF_REMOVEDKEYS)
                .exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERLEVELANDGROUP, exprDotMethod(ref("removedKey"), "getLevel")), "remove", exprDotMethod(ref("removedKey"), "getKey"))
                .blockEnd()
                .exprDotMethod(REF_REMOVEDKEYS, "clear");
        return method;
    }
}
