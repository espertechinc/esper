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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionMethodDesc;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleTableInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

public interface AggregationPortableValidation {
    void validateIntoTableCompatible(String tableExpression, AggregationPortableValidation intoTableAgg, String intoExpression, AggregationForgeFactory factory) throws ExprValidationException;

    boolean isAggregationMethod(String name, ExprNode[] parameters, ExprValidationContext validationContext);
    AggregationMultiFunctionMethodDesc validateAggregationMethod(ExprValidationContext validationContext, String aggMethodName, ExprNode[] params) throws ExprValidationException;

    CodegenExpression make(CodegenMethodScope parent, ModuleTableInitializeSymbol symbols, CodegenClassScope classScope);
}
