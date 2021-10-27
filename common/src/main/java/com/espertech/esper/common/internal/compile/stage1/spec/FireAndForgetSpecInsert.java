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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import java.util.List;

public class FireAndForgetSpecInsert extends FireAndForgetSpec {
    private final boolean useValuesKeyword;
    private final List<List<ExprNode>> multirow;

    public FireAndForgetSpecInsert(boolean useValuesKeyword, List<List<ExprNode>> multirow) {
        this.useValuesKeyword = useValuesKeyword;
        this.multirow = multirow;
    }

    public boolean isUseValuesKeyword() {
        return useValuesKeyword;
    }

    public List<List<ExprNode>> getMultirow() {
        return multirow;
    }
}
