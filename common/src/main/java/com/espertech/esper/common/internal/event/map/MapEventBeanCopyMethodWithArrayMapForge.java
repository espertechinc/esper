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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.*;

import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Copy method for Map-underlying events.
 */
public class MapEventBeanCopyMethodWithArrayMapForge implements EventBeanCopyMethodForge {
    private final MapEventType mapEventType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final String[] mapPropertiesToCopy;
    private final String[] arrayPropertiesToCopy;

    /**
     * Ctor.
     *
     * @param mapEventType               map event type
     * @param eventBeanTypedEventFactory for copying events
     * @param mapPropertiesToCopy        map props
     * @param arrayPropertiesToCopy      array props
     */
    public MapEventBeanCopyMethodWithArrayMapForge(MapEventType mapEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, Set<String> mapPropertiesToCopy, Set<String> arrayPropertiesToCopy) {
        this.mapEventType = mapEventType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.mapPropertiesToCopy = mapPropertiesToCopy.toArray(new String[mapPropertiesToCopy.size()]);
        this.arrayPropertiesToCopy = arrayPropertiesToCopy.toArray(new String[arrayPropertiesToCopy.size()]);
    }

    public CodegenExpression makeCopyMethodClassScoped(CodegenClassScope classScope) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        return newInstance(MapEventBeanCopyMethodWithArrayMap.class,
                cast(MapEventType.class, EventTypeUtility.resolveTypeCodegen(mapEventType, EPStatementInitServices.REF)),
                factory,
                constant(mapPropertiesToCopy), constant(arrayPropertiesToCopy));
    }

    public EventBeanCopyMethod getCopyMethod(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new MapEventBeanCopyMethodWithArrayMap(mapEventType, eventBeanTypedEventFactory, mapPropertiesToCopy, arrayPropertiesToCopy);
    }
}
