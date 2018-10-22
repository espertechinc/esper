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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class AggregationUseFlags {
    private final boolean isUnidirectional;
    private final boolean isFireAndForget;
    private final boolean isOnSelect;

    public AggregationUseFlags(boolean isUnidirectional, boolean isFireAndForget, boolean isOnSelect) {
        this.isUnidirectional = isUnidirectional;
        this.isFireAndForget = isFireAndForget;
        this.isOnSelect = isOnSelect;
    }

    public boolean isUnidirectional() {
        return isUnidirectional;
    }

    public boolean isFireAndForget() {
        return isFireAndForget;
    }

    public boolean isOnSelect() {
        return isOnSelect;
    }

    public CodegenExpression toExpression() {
        return newInstance(AggregationUseFlags.class, constant(isUnidirectional), constant(isFireAndForget), constant(isOnSelect));
    }
}
