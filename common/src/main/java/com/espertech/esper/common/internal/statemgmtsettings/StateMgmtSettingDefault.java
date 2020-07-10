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

import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

public class StateMgmtSettingDefault implements StateMgmtSetting {
    public final static StateMgmtSettingDefault INSTANCE = new StateMgmtSettingDefault();

    public StateMgmtSettingDefault() {
    }

    public CodegenExpression toExpression() {
        return constantNull();
    }
}
