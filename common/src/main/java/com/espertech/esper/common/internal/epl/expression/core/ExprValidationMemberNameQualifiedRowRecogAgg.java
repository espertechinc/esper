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
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameMatchRecognizeAgg;

public class ExprValidationMemberNameQualifiedRowRecogAgg implements ExprValidationMemberName {
    private final int streamNum;

    public ExprValidationMemberNameQualifiedRowRecogAgg(int streamNum) {
        this.streamNum = streamNum;
    }

    public CodegenFieldName aggregationResultFutureRef() {
        return new CodegenFieldNameMatchRecognizeAgg(streamNum);
    }

    public CodegenFieldName priorStrategy(int streamNum) {
        throw new IllegalStateException("Match-recognize measures-clauses not supported in subquery");
    }

    public CodegenFieldName previousStrategy(int streamNum) {
        throw new IllegalStateException("Match-recognize measures-clauses not supported with previous");
    }

    public CodegenFieldName previousMatchrecognizeStrategy() {
        throw new IllegalStateException("Match-recognize measures-clauses not supported in subquery");
    }
}
