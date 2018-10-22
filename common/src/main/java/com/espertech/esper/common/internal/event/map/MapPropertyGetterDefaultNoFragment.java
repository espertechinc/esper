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
package com.espertech.esper.common.internal.event.map;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

/**
 * Getter for map entry.
 */
public class MapPropertyGetterDefaultNoFragment extends MapPropertyGetterDefaultBase {
    public MapPropertyGetterDefaultNoFragment(String propertyName, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        super(propertyName, null, eventBeanTypedEventFactory);
    }

    protected Object handleCreateFragment(Object value) {
        return null;
    }

    protected CodegenExpression handleCreateFragmentCodegen(CodegenExpression value, CodegenClassScope codegenClassScope) {
        return constantNull();
    }
}
