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
package com.espertech.esper.common.internal.epl.agg.access.linear;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.core.AggregatorAccess;

public interface AggregatorAccessLinear extends AggregatorAccess {
    CodegenExpression sizeCodegen();

    CodegenExpression iteratorCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenNamedMethods namedMethods);

    CodegenExpression collectionReadOnlyCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    CodegenExpression getLastValueCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenNamedMethods namedMethods);

    CodegenExpression getFirstValueCodegen(CodegenClassScope classScope, CodegenMethod method);

    CodegenExpression getFirstNthValueCodegen(CodegenExpressionRef index, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    CodegenExpression getLastNthValueCodegen(CodegenExpressionRef index, CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);
}
