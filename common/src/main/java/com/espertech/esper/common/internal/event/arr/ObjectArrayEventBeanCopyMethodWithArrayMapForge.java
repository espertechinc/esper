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
package com.espertech.esper.common.internal.event.arr;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.util.IntArrayUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Copy method for Map-underlying events.
 */
public class ObjectArrayEventBeanCopyMethodWithArrayMapForge implements EventBeanCopyMethodForge {
    private final ObjectArrayEventType eventType;
    private final int[] mapIndexes;
    private final int[] arrayIndexes;

    public ObjectArrayEventBeanCopyMethodWithArrayMapForge(ObjectArrayEventType eventType, Set<String> mapPropertiesToCopy, Set<String> arrayPropertiesToCopy, Map<String, Integer> propertiesIndexes) {
        this.eventType = eventType;

        Set<Integer> mapIndexesToCopy = new HashSet<>();
        for (String prop : mapPropertiesToCopy) {
            Integer index = propertiesIndexes.get(prop);
            if (index != null) {
                mapIndexesToCopy.add(index);
            }
        }
        mapIndexes = IntArrayUtil.toArray(mapIndexesToCopy);

        Set<Integer> arrayIndexesToCopy = new HashSet<>();
        for (String prop : arrayPropertiesToCopy) {
            Integer index = propertiesIndexes.get(prop);
            if (index != null) {
                arrayIndexesToCopy.add(index);
            }
        }
        arrayIndexes = IntArrayUtil.toArray(arrayIndexesToCopy);
    }

    public CodegenExpression makeCopyMethodClassScoped(CodegenClassScope classScope) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        return newInstance(ObjectArrayEventBeanCopyMethodWithArrayMap.class,
                cast(ObjectArrayEventType.class, EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF)),
                factory,
                constant(mapIndexes), constant(arrayIndexes));
    }

    public EventBeanCopyMethod getCopyMethod(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new ObjectArrayEventBeanCopyMethodWithArrayMap(eventType, eventBeanTypedEventFactory, mapIndexes, arrayIndexes);
    }
}
