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
package com.espertech.esper.common.internal.statemgmtsettings;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.enumValue;

public class StateMgmtSettingBucketNone implements StateMgmtSettingBucket {
    public final static StateMgmtSettingBucketNone INSTANCE = new StateMgmtSettingBucketNone();

    private StateMgmtSettingBucketNone() {
    }

    public CodegenExpression toExpression() {
        return enumValue(StateMgmtSettingBucketNone.class, "INSTANCE");
    }
}
