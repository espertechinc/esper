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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecCondition;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecConditionFilter;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecConditionPattern;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.Map;
import java.util.Set;

public class ContextPropertyEventType {
    public static final String PROP_CTX_NAME = "name";
    public static final String PROP_CTX_ID = "id";
    public static final String PROP_CTX_LABEL = "label";
    public static final String PROP_CTX_STARTTIME = "startTime";
    public static final String PROP_CTX_ENDTIME = "endTime";
    public static final String PROP_CTX_KEY_PREFIX = "key";
    public static final String PROP_CTX_KEY_PREFIX_SINGLE = "key1";

    public static void addEndpointTypes(ContextSpecCondition endpoint, Map<String, Object> properties, Set<String> allTags) throws ExprValidationException {
        if (endpoint instanceof ContextSpecConditionFilter) {
            ContextSpecConditionFilter filter = (ContextSpecConditionFilter) endpoint;
            if (filter.getOptionalFilterAsName() != null) {
                allTags.add(filter.getOptionalFilterAsName());
                properties.put(filter.getOptionalFilterAsName(), filter.getFilterSpecCompiled().getFilterForEventType());
            }
        }
        if (endpoint instanceof ContextSpecConditionPattern) {
            ContextSpecConditionPattern pattern = (ContextSpecConditionPattern) endpoint;
            if (pattern.getAsName() == null) {
                for (Map.Entry<String, Pair<EventType, String>> entry : pattern.getPatternCompiled().getTaggedEventTypes().entrySet()) {
                    if (properties.containsKey(entry.getKey()) && !properties.get(entry.getKey()).equals(entry.getValue().getFirst())) {
                        throw new ExprValidationException("The stream or tag name '" + entry.getKey() + "' is already declared");
                    }
                    allTags.add(entry.getKey());
                    properties.put(entry.getKey(), entry.getValue().getFirst());
                }
            } else {
                if (properties.containsKey(pattern.getAsName()) || allTags.contains(pattern.getAsName())) {
                    throw new ExprValidationException("The stream or tag name '" + pattern.getAsName() + "' is already declared");
                }
                if (pattern.getAsNameEventType() == null) {
                    throw new IllegalStateException("no event type assigned");
                }
                properties.put(pattern.getAsName(), pattern.getAsNameEventType());
                allTags.add(pattern.getAsName());
            }
        }
    }

    public static int getStreamNumberForNestingLevel(int nestingLevel, boolean isStartCondition) {
        return nestingLevel * 10 + (isStartCondition ? 0 : 1);
    }
}
