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
package com.espertech.esper.event.map;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;

/**
 * Getter for map entry.
 */
public class MapPropertyGetterDefaultNoFragment extends MapPropertyGetterDefaultBase {
    public MapPropertyGetterDefaultNoFragment(String propertyName, EventAdapterService eventAdapterService) {
        super(propertyName, null, eventAdapterService);
    }

    protected Object handleCreateFragment(Object value) {
        return null;
    }

    protected CodegenExpression handleCreateFragmentCodegen(CodegenExpression value, CodegenClassScope codegenClassScope) {
        return constantNull();
    }
}
