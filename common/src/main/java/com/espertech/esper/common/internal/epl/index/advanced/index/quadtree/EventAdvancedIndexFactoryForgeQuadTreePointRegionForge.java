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

public class EventAdvancedIndexFactoryForgeQuadTreePointRegionForge extends EventAdvancedIndexFactoryForgeQuadTreeForge {

    public final static EventAdvancedIndexFactoryForgeQuadTreePointRegionForge INSTANCE = new EventAdvancedIndexFactoryForgeQuadTreePointRegionForge();

    private EventAdvancedIndexFactoryForgeQuadTreePointRegionForge() {
    }

    public boolean providesIndexForOperation(String operationName) {
        return operationName.equals(SettingsApplicationDotMethodPointInsideRectange.LOOKUP_OPERATION_NAME);
    }

    public CodegenExpression codegenMake(CodegenMethodScope parent, CodegenClassScope classScope) {
        return publicConstValue(EventAdvancedIndexFactoryForgeQuadTreePointRegionFactory.class, "INSTANCE");
    }

    public EventAdvancedIndexFactory getRuntimeFactory() {
        return EventAdvancedIndexFactoryForgeQuadTreePointRegionFactory.INSTANCE;
    }
}
