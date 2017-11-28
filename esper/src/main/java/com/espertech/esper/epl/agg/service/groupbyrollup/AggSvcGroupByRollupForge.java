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
package com.espertech.esper.epl.agg.service.groupbyrollup;

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
import com.espertech.esper.epl.agg.codegen.AggregationCodegenRowLevelDesc;
import com.espertech.esper.epl.agg.codegen.AggregationRowGenerated;
import com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames;
import com.espertech.esper.epl.agg.service.common.*;
import com.espertech.esper.epl.agg.service.groupby.AggSvcGroupByForge;
import com.espertech.esper.util.CollectionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames.REF_AGGVISITOR;
import static com.espertech.esper.epl.agg.codegen.AggregationServiceCodegenNames.REF_COLUMN;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.*;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByRollupForge implements AggregationServiceFactoryForge {
    private final static CodegenExpressionRef REF_AGGREGATORSPERGROUP = ref("aggregatorsPerGroup");
    private final static CodegenExpressionRef REF_AGGREGATORTOPGROUP = ref("aggregatorTopGroup");
    private final static CodegenExpressionRef REF_CURRENTROW = ref("currentRow");
    private final static CodegenExpressionRef REF_CURRENTGROUPKEY = ref("currentGroupKey");
    private final static CodegenExpressionRef REF_HASREMOVEDKEY = ref("hasRemovedKey");
    private final static CodegenExpressionRef REF_REMOVEDKEYS = ref("removedKeys");

    protected final AggregationRowStateForgeDesc rowStateForgeDesc;
    protected final boolean isJoin;
    protected final AggregationGroupByRollupDesc rollupDesc;

    public AggSvcGroupByRollupForge(AggregationRowStateForgeDesc rowStateForgeDesc, boolean isJoin, AggregationGroupByRollupDesc rollupDesc) {
        this.rowStateForgeDesc = rowStateForgeDesc;
        this.isJoin = isJoin;
        this.rollupDesc = rollupDesc;
    }

    public AggregationServiceFactory getAggregationServiceFactory(StatementContext stmtContext, boolean isFireAndForget) {
        AggregationRowStateEvalDesc eval = rowStateForgeDesc.toEval(stmtContext, isFireAndForget);
        return new AggSvcGroupByRollupFactory(eval, isJoin, rollupDesc);
    }

    public AggregationCodegenRowLevelDesc getRowLevelDesc() {
        return AggregationCodegenRowLevelDesc.fromTopOnly(rowStateForgeDesc);
    }

    public void rowCtorCodegen(CodegenClassScope classScope, CodegenCtor rowCtor, List<CodegenTypedParam> rowMembers, CodegenNamedMethods namedMethods) {
        AggregationServiceCodegenUtil.generateRefCount(true, namedMethods, rowCtor, rowMembers, classScope);
    }

    public void makeServiceCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONSERVICE, ref("o")));
    }

    public void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope) {
        explicitMembers.add(new CodegenTypedParam(Map[].class, REF_AGGREGATORSPERGROUP.getRef()));
        explicitMembers.add(new CodegenTypedParam(List[].class, REF_REMOVEDKEYS.getRef()));
        ctor.getBlock().assignRef(REF_AGGREGATORSPERGROUP, newArrayByLength(Map.class, constant(rollupDesc.getNumLevelsAggregation())))
                .assignRef(REF_REMOVEDKEYS, newArrayByLength(List.class, constant(rollupDesc.getNumLevelsAggregation())));
        for (int i = 0; i < rollupDesc.getNumLevelsAggregation(); i++) {
            ctor.getBlock().assignArrayElement(REF_AGGREGATORSPERGROUP, constant(i), newInstance(HashMap.class));
            ctor.getBlock().assignArrayElement(REF_REMOVEDKEYS, constant(i), newInstance(ArrayList.class, constant(4)));
        }

        explicitMembers.add(new CodegenTypedParam(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, REF_AGGREGATORTOPGROUP.getRef()));
        ctor.getBlock().assignRef(REF_AGGREGATORTOPGROUP, newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, ref("o")))
                .exprDotMethod(REF_AGGREGATORTOPGROUP, "decreaseRefcount");

        explicitMembers.add(new CodegenTypedParam(AggregationRowGenerated.class, REF_CURRENTROW.getRef()));
        explicitMembers.add(new CodegenTypedParam(Object.class, REF_CURRENTGROUPKEY.getRef()));
        explicitMembers.add(new CodegenTypedParam(boolean.class, REF_HASREMOVEDKEY.getRef()));
    }

    public void getValueCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getValue", AggregationServiceCodegenNames.REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getCollectionOfEventsCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getCollectionOfEvents", REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getEventBeanCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getEventBean", REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getCollectionScalarCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getCollectionScalar", REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void applyEnterCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        applyCodegen(true, method, classScope);
    }

    public void applyLeaveCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        applyCodegen(false, method, classScope);
    }

    public void stopMethodCodegen(AggregationServiceFactoryForge forge, CodegenMethodNode method) {
        // no action
    }

    public void setRemovedCallbackCodegen(CodegenMethodNode method) {
        // no action
    }

    public void setCurrentAccessCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().ifCondition(exprDotMethod(AggregationServiceCodegenNames.REF_ROLLUPLEVEL, "isAggregationTop"))
                .assignRef(REF_CURRENTROW, REF_AGGREGATORTOPGROUP)
                .ifElse()
                .assignRef(REF_CURRENTROW, cast(AggregationRowGenerated.class, exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, exprDotMethod(AggregationServiceCodegenNames.REF_ROLLUPLEVEL, "getAggregationOffset")), "get", AggregationServiceCodegenNames.REF_GROUPKEY)))
                .ifCondition(equalsNull(REF_CURRENTROW))
                .assignRef(REF_CURRENTROW, newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, ref("o")))
                .blockEnd()
                .blockEnd()
                .assignRef(REF_CURRENTGROUPKEY, AggregationServiceCodegenNames.REF_GROUPKEY);
    }

    public void clearResultsCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGREGATORTOPGROUP, "clear");
        for (int i = 0; i < rollupDesc.getNumLevelsAggregation(); i++) {
            method.getBlock().exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, constant(i)), "clear");
        }
    }

    public void acceptCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitAggregations", getGroupKeyCountCodegen(method, classScope), REF_AGGREGATORSPERGROUP, REF_AGGREGATORTOPGROUP);
    }

    public void getGroupKeysCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodThrowUnsupported();
    }

    public void getGroupKeyCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(REF_CURRENTGROUPKEY);
    }

    public void acceptGroupDetailCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitGrouped", getGroupKeyCountCodegen(method, classScope))
                .forEach(Map.class, "anAggregatorsPerGroup", REF_AGGREGATORSPERGROUP)
                .forEach(Map.Entry.class, "entry", exprDotMethod(ref("anAggregatorsPerGroup"), "entrySet"))
                .exprDotMethod(REF_AGGVISITOR, "visitGroup", exprDotMethod(ref("entry"), "getKey"), exprDotMethod(ref("entry"), "getValue"))
                .blockEnd()
                .blockEnd()
                .exprDotMethod(REF_AGGVISITOR, "visitGroup", publicConstValue(CollectionUtil.class, "OBJECTARRAY_EMPTY"), REF_AGGREGATORTOPGROUP);
    }

    public void isGroupedCodegen(CodegenMethodNode method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(constantTrue());
    }

    private CodegenExpression getGroupKeyCountCodegen(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethodNode method = parent.makeChild(int.class, AggSvcGroupByRollupForge.class, classScope);
        method.getBlock().declareVar(int.class, "size", constant(1));
        for (int i = 0; i < rollupDesc.getNumLevelsAggregation(); i++) {
            method.getBlock().assignCompound("size", "+", exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, constant(i)), "size"));
        }
        method.getBlock().methodReturn(ref("size"));
        return localMethod(method);
    }

    private void applyCodegen(boolean enter, CodegenMethodNode method, CodegenClassScope classScope) {
        if (enter) {
            method.getBlock().localMethod(handleRemovedKeysCodegen(method, classScope));
        }

        method.getBlock().declareVar(Object[].class, "groupKeyPerLevel", cast(Object[].class, AggregationServiceCodegenNames.REF_GROUPKEY));
        for (int i = 0; i < rollupDesc.getNumLevels(); i++) {
            AggregationGroupByRollupLevel level = rollupDesc.getLevels()[i];
            String groupKeyName = "groupKey_" + i;
            method.getBlock().declareVar(Object.class, groupKeyName, arrayAtIndex(ref("groupKeyPerLevel"), constant(i)));

            if (level.isAggregationTop()) {
                method.getBlock().assignRef(REF_CURRENTROW, REF_AGGREGATORTOPGROUP)
                        .exprDotMethod(REF_CURRENTROW, enter ? "increaseRefcount" : "decreaseRefcount");
            } else {
                if (enter) {
                    method.getBlock().assignRef(REF_CURRENTROW, cast(AggregationRowGenerated.class, exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, constant(level.getAggregationOffset())), "get", ref(groupKeyName))))
                            .ifCondition(equalsNull(REF_CURRENTROW))
                            .assignRef(REF_CURRENTROW, newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, ref("o")))
                            .exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, constant(level.getAggregationOffset())), "put", ref(groupKeyName), REF_CURRENTROW)
                            .ifElse()
                            .exprDotMethod(REF_CURRENTROW, "increaseRefcount");
                } else {
                    method.getBlock().assignRef(REF_CURRENTROW, cast(AggregationRowGenerated.class, exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, constant(level.getAggregationOffset())), "get", ref(groupKeyName))))
                            .ifCondition(equalsNull(REF_CURRENTROW))
                            .assignRef(REF_CURRENTROW, newInstanceInnerClass(AggregationServiceCodegenNames.CLASSNAME_AGGREGATIONROW_TOP, ref("o")))
                            .exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, constant(level.getAggregationOffset())), "put", ref(groupKeyName), REF_CURRENTROW)
                            .blockEnd()
                            .exprDotMethod(REF_CURRENTROW, "decreaseRefcount");
                }
            }
            method.getBlock().exprDotMethod(REF_CURRENTROW, enter ? "applyEnter" : "applyLeave", REF_EPS, REF_EXPREVALCONTEXT);

            if (!enter && !level.isAggregationTop()) {
                CodegenBlock ifCanDelete = method.getBlock().ifCondition(relational(exprDotMethod(REF_CURRENTROW, "getRefcount"), LE, constant(0)));
                ifCanDelete.assignRef(REF_HASREMOVEDKEY, constantTrue());
                if (!level.isAggregationTop()) {
                    CodegenExpression removedKeyForLevel = arrayAtIndex(REF_REMOVEDKEYS, constant(level.getAggregationOffset()));
                    ifCanDelete.exprDotMethod(removedKeyForLevel, "add", ref(groupKeyName));
                }
            }
        }
    }

    private CodegenMethodNode handleRemovedKeysCodegen(CodegenMethodNode scope, CodegenClassScope classScope) {
        CodegenMethodNode method = scope.makeChild(void.class, AggSvcGroupByForge.class, classScope);
        method.getBlock().ifCondition(not(REF_HASREMOVEDKEY))
                .blockReturnNoValue()
                .assignRef(REF_HASREMOVEDKEY, constantFalse())
                .forLoopIntSimple("i", arrayLength(REF_REMOVEDKEYS))
                .ifCondition(exprDotMethod(arrayAtIndex(REF_REMOVEDKEYS, ref("i")), "isEmpty"))
                .blockContinue()
                .forEach(Object.class, "removedKey", arrayAtIndex(REF_REMOVEDKEYS, ref("i")))
                .exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, ref("i")), "remove", ref("removedKey"))
                .blockEnd()
                .exprDotMethod(arrayAtIndex(REF_REMOVEDKEYS, ref("i")), "clear");
        return method;
    }
}
