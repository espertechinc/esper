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
package com.espertech.esper.common.internal.event.bean.getter;

import com.espertech.esper.common.internal.event.bean.introspect.EventPropertyGetterSPIFactory;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import java.lang.reflect.Field;

public final class ReflectionPropFieldGetterFactory implements EventPropertyGetterSPIFactory {
    private final Field field;

    public ReflectionPropFieldGetterFactory(Field field) {
        this.field = field;
    }

    public EventPropertyGetterSPI make(EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        return new ReflectionPropFieldGetter(field, eventBeanTypedEventFactory, beanEventTypeFactory);
    }
}
