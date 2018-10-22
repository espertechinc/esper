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
import com.espertech.esper.common.internal.bytecodemodel.name.CodegenFieldNameViewAgg;

public class ExprValidationMemberNameQualifiedView implements ExprValidationMemberName {
    private final int streamNumber;

    public ExprValidationMemberNameQualifiedView(int streamNumber) {
        this.streamNumber = streamNumber;
    }

    public CodegenFieldName aggregationResultFutureRef() {
        return new CodegenFieldNameViewAgg(streamNumber);
    }

    public CodegenFieldName priorStrategy(int streamNum) {
        throw new UnsupportedOperationException("Not supported for views");
    }

    public CodegenFieldName previousStrategy(int streamNumber) {
        throw new UnsupportedOperationException("Not supported for views");
    }

    public CodegenFieldName previousMatchrecognizeStrategy() {
        throw new IllegalStateException("Match-recognize not supported in view parameters");
    }
}
