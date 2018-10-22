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
package com.espertech.esper.common.internal.epl.expression.time.eval;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacusField;
import com.espertech.esper.common.internal.epl.expression.time.adder.TimePeriodAdder;
import com.espertech.esper.common.internal.epl.expression.time.adder.TimePeriodAdderUtil;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriodUtil;
import com.espertech.esper.common.internal.settings.RuntimeSettingsTimeZoneField;

import java.util.TimeZone;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class TimePeriodComputeConstGivenCalAddForge implements TimePeriodComputeForge {
    private final TimePeriodAdder[] adders;
    private final int[] added;
    private final TimeAbacus timeAbacus;
    private final int indexMicroseconds;

    public TimePeriodComputeConstGivenCalAddForge(TimePeriodAdder[] adders, int[] added, TimeAbacus timeAbacus) {
        this.adders = adders;
        this.added = added;
        this.timeAbacus = timeAbacus;
        this.indexMicroseconds = ExprTimePeriodUtil.findIndexMicroseconds(adders);
    }

    public TimePeriodCompute getEvaluator() {
        return new TimePeriodComputeConstGivenCalAddEval(adders, added, timeAbacus, indexMicroseconds, TimeZone.getDefault());
    }

    public CodegenExpression makeEvaluator(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(TimePeriodComputeConstGivenCalAddEval.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(TimePeriodComputeConstGivenCalAddEval.class, "eval", newInstance(TimePeriodComputeConstGivenCalAddEval.class))
                .exprDotMethod(ref("eval"), "setAdders", TimePeriodAdderUtil.makeArray(adders, parent, classScope))
                .exprDotMethod(ref("eval"), "setAdded", constant(added))
                .exprDotMethod(ref("eval"), "setTimeAbacus", classScope.addOrGetFieldSharable(TimeAbacusField.INSTANCE))
                .exprDotMethod(ref("eval"), "setIndexMicroseconds", constant(indexMicroseconds))
                .exprDotMethod(ref("eval"), "setTimeZone", classScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE))
                .methodReturn(ref("eval"));
        return localMethod(method);
    }
}
