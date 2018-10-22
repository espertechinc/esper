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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.*;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.cast;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Copy method for Map-underlying events.
 */
public class MapEventBeanCopyMethodForge implements EventBeanCopyMethodForge {
    private final MapEventType mapEventType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;

    /**
     * Ctor.
     *
     * @param mapEventType               map event type
     * @param eventBeanTypedEventFactory for copying events
     */
    public MapEventBeanCopyMethodForge(MapEventType mapEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        this.mapEventType = mapEventType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public EventBean copy(EventBean theEvent) {
        MappedEventBean mapped = (MappedEventBean) theEvent;
        Map<String, Object> props = mapped.getProperties();
        return eventBeanTypedEventFactory.adapterForTypedMap(new HashMap<String, Object>(props), mapEventType);
    }

    public CodegenExpression makeCopyMethodClassScoped(CodegenClassScope classScope) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        return newInstance(MapEventBeanCopyMethod.class,
                cast(MapEventType.class, EventTypeUtility.resolveTypeCodegen(mapEventType, EPStatementInitServices.REF)),
                factory);
    }

    public EventBeanCopyMethod getCopyMethod(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new MapEventBeanCopyMethod(mapEventType, eventBeanTypedEventFactory);
    }
}
