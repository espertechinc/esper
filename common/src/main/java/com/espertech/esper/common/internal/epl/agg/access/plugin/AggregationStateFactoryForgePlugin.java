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
package com.espertech.esper.common.internal.epl.agg.access.plugin;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateModeManaged;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregatorAccess;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

public class AggregationStateFactoryForgePlugin implements AggregationStateFactoryForge {
    private final AggregationForgeFactoryAccessPlugin forgeFactory;
    private final AggregationMultiFunctionStateModeManaged mode;
    private AggregatorAccessPlugin access;

    public AggregationStateFactoryForgePlugin(AggregationForgeFactoryAccessPlugin forgeFactory, AggregationMultiFunctionStateModeManaged mode) {
        this.forgeFactory = forgeFactory;
        this.mode = mode;
    }

    public void initAccessForge(int col, boolean join, CodegenCtor ctor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        access = new AggregatorAccessPlugin(col, join, ctor, membersColumnized, classScope, forgeFactory.getAggregationExpression().getOptionalFilter(), mode);
    }

    public AggregatorAccess getAggregator() {
        return access;
    }

    public CodegenExpression codegenGetAccessTableState(int column, CodegenMethodScope parent, CodegenClassScope classScope) {
        return AggregatorAccessPlugin.codegenGetAccessTableState(column);
    }

    public ExprNode getExpression() {
        return forgeFactory.getAggregationExpression();
    }
}
