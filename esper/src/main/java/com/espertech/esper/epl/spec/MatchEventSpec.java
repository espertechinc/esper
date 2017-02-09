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
package com.espertech.esper.epl.spec;

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;

import java.util.LinkedHashMap;

/**
 * Specification of matches available.
 */
public class MatchEventSpec {
    private final LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes;
    private final LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes;

    public MatchEventSpec(LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes) {
        this.taggedEventTypes = taggedEventTypes;
        this.arrayEventTypes = arrayEventTypes;
    }

    public MatchEventSpec() {
        this.taggedEventTypes = new LinkedHashMap<String, Pair<EventType, String>>();
        this.arrayEventTypes = new LinkedHashMap<String, Pair<EventType, String>>();
    }

    public LinkedHashMap<String, Pair<EventType, String>> getArrayEventTypes() {
        return arrayEventTypes;
    }

    public LinkedHashMap<String, Pair<EventType, String>> getTaggedEventTypes() {
        return taggedEventTypes;
    }
}
