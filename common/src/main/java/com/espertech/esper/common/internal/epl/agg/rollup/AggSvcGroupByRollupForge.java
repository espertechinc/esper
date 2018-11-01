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
package com.espertech.esper.common.internal.epl.agg.rollup;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.agg.core.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;
import static com.espertech.esper.common.internal.context.module.EPStatementInitServices.GETAGGREGATIONSERVICEFACTORYSERVICE;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceCodegenNames.REF_AGGVISITOR;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationServiceCodegenNames.REF_COLUMN;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByRollupForge implements AggregationServiceFactoryForgeWMethodGen {
    private final static CodegenExpressionRef REF_AGGREGATORSPERGROUP = ref("aggregatorsPerGroup");
    private final static CodegenExpressionRef REF_AGGREGATORTOPGROUP = ref("aggregatorTopGroup");
    private final static CodegenExpressionRef REF_CURRENTROW = ref("currentRow");
    private final static CodegenExpressionRef REF_CURRENTGROUPKEY = ref("currentGroupKey");
    private final static CodegenExpressionRef REF_HASREMOVEDKEY = ref("hasRemovedKey");
    private final static CodegenExpressionRef REF_REMOVEDKEYS = ref("removedKeys");

    protected final AggregationRowStateForgeDesc rowStateForgeDesc;
    protected final AggregationGroupByRollupDesc rollupDesc;
    protected final ExprNode[] groupByNodes;

    public AggSvcGroupByRollupForge(AggregationRowStateForgeDesc rowStateForgeDesc, AggregationGroupByRollupDesc rollupDesc, ExprNode[] groupByNodes) {
        this.rowStateForgeDesc = rowStateForgeDesc;
        this.rollupDesc = rollupDesc;
        this.groupByNodes = groupByNodes;
    }

    public AggregationCodegenRowLevelDesc getRowLevelDesc() {
        return AggregationCodegenRowLevelDesc.fromTopOnly(rowStateForgeDesc);
    }

    public void providerCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        Class[] groupByTypes = ExprNodeUtilityQuery.getExprResultTypes(groupByNodes);
        method.getBlock()
                .declareVar(AggregationServiceFactory.class, "svcFactory", CodegenExpressionBuilder.newInstance(classNames.getServiceFactory(), ref("this")))
                .declareVar(AggregationRowFactory.class, "rowFactory", CodegenExpressionBuilder.newInstance(classNames.getRowFactoryTop(), ref("this")))
                .declareVar(DataInputOutputSerde.class, "rowSerde", CodegenExpressionBuilder.newInstance(classNames.getRowSerdeTop(), ref("this")))
                .methodReturn(exprDotMethodChain(EPStatementInitServices.REF).add(GETAGGREGATIONSERVICEFACTORYSERVICE).add("groupByRollup",
                        ref("svcFactory"), rollupDesc.codegen(), ref("rowFactory"), rowStateForgeDesc.getUseFlags().toExpression(),
                        ref("rowSerde"), constant(groupByTypes)));
    }

    public void rowCtorCodegen(AggregationRowCtorDesc rowCtorDesc) {
        AggregationServiceCodegenUtil.generateIncidentals(true, false, rowCtorDesc);
    }

    public void makeServiceCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        method.getBlock().methodReturn(CodegenExpressionBuilder.newInstance(classNames.getService(), ref("o")));
    }

    public void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope, AggregationClassNames classNames) {
        explicitMembers.add(new CodegenTypedParam(Map[].class, REF_AGGREGATORSPERGROUP.getRef()));
        explicitMembers.add(new CodegenTypedParam(List[].class, REF_REMOVEDKEYS.getRef()));
        ctor.getBlock().assignRef(REF_AGGREGATORSPERGROUP, newArrayByLength(Map.class, constant(rollupDesc.getNumLevelsAggregation())))
                .assignRef(REF_REMOVEDKEYS, newArrayByLength(List.class, constant(rollupDesc.getNumLevelsAggregation())));
        for (int i = 0; i < rollupDesc.getNumLevelsAggregation(); i++) {
            ctor.getBlock().assignArrayElement(REF_AGGREGATORSPERGROUP, constant(i), newInstance(HashMap.class));
            ctor.getBlock().assignArrayElement(REF_REMOVEDKEYS, constant(i), newInstance(ArrayList.class, constant(4)));
        }

        explicitMembers.add(new CodegenTypedParam(classNames.getRowTop(), REF_AGGREGATORTOPGROUP.getRef()));
        ctor.getBlock().assignRef(REF_AGGREGATORTOPGROUP, CodegenExpressionBuilder.newInstance(classNames.getRowTop()))
                .exprDotMethod(REF_AGGREGATORTOPGROUP, "decreaseRefcount");

        explicitMembers.add(new CodegenTypedParam(AggregationRow.class, REF_CURRENTROW.getRef()));
        explicitMembers.add(new CodegenTypedParam(Object.class, REF_CURRENTGROUPKEY.getRef()));
        explicitMembers.add(new CodegenTypedParam(boolean.class, REF_HASREMOVEDKEY.getRef()));
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getValue", REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getCollectionOfEventsCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getCollectionOfEvents", REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getEventBeanCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getEventBean", REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void getCollectionScalarCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(exprDotMethod(REF_CURRENTROW, "getCollectionScalar", REF_COLUMN, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
    }

    public void applyEnterCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, AggregationClassNames classNames) {
        applyCodegen(true, method, classScope, classNames);
    }

    public void applyLeaveCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, AggregationClassNames classNames) {
        applyCodegen(false, method, classScope, classNames);
    }

    public void stopMethodCodegen(AggregationServiceFactoryForgeWMethodGen forge, CodegenMethod method) {
        // no action
    }

    public void setRemovedCallbackCodegen(CodegenMethod method) {
        // no action
    }

    public void setCurrentAccessCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        method.getBlock().ifCondition(exprDotMethod(AggregationServiceCodegenNames.REF_ROLLUPLEVEL, "isAggregationTop"))
                .assignRef(REF_CURRENTROW, REF_AGGREGATORTOPGROUP)
                .ifElse()
                .assignRef(REF_CURRENTROW, cast(AggregationRow.class, exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, exprDotMethod(AggregationServiceCodegenNames.REF_ROLLUPLEVEL, "getAggregationOffset")), "get", AggregationServiceCodegenNames.REF_GROUPKEY)))
                .ifCondition(equalsNull(REF_CURRENTROW))
                .assignRef(REF_CURRENTROW, CodegenExpressionBuilder.newInstance(classNames.getRowTop()))
                .blockEnd()
                .blockEnd()
                .assignRef(REF_CURRENTGROUPKEY, AggregationServiceCodegenNames.REF_GROUPKEY);
    }

    public void clearResultsCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGREGATORTOPGROUP, "clear");
        for (int i = 0; i < rollupDesc.getNumLevelsAggregation(); i++) {
            method.getBlock().exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, constant(i)), "clear");
        }
    }

    public void acceptCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitAggregations", getGroupKeyCountCodegen(method, classScope), REF_AGGREGATORSPERGROUP, REF_AGGREGATORTOPGROUP);
    }

    public void getGroupKeysCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodThrowUnsupported();
    }

    public void getGroupKeyCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(REF_CURRENTGROUPKEY);
    }

    public void acceptGroupDetailCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(REF_AGGVISITOR, "visitGrouped", getGroupKeyCountCodegen(method, classScope))
                .forEach(Map.class, "anAggregatorsPerGroup", REF_AGGREGATORSPERGROUP)
                .forEach(Map.Entry.class, "entry", exprDotMethod(ref("anAggregatorsPerGroup"), "entrySet"))
                .exprDotMethod(REF_AGGVISITOR, "visitGroup", exprDotMethod(ref("entry"), "getKey"), exprDotMethod(ref("entry"), "getValue"))
                .blockEnd()
                .blockEnd()
                .exprDotMethod(REF_AGGVISITOR, "visitGroup", publicConstValue(CollectionUtil.class, "OBJECTARRAY_EMPTY"), REF_AGGREGATORTOPGROUP);
    }

    public void isGroupedCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(constantTrue());
    }

    public void rowWriteMethodCodegen(CodegenMethod method, int level) {
        method.getBlock().exprDotMethod(ref("output"), "writeInt", ref("row.refcount"));
    }

    public void rowReadMethodCodegen(CodegenMethod method, int level) {
        method.getBlock().assignRef("row.refcount", exprDotMethod(ref("input"), "readInt"));
    }

    private CodegenExpression getGroupKeyCountCodegen(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(int.class, AggSvcGroupByRollupForge.class, classScope);
        method.getBlock().declareVar(int.class, "size", constant(1));
        for (int i = 0; i < rollupDesc.getNumLevelsAggregation(); i++) {
            method.getBlock().assignCompound("size", "+", exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, constant(i)), "size"));
        }
        method.getBlock().methodReturn(ref("size"));
        return localMethod(method);
    }

    private void applyCodegen(boolean enter, CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
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
                    method.getBlock().assignRef(REF_CURRENTROW, cast(AggregationRow.class, exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, constant(level.getAggregationOffset())), "get", ref(groupKeyName))))
                            .ifCondition(equalsNull(REF_CURRENTROW))
                            .assignRef(REF_CURRENTROW, CodegenExpressionBuilder.newInstance(classNames.getRowTop()))
                            .exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, constant(level.getAggregationOffset())), "put", ref(groupKeyName), REF_CURRENTROW)
                            .blockEnd()
                            .exprDotMethod(REF_CURRENTROW, "increaseRefcount");
                } else {
                    method.getBlock().assignRef(REF_CURRENTROW, cast(AggregationRow.class, exprDotMethod(arrayAtIndex(REF_AGGREGATORSPERGROUP, constant(level.getAggregationOffset())), "get", ref(groupKeyName))))
                            .ifCondition(equalsNull(REF_CURRENTROW))
                            .assignRef(REF_CURRENTROW, CodegenExpressionBuilder.newInstance(classNames.getRowTop()))
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

    private CodegenMethod handleRemovedKeysCodegen(CodegenMethod scope, CodegenClassScope classScope) {
        CodegenMethod method = scope.makeChild(void.class, this.getClass(), classScope);
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
