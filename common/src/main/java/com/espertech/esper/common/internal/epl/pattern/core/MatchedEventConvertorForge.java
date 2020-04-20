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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.event.map.MapEventBean;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Converts from a map of prior matching events to a events per stream for resultion by expressions.
 */
public class MatchedEventConvertorForge {
    private final LinkedHashMap<String, Pair<EventType, String>> filterTypes;
    private final LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes;
    private final LinkedHashSet<String> allTags;
    private final Set<Integer> streamsUsedCanNull;
    private final boolean baseStreamIndexOne;

    public MatchedEventConvertorForge(LinkedHashMap<String, Pair<EventType, String>> filterTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTags, Set<Integer> streamsUsedCanNull, boolean baseStreamIndexOne) {
        this.filterTypes = filterTypes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(filterTypes);
        this.arrayEventTypes = arrayEventTypes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(arrayEventTypes);
        this.allTags = allTags == null ? new LinkedHashSet<>() : allTags;
        this.streamsUsedCanNull = streamsUsedCanNull;
        this.baseStreamIndexOne = baseStreamIndexOne;
    }

    public CodegenMethod make(CodegenMethodScope parent, CodegenClassScope classScope) {
        int size = filterTypes.size() + arrayEventTypes.size();
        CodegenMethod method = parent.makeChild(EventBean[].class, this.getClass(), classScope).addParam(MatchedEventMap.class, "mem");
        if (size == 0 || (streamsUsedCanNull != null && streamsUsedCanNull.isEmpty())) {
            method.getBlock().methodReturn(publicConstValue(CollectionUtil.class, "EVENTBEANARRAY_EMPTY"));
            return method;
        }

        int sizeArray = baseStreamIndexOne ? size + 1 : size;
        method.getBlock()
                .declareVar(EventBean[].class, "events", newArrayByLength(EventBean.class, constant(sizeArray)))
                .declareVar(Object[].class, "buf", exprDotMethod(ref("mem"), "getMatchingEvents"));

        int count = 0;
        for (Map.Entry<String, Pair<EventType, String>> entry : filterTypes.entrySet()) {
            int indexTag = findTag(allTags, entry.getKey());
            int indexStream = baseStreamIndexOne ? count + 1 : count;
            if (streamsUsedCanNull == null || streamsUsedCanNull.contains(indexStream)) {
                method.getBlock().assignArrayElement(ref("events"), constant(indexStream), cast(EventBean.class, arrayAtIndex(ref("buf"), constant(indexTag))));
            }
            count++;
        }

        for (Map.Entry<String, Pair<EventType, String>> entry : arrayEventTypes.entrySet()) {
            int indexTag = findTag(allTags, entry.getKey());
            int indexStream = baseStreamIndexOne ? count + 1 : count;
            if (streamsUsedCanNull == null || streamsUsedCanNull.contains(indexStream)) {
                method.getBlock()
                    .declareVar(EventBean[].class, "arr" + count, cast(EventBean[].class, arrayAtIndex(ref("buf"), constant(indexTag))))
                    .declareVar(Map.class, "map" + count, staticMethod(Collections.class, "singletonMap", constant(entry.getKey()), ref("arr" + count)))
                    .assignArrayElement(ref("events"), constant(indexStream), newInstance(MapEventBean.class, ref("map" + count), constantNull()));
            }
            count++;
        }

        method.getBlock().methodReturn(ref("events"));
        return method;
    }

    private int findTag(LinkedHashSet<String> allTags, String tag) {
        int index = 0;
        for (String oneTag : allTags) {
            if (tag.equals(oneTag)) {
                return index;
            }
            index++;
        }
        throw new IllegalStateException("Unexpected tag '" + tag + "'");
    }

    public CodegenExpression makeAnonymous(CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass clazz = newAnonymousClass(method.getBlock(), MatchedEventConvertor.class);
        CodegenMethod convert = CodegenMethod.makeParentNode(EventBean[].class, this.getClass(), classScope).addParam(MatchedEventMap.class, "events");
        clazz.addMethod("convert", convert);
        convert.getBlock().methodReturn(localMethod(make(convert, classScope), ref("events")));
        return clazz;
    }
}