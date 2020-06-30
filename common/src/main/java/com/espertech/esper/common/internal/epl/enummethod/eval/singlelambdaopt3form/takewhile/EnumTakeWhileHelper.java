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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.takewhile;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;

import java.util.Collection;
import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumTakeWhileHelper {

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param enumcoll events
     * @return array
     */
    public static EventBean[] takeWhileLastEventBeanToArray(Collection<EventBean> enumcoll) {
        int size = enumcoll.size();
        EventBean[] all = new EventBean[size];
        int count = 0;
        for (EventBean item : enumcoll) {
            all[count++] = item;
        }
        return all;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param enumcoll coll
     * @return array
     */
    public static Object[] takeWhileLastScalarToArray(Collection enumcoll) {
        int size = enumcoll.size();
        Object[] all = new Object[size];
        int count = 0;
        for (Object item : enumcoll) {
            all[count++] = item;
        }
        return all;
    }

    public static void initBlockSizeOneScalar(int numParameters, CodegenBlock block, CodegenExpression innerValue, EPType evaluationType) {
        CodegenBlock blockSingle = block.ifCondition(equalsIdentity(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size"), constant(1)))
            .declareVar(EPTypePremade.OBJECT.getEPType(), "item", exprDotMethodChain(EnumForgeCodegenNames.REF_ENUMCOLL).add("iterator").add("next"))
            .assignArrayElement("props", constant(0), ref("item"));
        if (numParameters >= 2) {
            blockSingle.assignArrayElement("props", constant(1), constant(0));
        }
        CodegenLegoBooleanExpression.codegenReturnValueIfNotNullAndNotPass(blockSingle, evaluationType, innerValue, staticMethod(Collections.class, "emptyList"));
        blockSingle.blockReturn(staticMethod(Collections.class, "singletonList", ref("item")));

        block.declareVar(EPTypePremade.ARRAYDEQUE.getEPType(), "result", newInstance(EPTypePremade.ARRAYDEQUE.getEPType()));
    }

    public static void initBlockSizeOneEvent(CodegenBlock block, CodegenExpression innerValue, int streamNumLambda, EPTypeClass evaluationType) {
        CodegenBlock blockSingle = block.ifCondition(equalsIdentity(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size"), constant(1)))
            .declareVar(EventBean.EPTYPE, "item", cast(EventBean.EPTYPE, exprDotMethodChain(EnumForgeCodegenNames.REF_ENUMCOLL).add("iterator").add("next")))
            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(streamNumLambda), ref("item"));
        CodegenLegoBooleanExpression.codegenReturnValueIfNotNullAndNotPass(blockSingle, evaluationType, innerValue, staticMethod(Collections.class, "emptyList"));
        blockSingle.blockReturn(staticMethod(Collections.class, "singletonList", ref("item")));

        block.declareVar(EPTypePremade.ARRAYDEQUE.getEPType(), "result", newInstance(EPTypePremade.ARRAYDEQUE.getEPType()));
    }

    public static void initBlockSizeOneEventPlus(int numParameters, CodegenBlock block, CodegenExpression innerValue, int streamNumLambda, EPTypeClass evaluationType) {
        CodegenBlock blockSingle = block.ifCondition(equalsIdentity(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size"), constant(1)))
            .declareVar(EventBean.EPTYPE, "item", cast(EventBean.EPTYPE, exprDotMethodChain(EnumForgeCodegenNames.REF_ENUMCOLL).add("iterator").add("next")))
            .assignArrayElement(EnumForgeCodegenNames.REF_EPS, constant(streamNumLambda), ref("item"))
            .assignArrayElement("props", constant(0), constant(0));
        if (numParameters > 2) {
            blockSingle.assignArrayElement("props", constant(1), constant(1));
        }
        CodegenLegoBooleanExpression.codegenReturnValueIfNotNullAndNotPass(blockSingle, evaluationType, innerValue, staticMethod(Collections.class, "emptyList"));
        blockSingle.blockReturn(staticMethod(Collections.class, "singletonList", ref("item")));

        block.declareVar(EPTypePremade.ARRAYDEQUE.getEPType(), "result", newInstance(EPTypePremade.ARRAYDEQUE.getEPType()));
    }
}
