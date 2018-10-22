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
package com.espertech.esper.common.internal.epl.agg.access.countminsketch;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregatorAccess;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchSpecForge;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionCountMinSketchNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

public class AggregationStateCountMinSketchForge implements AggregationStateFactoryForge {
    protected final ExprAggMultiFunctionCountMinSketchNode parent;
    protected final CountMinSketchSpecForge specification;
    protected AggregatorAccessCountMinSketch aggregator;

    public AggregationStateCountMinSketchForge(ExprAggMultiFunctionCountMinSketchNode parent, CountMinSketchSpecForge specification) {
        this.parent = parent;
        this.specification = specification;
    }

    public void initAccessForge(int col, boolean join, CodegenCtor rowCtor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        aggregator = new AggregatorAccessCountMinSketch(this, col, rowCtor, membersColumnized, classScope);
    }

    public AggregatorAccess getAggregator() {
        return aggregator;
    }

    public CodegenExpression codegenGetAccessTableState(int column, CodegenMethodScope parent, CodegenClassScope classScope) {
        return AggregatorAccessCountMinSketch.codegenGetAccessTableState(column, parent, classScope);
    }

    public ExprNode getExpression() {
        return parent;
    }
}
