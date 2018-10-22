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
package com.espertech.esper.common.internal.epl.index.advanced.index.quadtree;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.publicConstValue;

public class EventAdvancedIndexFactoryForgeQuadTreeMXCIFForge extends EventAdvancedIndexFactoryForgeQuadTreeForge {

    public final static EventAdvancedIndexFactoryForgeQuadTreeMXCIFForge INSTANCE = new EventAdvancedIndexFactoryForgeQuadTreeMXCIFForge();

    private EventAdvancedIndexFactoryForgeQuadTreeMXCIFForge() {
    }

    public boolean providesIndexForOperation(String operationName) {
        return operationName.equals(SettingsApplicationDotMethodRectangeIntersectsRectangle.LOOKUP_OPERATION_NAME);
    }

    public CodegenExpression codegenMake(CodegenMethodScope parent, CodegenClassScope classScope) {
        return publicConstValue(EventAdvancedIndexFactoryForgeQuadTreeMXCIFFactory.class, "INSTANCE");
    }

    public EventAdvancedIndexFactory getRuntimeFactory() {
        return EventAdvancedIndexFactoryForgeQuadTreeMXCIFFactory.INSTANCE;
    }
}
