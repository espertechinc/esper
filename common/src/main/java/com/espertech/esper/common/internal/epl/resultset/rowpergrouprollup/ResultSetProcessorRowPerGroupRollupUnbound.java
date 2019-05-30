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
package com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInstanceAux;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.collection.ArrayEventIterator;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorHelperFactoryField;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil.METHOD_TOPAIRNULLIFALLNULL;
import static com.espertech.esper.common.internal.epl.resultset.grouped.ResultSetProcessorGroupedUtil.METHOD_APPLYAGGVIEWRESULTKEYEDVIEW;
import static com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupImpl.generateGroupKeysViewCodegen;
import static com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupImpl.generateOutputEventsViewCodegen;

public class ResultSetProcessorRowPerGroupRollupUnbound {

    private final static String NAME_UNBOUNDHELPER = "unboundHelper";

    static void stopMethodUnboundCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        ResultSetProcessorRowPerGroupRollupImpl.stopMethodCodegenBound(method, instance);
        method.getBlock().exprDotMethod(ref(NAME_UNBOUNDHELPER), "destroy");
    }

    static void applyViewResultUnboundCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateGroupKeysView = generateGroupKeysViewCodegen(forge, classScope, instance);

        method.getBlock()
                .declareVar(Object[][].class, "newDataMultiKey", localMethod(generateGroupKeysView, REF_NEWDATA, exprDotMethod(ref(NAME_UNBOUNDHELPER), "getBuffer"), constantTrue()))
                .declareVar(Object[][].class, "oldDataMultiKey", localMethod(generateGroupKeysView, REF_OLDDATA, exprDotMethod(ref(NAME_UNBOUNDHELPER), "getBuffer"), constantFalse()))
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"), ref("eventsPerStream"));
    }

    static void processViewResultUnboundCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(ResultSetProcessorHelperFactoryField.INSTANCE);
        CodegenExpression eventTypes = classScope.addFieldUnshared(true, EventType[].class, EventTypeUtility.resolveTypeArrayCodegen(forge.getEventTypes(), EPStatementInitServices.REF));
        instance.addMember(NAME_UNBOUNDHELPER, ResultSetProcessorRowPerGroupRollupUnboundHelper.class);
        instance.getServiceCtor().getBlock().assignRef(NAME_UNBOUNDHELPER, exprDotMethod(factory, "makeRSRowPerGroupRollupSnapshotUnbound", MEMBER_AGENTINSTANCECONTEXT, ref("this"),
                constant(forge.getGroupKeyTypes()), constant(forge.getNumStreams()), eventTypes));

        CodegenMethod generateGroupKeysView = generateGroupKeysViewCodegen(forge, classScope, instance);
        CodegenMethod generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);

        method.getBlock()
                .declareVar(Object[][].class, "newDataMultiKey", localMethod(generateGroupKeysView, REF_NEWDATA, exprDotMethod(ref(NAME_UNBOUNDHELPER), "getBuffer"), constantTrue()))
                .declareVar(Object[][].class, "oldDataMultiKey", localMethod(generateGroupKeysView, REF_OLDDATA, exprDotMethod(ref(NAME_UNBOUNDHELPER), "getBuffer"), constantFalse()))
                .declareVar(EventBean[].class, "selectOldEvents", forge.isSelectRStream() ? localMethod(generateOutputEventsView, exprDotMethod(ref(NAME_UNBOUNDHELPER), "getBuffer"), constantFalse(), REF_ISSYNTHESIZE) : constantNull())
                .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                .staticMethod(ResultSetProcessorGroupedUtil.class, METHOD_APPLYAGGVIEWRESULTKEYEDVIEW, MEMBER_AGGREGATIONSVC, MEMBER_AGENTINSTANCECONTEXT, REF_NEWDATA, ref("newDataMultiKey"), REF_OLDDATA, ref("oldDataMultiKey"), ref("eventsPerStream"))
                .declareVar(EventBean[].class, "selectNewEvents", localMethod(generateOutputEventsView, exprDotMethod(ref(NAME_UNBOUNDHELPER), "getBuffer"), constantTrue(), REF_ISSYNTHESIZE))
                .methodReturn(staticMethod(ResultSetProcessorUtil.class, METHOD_TOPAIRNULLIFALLNULL, ref("selectNewEvents"), ref("selectOldEvents")));
    }

    static void getIteratorViewUnboundCodegen(ResultSetProcessorRowPerGroupRollupForge forge, CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        CodegenMethod generateOutputEventsView = generateOutputEventsViewCodegen(forge, classScope, instance);

        method.getBlock().declareVar(EventBean[].class, "output", localMethod(generateOutputEventsView, exprDotMethod(ref(NAME_UNBOUNDHELPER), "getBuffer"), constantTrue(), constantTrue()))
                .methodReturn(newInstance(ArrayEventIterator.class, ref("output")));
    }
}
