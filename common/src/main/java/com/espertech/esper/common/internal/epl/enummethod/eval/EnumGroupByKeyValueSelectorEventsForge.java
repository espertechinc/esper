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
package com.espertech.esper.common.internal.epl.enummethod.eval;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

public class EnumGroupByKeyValueSelectorEventsForge extends EnumForgeBase {

    protected ExprForge secondExpression;

    public EnumGroupByKeyValueSelectorEventsForge(ExprForge innerExpression, int streamCountIncoming, ExprForge secondExpression) {
        super(innerExpression, streamCountIncoming);
        this.secondExpression = secondExpression;
    }

    public EnumEval getEnumEvaluator() {
        return new EnumGroupByKeyValueSelectorEventsForgeEval(this, innerExpression.getExprEvaluator(), secondExpression.getExprEvaluator());
    }

    public CodegenExpression codegen(EnumForgeCodegenParams premade, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return EnumGroupByKeyValueSelectorEventsForgeEval.codegen(this, premade, codegenMethodScope, codegenClassScope);
    }
}
