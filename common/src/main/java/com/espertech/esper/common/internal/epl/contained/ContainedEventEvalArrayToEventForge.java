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
package com.espertech.esper.common.internal.epl.contained;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturerForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContainedEventEvalArrayToEventForge implements ContainedEventEvalForge {

    private final ExprForge evaluator;
    private final EventBeanManufacturerForge manufacturer;

    public ContainedEventEvalArrayToEventForge(ExprForge evaluator, EventBeanManufacturerForge manufacturer) {
        this.evaluator = evaluator;
        this.manufacturer = manufacturer;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ContainedEventEvalArrayToEvent.class, this.getClass(), classScope);
        CodegenExpression eval = ExprNodeUtilityCodegen.codegenEvaluator(evaluator, method, this.getClass(), classScope);
        method.getBlock()
                .declareVar(EventBeanManufacturer.class, "manu", manufacturer.make(method, classScope))
                .methodReturn(newInstance(ContainedEventEvalArrayToEvent.class, eval, ref("manu")));
        return localMethod(method);
    }
}
