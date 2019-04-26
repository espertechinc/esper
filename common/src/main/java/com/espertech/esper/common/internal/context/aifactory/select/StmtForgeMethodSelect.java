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
package com.espertech.esper.common.internal.context.aifactory.select;

import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtForgeMethod;
import com.espertech.esper.common.internal.compile.stage3.StmtForgeMethodResult;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

public class StmtForgeMethodSelect implements StmtForgeMethod {

    private final StatementBaseInfo base;

    public StmtForgeMethodSelect(StatementBaseInfo base) {
        this.base = base;
    }

    public StmtForgeMethodResult make(String packageName, String classPostfix, StatementCompileTimeServices services) throws ExprValidationException {
        StmtForgeMethodSelectResult forgeablesResult = StmtForgeMethodSelectUtil.make(false, packageName, classPostfix, base, services);
        return forgeablesResult.getForgeResult();
    }
}
