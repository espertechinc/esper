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
package com.espertech.esper.common.internal.epl.lookup;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.SubordTableLookupStrategyFactoryQuadTreeForge;

import java.util.Map;

public interface EventAdvancedIndexFactoryForge {
    boolean providesIndexForOperation(String operationName);

    SubordTableLookupStrategyFactoryQuadTreeForge getSubordinateLookupStrategy(String operationName, Map<Integer, ExprNode> expressions, boolean isNWOnTrigger, int numOuterstreams);

    CodegenExpression codegenMake(CodegenMethodScope parent, CodegenClassScope classScope);

    EventAdvancedIndexFactory getRuntimeFactory();
}
