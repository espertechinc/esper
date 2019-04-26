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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import java.util.List;

public final class FilterSpecValidatedDesc {
    private final List<ExprNode> expressions;
    private final List<StmtClassForgeableFactory> additionalForgeables;

    public FilterSpecValidatedDesc(List<ExprNode> expressions, List<StmtClassForgeableFactory> additionalForgeables) {
        this.expressions = expressions;
        this.additionalForgeables = additionalForgeables;
    }

    public List<ExprNode> getExpressions() {
        return expressions;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }
}
