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
package com.espertech.esper.common.internal.rettype;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Clazz can be either
 * - Collection
 * - Array i.e. "EventType[].class"
 */
public class EPChainableTypeEventMulti implements EPChainableType {
    public final static EPTypeClass EPTYPE = new EPTypeClass(EPChainableTypeEventMulti.class);

    private final Class container;
    private final EventType component;

    public EPChainableTypeEventMulti(Class container, EventType component) {
        this.container = container;
        this.component = component;
    }

    public Class getContainer() {
        return container;
    }

    public EventType getComponent() {
        return component;
    }

    public CodegenExpression codegen(CodegenMethod method, CodegenClassScope classScope, CodegenExpression typeInitSvcRef) {
        return newInstance(EPChainableTypeEventMulti.EPTYPE, constant(container), EventTypeUtility.resolveTypeCodegen(component, typeInitSvcRef));
    }
}
