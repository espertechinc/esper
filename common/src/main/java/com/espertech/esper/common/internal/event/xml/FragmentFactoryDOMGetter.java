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
package com.espertech.esper.common.internal.event.xml;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCodegenField;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import org.w3c.dom.Node;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for fragments for DOM getters.
 */
public class FragmentFactoryDOMGetter implements FragmentFactorySPI {
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final BaseXMLEventType xmlEventType;
    private final String propertyExpression;
    private volatile EventType fragmentType;

    /**
     * Ctor.
     *
     * @param eventBeanTypedEventFactory for event type lookup
     * @param xmlEventType               the originating type
     * @param propertyExpression         property expression
     */
    public FragmentFactoryDOMGetter(EventBeanTypedEventFactory eventBeanTypedEventFactory, BaseXMLEventType xmlEventType, String propertyExpression) {
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.xmlEventType = xmlEventType;
        this.propertyExpression = propertyExpression;
    }

    public EventBean getEvent(Node result) {
        if (fragmentType == null) {
            FragmentEventType type = xmlEventType.getFragmentType(propertyExpression);
            if (type == null) {
                return null;
            }
            fragmentType = type.getFragmentType();
        }

        return eventBeanTypedEventFactory.adapterForTypedDOM(result, fragmentType);
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpression xmlType = cast(BaseXMLEventType.class, EventTypeUtility.resolveTypeCodegen(xmlEventType, EPStatementInitServices.REF));
        return newInstance(FragmentFactoryDOMGetter.class, factory, xmlType, constant(propertyExpression));
    }
}
