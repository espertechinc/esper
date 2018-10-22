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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.filterspec.FilterSpecParam.*;

public interface FilterSpecParamFilterForEvalDoubleForge extends FilterSpecParamFilterForEvalForge {

    static CodegenExpression makeAnonymous(FilterSpecParamFilterForEvalDoubleForge eval, Class originator, CodegenClassScope classScope, CodegenMethod method) {
        CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(method.getBlock(), FilterSpecParamFilterForEvalDouble.class);

        CodegenMethod getFilterValueDouble = CodegenMethod.makeParentNode(Double.class, originator, classScope).addParam(GET_FILTER_VALUE_FP);
        anonymousClass.addMethod("getFilterValueDouble", getFilterValueDouble);
        getFilterValueDouble.getBlock().methodReturn(cast(Double.class, eval.makeCodegen(classScope, getFilterValueDouble)));

        CodegenMethod getFilterValue = CodegenMethod.makeParentNode(Object.class, originator, classScope).addParam(GET_FILTER_VALUE_FP);
        anonymousClass.addMethod("getFilterValue", getFilterValue);
        getFilterValue.getBlock().methodReturn(exprDotMethod(ref("this"), "getFilterValueDouble", REF_MATCHEDEVENTMAP, REF_EXPREVALCONTEXT, REF_STMTCTXFILTEREVALENV));

        return anonymousClass;
    }
}
