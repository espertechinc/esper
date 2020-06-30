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
package com.espertech.esper.common.internal.epl.resultset.order;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.HashableMultiKey;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.agg.rollup.GroupByRollupKey;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil;

import java.util.Arrays;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_ISNEWDATA;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.NAME_ISNEWDATA;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.order.OrderByProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode.instblock;

/**
 * An order-by processor that sorts events according to the expressions
 * in the order_by clause.
 */
public class OrderByProcessorImpl {

    public static void getSortKeyCodegen(OrderByProcessorForgeImpl forge, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        String[] expressions = null;
        boolean[] descending = null;
        if (classScope.isInstrumented()) {
            expressions = forge.getExpressionTexts();
            descending = forge.getDescendingFlags();
        }
        method.getBlock().apply(instblock(classScope, "qOrderBy", REF_EPS, constant(expressions), constant(descending)));
        CodegenMethod getSortKey = generateOrderKeyCodegen("getSortKeyInternal", forge.getOrderBy(), classScope, namedMethods);
        method.getBlock()
                .declareVar(EPTypePremade.OBJECT.getEPType(), "key", localMethod(getSortKey, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT))
                .apply(instblock(classScope, "aOrderBy", ref("key")))
                .methodReturn(ref("key"));
    }

    public static void getSortKeyRollupCodegen(OrderByProcessorForgeImpl forge, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "num", exprDotMethod(REF_ORDERROLLUPLEVEL, "getLevelNumber"));
        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(ref("num"), forge.getOrderByRollup().length, true);
        for (int i = 0; i < blocks.length; i++) {
            CodegenMethod getSortKey = generateOrderKeyCodegen("getSortKeyInternal_" + i, forge.getOrderByRollup()[i], classScope, namedMethods);
            blocks[i].blockReturn(localMethod(getSortKey, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
        }
    }

    static void sortPlainCodegen(OrderByProcessorForgeImpl forge, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod node = sortWGroupKeysInternalCodegen(forge, classScope, namedMethods);
        method.getBlock().ifCondition(or(equalsNull(REF_OUTGOINGEVENTS), relational(arrayLength(REF_OUTGOINGEVENTS), LT, constant(2))))
                .blockReturn(REF_OUTGOINGEVENTS);

        method.getBlock().methodReturn(localMethod(node, REF_OUTGOINGEVENTS, REF_GENERATINGEVENTS, constantNull(), REF_ISNEWDATA, REF_EXPREVALCONTEXT, MEMBER_AGGREGATIONSVC));
    }

    static void sortRollupCodegen(OrderByProcessorForgeImpl forge, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod createSortPropertiesWRollup = createSortPropertiesWRollupCodegen(forge, classScope, namedMethods);
        CodegenExpression comparator = classScope.addOrGetFieldSharable(forge.getComparator());
        method.getBlock().declareVar(EPTypePremade.LIST.getEPType(), "sortValuesMultiKeys", localMethod(createSortPropertiesWRollup, REF_ORDERCURRENTGENERATORS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT, MEMBER_AGGREGATIONSVC))
                .methodReturn(staticMethod(OrderByProcessorUtil.class, "sortGivenOutgoingAndSortKeys", REF_OUTGOINGEVENTS, ref("sortValuesMultiKeys"), comparator));
    }

    static void sortWGroupKeysCodegen(OrderByProcessorForgeImpl forge, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod sortWGroupKeysInternal = sortWGroupKeysInternalCodegen(forge, classScope, namedMethods);
        method.getBlock().ifCondition(or(equalsNull(REF_OUTGOINGEVENTS), relational(arrayLength(REF_OUTGOINGEVENTS), LT, constant(2))))
                .blockReturn(REF_OUTGOINGEVENTS)
                .methodReturn(localMethod(sortWGroupKeysInternal, REF_OUTGOINGEVENTS, REF_GENERATINGEVENTS, REF_ORDERGROUPBYKEYS, REF_ISNEWDATA, REF_EXPREVALCONTEXT, MEMBER_AGGREGATIONSVC));
    }

    private static CodegenMethod sortWGroupKeysInternalCodegen(OrderByProcessorForgeImpl forge, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenMethod createSortProperties = createSortPropertiesCodegen(forge, classScope, namedMethods);
        CodegenExpression comparator = classScope.addOrGetFieldSharable(forge.getComparator());
        Consumer<CodegenMethod> code = method -> {
            method.getBlock().declareVar(EPTypePremade.LIST.getEPType(), "sortValuesMultiKeys", localMethod(createSortProperties, REF_GENERATINGEVENTS, ref("groupByKeys"), REF_ISNEWDATA, REF_EXPREVALCONTEXT, MEMBER_AGGREGATIONSVC))
                    .methodReturn(staticMethod(OrderByProcessorUtil.class, "sortGivenOutgoingAndSortKeys", REF_OUTGOINGEVENTS, ref("sortValuesMultiKeys"), comparator));
        };
        return namedMethods.addMethod(EventBean.EPTYPEARRAY, "sortWGroupKeysInternal", CodegenNamedParam.from(EventBean.EPTYPEARRAY, REF_OUTGOINGEVENTS.getRef(), EventBean.EPTYPEARRAYARRAY, REF_GENERATINGEVENTS.getRef(),
                EPTypePremade.OBJECTARRAY.getEPType(), "groupByKeys", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), REF_ISNEWDATA.getRef(), ExprEvaluatorContext.EPTYPE, REF_EXPREVALCONTEXT.getRef(), AggregationService.EPTYPE, MEMBER_AGGREGATIONSVC.getRef()), OrderByProcessorImpl.class, classScope, code);
    }

    private static CodegenMethod createSortPropertiesCodegen(OrderByProcessorForgeImpl forge, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        Consumer<CodegenMethod> code = method -> {

            String[] expressions = null;
            boolean[] descending = null;
            if (classScope.isInstrumented()) {
                expressions = forge.getExpressionTexts();
                descending = forge.getDescendingFlags();
            }

            method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "sortProperties", newArrayByLength(EPTypePremade.OBJECT.getEPType(), arrayLength(REF_GENERATINGEVENTS)));

            OrderByElementForge[] elements = forge.getOrderBy();
            CodegenBlock forEach = method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0))
                    .forEach(EventBean.EPTYPEARRAY, "eventsPerStream", REF_GENERATINGEVENTS);

            if (forge.isNeedsGroupByKeys()) {
                forEach.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", arrayAtIndex(ref("groupByKeys"), ref("count")), exprDotMethod(REF_EXPREVALCONTEXT, "getAgentInstanceId"), constantNull());
            }

            forEach.apply(instblock(classScope, "qOrderBy", ref("eventsPerStream"), constant(expressions), constant(descending)));
            if (elements.length == 1) {
                forEach.assignArrayElement("sortProperties", ref("count"), localMethod(CodegenLegoMethodExpression.codegenExpression(elements[0].getExprNode().getForge(), method, classScope), ref("eventsPerStream"), REF_ISNEWDATA, REF_EXPREVALCONTEXT));
            } else {
                forEach.declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "values", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(forge.getOrderBy().length)));
                for (int i = 0; i < forge.getOrderBy().length; i++) {
                    forEach.assignArrayElement("values", constant(i), localMethod(CodegenLegoMethodExpression.codegenExpression(elements[i].getExprNode().getForge(), method, classScope), ref("eventsPerStream"), REF_ISNEWDATA, REF_EXPREVALCONTEXT));
                }
                forEach.assignArrayElement("sortProperties", ref("count"), newInstance(HashableMultiKey.EPTYPE, ref("values")));
            }
            forEach.apply(instblock(classScope, "aOrderBy", ref("sortProperties")))
                    .incrementRef("count");
            method.getBlock().methodReturn(staticMethod(Arrays.class, "asList", ref("sortProperties")));
        };
        return namedMethods.addMethod(EPTypePremade.LIST.getEPType(), "createSortProperties", CodegenNamedParam.from(EventBean.EPTYPEARRAYARRAY, REF_GENERATINGEVENTS.getRef(),
                EPTypePremade.OBJECTARRAY.getEPType(), "groupByKeys", EPTypePremade.BOOLEANPRIMITIVE.getEPType(), REF_ISNEWDATA.getRef(), ExprEvaluatorContext.EPTYPE, REF_EXPREVALCONTEXT.getRef(), AggregationService.EPTYPE, MEMBER_AGGREGATIONSVC.getRef()), OrderByProcessorImpl.class, classScope, code);
    }

    static void sortWOrderKeysCodegen(OrderByProcessorForgeImpl forge, CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpression comparator = classScope.addOrGetFieldSharable(forge.getComparator());
        method.getBlock().methodReturn(staticMethod(OrderByProcessorUtil.class, "sortWOrderKeys", REF_OUTGOINGEVENTS, REF_ORDERKEYS, comparator));
    }

    static void sortTwoKeysCodegen(OrderByProcessorForgeImpl forge, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        CodegenExpression comparator = classScope.addOrGetFieldSharable(forge.getComparator());
        CodegenExpression compare = exprDotMethod(comparator, "compare", REF_ORDERFIRSTSORTKEY, REF_ORDERSECONDSORTKEY);
        method.getBlock().ifCondition(relational(compare, LE, constant(0)))
                .blockReturn(newArrayWithInit(EventBean.EPTYPE, REF_ORDERFIRSTEVENT, REF_ORDERSECONDEVENT))
                .methodReturn(newArrayWithInit(EventBean.EPTYPE, REF_ORDERSECONDEVENT, REF_ORDERFIRSTEVENT));
    }

    private static CodegenMethod createSortPropertiesWRollupCodegen(OrderByProcessorForgeImpl forge, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        Consumer<CodegenMethod> code = method -> {
            method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "sortProperties", newArrayByLength(EPTypePremade.OBJECT.getEPType(), exprDotMethod(REF_ORDERCURRENTGENERATORS, "size")))
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));

            CodegenBlock forEach = method.getBlock().forEach(GroupByRollupKey.EPTYPE, "rollup", REF_ORDERCURRENTGENERATORS);

            if (forge.isNeedsGroupByKeys()) {
                forEach.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", exprDotMethod(ref("rollup"), "getGroupKey"), exprDotMethod(REF_EXPREVALCONTEXT, "getAgentInstanceId"), exprDotMethod(ref("rollup"), "getLevel"));
            }

            forEach.declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "num", exprDotMethodChain(ref("rollup")).add("getLevel").add("getLevelNumber"));
            CodegenBlock[] blocks = forEach.switchBlockOfLength(ref("num"), forge.getOrderByRollup().length, false);
            for (int i = 0; i < blocks.length; i++) {
                CodegenMethod getSortKey = generateOrderKeyCodegen("getSortKeyInternal_" + i, forge.getOrderByRollup()[i], classScope, namedMethods);
                blocks[i].assignArrayElement("sortProperties", ref("count"), localMethod(getSortKey, exprDotMethod(ref("rollup"), "getGenerator"), REF_ISNEWDATA, REF_EXPREVALCONTEXT));
            }

            forEach.incrementRef("count");
            method.getBlock().methodReturn(staticMethod(Arrays.class, "asList", ref("sortProperties")));
        };
        return namedMethods.addMethod(EPTypePremade.LIST.getEPType(), "createSortPropertiesWRollup", CodegenNamedParam.from(EPTypePremade.LIST.getEPType(), REF_ORDERCURRENTGENERATORS.getRef(), EPTypePremade.BOOLEANPRIMITIVE.getEPType(), REF_ISNEWDATA.getRef(), ExprEvaluatorContext.EPTYPE, REF_EXPREVALCONTEXT.getRef(), AggregationService.EPTYPE, MEMBER_AGGREGATIONSVC.getRef()), OrderByProcessorImpl.class, classScope, code);
    }

    public static CodegenMethod determineLocalMinMaxCodegen(OrderByProcessorForgeImpl forge, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        OrderByElementForge[] elements = forge.getOrderBy();
        CodegenExpression comparator = classScope.addOrGetFieldSharable(forge.getComparator());

        Consumer<CodegenMethod> code = method -> {
            method.getBlock().declareVar(EPTypePremade.OBJECT.getEPType(), "localMinMax", constantNull())
                    .declareVar(EventBean.EPTYPE, "outgoingMinMaxBean", constantNull())
                    .declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "count", constant(0));

            if (elements.length == 1) {
                CodegenBlock forEach = method.getBlock().forEach(EventBean.EPTYPEARRAY, "eventsPerStream", REF_GENERATINGEVENTS);

                forEach.declareVar(EPTypePremade.OBJECT.getEPType(), "sortKey", localMethod(CodegenLegoMethodExpression.codegenExpression(elements[0].getExprNode().getForge(), method, classScope), ref("eventsPerStream"), REF_ISNEWDATA, REF_EXPREVALCONTEXT))
                        .ifCondition(or(equalsNull(ref("localMinMax")), relational(exprDotMethod(comparator, "compare", ref("localMinMax"), ref("sortKey")), GT, constant(0))))
                        .assignRef("localMinMax", ref("sortKey"))
                        .assignRef("outgoingMinMaxBean", arrayAtIndex(REF_OUTGOINGEVENTS, ref("count")))
                        .blockEnd()
                        .incrementRef("count");
            } else {
                method.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "values", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(elements.length)))
                        .declareVar(HashableMultiKey.EPTYPE, "valuesMk", newInstance(HashableMultiKey.EPTYPE, ref("values")));

                CodegenBlock forEach = method.getBlock().forEach(EventBean.EPTYPEARRAY, "eventsPerStream", REF_GENERATINGEVENTS);

                if (forge.isNeedsGroupByKeys()) {
                    forEach.exprDotMethod(MEMBER_AGGREGATIONSVC, "setCurrentAccess", arrayAtIndex(ref("groupByKeys"), ref("count")), exprDotMethod(REF_EXPREVALCONTEXT, "getAgentInstanceId", constantNull()));
                }

                for (int i = 0; i < elements.length; i++) {
                    forEach.assignArrayElement("values", constant(i), localMethod(CodegenLegoMethodExpression.codegenExpression(elements[i].getExprNode().getForge(), method, classScope), ref("eventsPerStream"), REF_ISNEWDATA, REF_EXPREVALCONTEXT));
                }

                forEach.ifCondition(or(equalsNull(ref("localMinMax")), relational(exprDotMethod(comparator, "compare", ref("localMinMax"), ref("valuesMk")), GT, constant(0))))
                        .assignRef("localMinMax", ref("valuesMk"))
                        .assignRef("values", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(elements.length)))
                        .assignRef("valuesMk", newInstance(HashableMultiKey.EPTYPE, ref("values")))
                        .assignRef("outgoingMinMaxBean", arrayAtIndex(REF_OUTGOINGEVENTS, ref("count")))
                        .blockEnd()
                        .incrementRef("count");
            }

            method.getBlock().methodReturn(ref("outgoingMinMaxBean"));
        };

        return namedMethods.addMethod(EventBean.EPTYPE, "determineLocalMinMax", CodegenNamedParam.from(EventBean.EPTYPEARRAY, REF_OUTGOINGEVENTS.getRef(), EventBean.EPTYPEARRAYARRAY, REF_GENERATINGEVENTS.getRef(), EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT, AggregationService.EPTYPE, MEMBER_AGGREGATIONSVC.getRef()), OrderByProcessorImpl.class, classScope, code);
    }

    static CodegenMethod generateOrderKeyCodegen(String methodName, OrderByElementForge[] orderBy, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        Consumer<CodegenMethod> code = methodNode -> {
            if (orderBy.length == 1) {
                CodegenMethod expression = CodegenLegoMethodExpression.codegenExpression(orderBy[0].getExprNode().getForge(), methodNode, classScope);
                methodNode.getBlock().methodReturn(localMethod(expression, EnumForgeCodegenNames.REF_EPS, ResultSetProcessorCodegenNames.REF_ISNEWDATA, REF_EXPREVALCONTEXT));
                return;
            }

            methodNode.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "keys", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(orderBy.length)));
            for (int i = 0; i < orderBy.length; i++) {
                CodegenMethod expression = CodegenLegoMethodExpression.codegenExpression(orderBy[i].getExprNode().getForge(), methodNode, classScope);
                methodNode.getBlock().assignArrayElement("keys", constant(i), localMethod(expression, EnumForgeCodegenNames.REF_EPS, ResultSetProcessorCodegenNames.REF_ISNEWDATA, REF_EXPREVALCONTEXT));
            }
            methodNode.getBlock().methodReturn(newInstance(HashableMultiKey.EPTYPE, ref("keys")));
        };

        return namedMethods.addMethod(EPTypePremade.OBJECT.getEPType(), methodName, CodegenNamedParam.from(EventBean.EPTYPEARRAY, NAME_EPS, EPTypePremade.BOOLEANPRIMITIVE.getEPType(), NAME_ISNEWDATA, ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT), ResultSetProcessorUtil.class, classScope, code);
    }
}
