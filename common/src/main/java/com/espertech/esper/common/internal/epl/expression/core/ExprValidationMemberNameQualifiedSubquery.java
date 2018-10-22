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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldName;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameSubqueryAgg;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameSubqueryPrevious;
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameSubqueryPrior;

public class ExprValidationMemberNameQualifiedSubquery implements ExprValidationMemberName {
    private final int subqueryNum;

    public ExprValidationMemberNameQualifiedSubquery(int subqueryNum) {
        this.subqueryNum = subqueryNum;
    }

    public CodegenFieldName aggregationResultFutureRef() {
        return new CodegenFieldNameSubqueryAgg(subqueryNum);
    }

    public CodegenFieldName priorStrategy(int streamNum) {
        return new CodegenFieldNameSubqueryPrior(subqueryNum);
    }

    public CodegenFieldName previousStrategy(int streamNum) {
        return new CodegenFieldNameSubqueryPrevious(subqueryNum);
    }

    public CodegenFieldName previousMatchrecognizeStrategy() {
        throw new IllegalStateException("Match-recognize not supported in subquery");
    }
}
