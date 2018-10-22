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
package com.espertech.esper.common.internal.schedule;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethodChain;

public class TimeProviderField implements CodegenFieldSharable {
    public final static TimeProviderField INSTANCE = new TimeProviderField();

    private TimeProviderField() {
    }

    public Class type() {
        return TimeProvider.class;
    }

    public CodegenExpression initCtorScoped() {
        return exprDotMethodChain(EPStatementInitServices.REF).add(EPStatementInitServices.GETTIMEPROVIDER);
    }
}
