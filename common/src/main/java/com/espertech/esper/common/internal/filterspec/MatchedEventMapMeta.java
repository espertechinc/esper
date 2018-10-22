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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public final class MatchedEventMapMeta {
    private final static int MIN_MAP_LOOKUP = 3;

    private final String[] tagsPerIndex;
    private final EventType[] eventTypes;
    private final String[] arrayTags;

    private final Map<String, Integer> tagsPerIndexMap;

    public MatchedEventMapMeta(String[] tagsPerIndex, EventType[] eventTypes, String[] arrayTags) {
        this.tagsPerIndex = tagsPerIndex;
        this.eventTypes = eventTypes;
        this.arrayTags = arrayTags;
        this.tagsPerIndexMap = getMap(tagsPerIndex);
    }

    public String[] getTagsPerIndex() {
        return tagsPerIndex;
    }

    public EventType[] getEventTypes() {
        return eventTypes;
    }

    public int getTagFor(String key) {
        if (tagsPerIndexMap != null) {
            Integer result = tagsPerIndexMap.get(key);
            return result == null ? -1 : result;
        }
        for (int i = 0; i < tagsPerIndex.length; i++) {
            if (tagsPerIndex[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, Integer> getMap(String[] tagsPerIndex) {
        if (tagsPerIndex.length < MIN_MAP_LOOKUP) {
            return null;
        }

        Map<String, Integer> map = new HashMap<String, Integer>();
        for (int i = 0; i < tagsPerIndex.length; i++) {
            map.put(tagsPerIndex[i], i);
        }
        return map;
    }

    public boolean isHasArrayProperties() {
        return arrayTags != null;
    }

    public String[] getArrayTags() {
        return arrayTags;
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(MatchedEventMapMeta.class, this.getClass(), classScope);
        method.getBlock().declareVar(String[].class, "tagsPerIndex", constant(tagsPerIndex))
                .declareVar(EventType[].class, "eventTypes", EventTypeUtility.resolveTypeArrayCodegen(eventTypes, symbols.getAddInitSvc(method)))
                .methodReturn(newInstance(MatchedEventMapMeta.class, ref("tagsPerIndex"), ref("eventTypes"), constant(arrayTags)));
        return method;
    }

    public Object getEventTypeForTag(String tag) {
        return eventTypes[getTagFor(tag)];
    }

    public String[] getNonArrayTags() {
        if (!isHasArrayProperties()) {
            return tagsPerIndex;
        }
        String[] result = new String[tagsPerIndex.length - arrayTags.length];
        int count = 0;
        for (int i = 0; i < tagsPerIndex.length; i++) {
            boolean isArray = false;
            for (int j = 0; j < arrayTags.length; j++) {
                if (arrayTags[j].equals(tagsPerIndex[i])) {
                    isArray = true;
                    break;
                }
            }
            if (!isArray) {
                result[count++] = tagsPerIndex[i];
            }
        }
        return result;
    }
}
