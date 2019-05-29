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
package com.espertech.esper.common.internal.event.json.writer;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.util.IntArrayUtil;

import java.util.HashSet;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Copy method for Json-underlying events.
 */
public class JsonEventBeanCopyMethodForge implements EventBeanCopyMethodForge {
    private final JsonEventType eventType;
    private final int[] regularIndexes;
    private final int[] mapIndexes;
    private final int[] arrayIndexes;

    public JsonEventBeanCopyMethodForge(JsonEventType eventType, Set<String> mapPropertiesToCopy, Set<String> arrayPropertiesToCopy) {
        this.eventType = eventType;

        Set<Integer> mapIndexesToCopy = new HashSet<>();
        for (String prop : mapPropertiesToCopy) {
            JsonUnderlyingField field = eventType.getDetail().getFieldDescriptors().get(prop);
            mapIndexesToCopy.add(field.getPropertyNumber());
        }

        Set<Integer> arrayIndexesToCopy = new HashSet<>();
        for (String prop : arrayPropertiesToCopy) {
            JsonUnderlyingField field = eventType.getDetail().getFieldDescriptors().get(prop);
            arrayIndexesToCopy.add(field.getPropertyNumber());
        }

        Set<Integer> regularProps = new HashSet<>();
        for (String prop : eventType.getTypes().keySet()) {
            if (mapPropertiesToCopy.contains(prop) || arrayPropertiesToCopy.contains(prop)) {
                continue;
            }
            JsonUnderlyingField field = eventType.getDetail().getFieldDescriptors().get(prop);
            regularProps.add(field.getPropertyNumber());
        }

        mapIndexes = IntArrayUtil.toArray(mapIndexesToCopy);
        arrayIndexes = IntArrayUtil.toArray(arrayIndexesToCopy);
        regularIndexes = IntArrayUtil.toArray(regularProps);
    }

    public CodegenExpression makeCopyMethodClassScoped(CodegenClassScope classScope) {
        CodegenExpressionField factory = classScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        return newInstance(JsonEventBeanCopyMethod.class,
            cast(JsonEventType.class, EventTypeUtility.resolveTypeCodegen(eventType, EPStatementInitServices.REF)),
            factory,
            constant(regularIndexes), constant(mapIndexes), constant(arrayIndexes));
    }

    public EventBeanCopyMethod getCopyMethod(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new JsonEventBeanCopyMethod(eventType, eventBeanTypedEventFactory, regularIndexes, mapIndexes, arrayIndexes);
    }
}
