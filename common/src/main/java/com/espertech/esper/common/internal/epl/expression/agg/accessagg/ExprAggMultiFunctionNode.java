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
package com.espertech.esper.common.internal.epl.expression.agg.accessagg;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEnumerationForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

public interface ExprAggMultiFunctionNode extends ExprEnumerationForge, ExprNode {

    AggregationForgeFactory getAggregationForgeFactory();

    int getColumn();

    CodegenExpression getAggFuture(CodegenClassScope classScope);
}
