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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNode;
import com.espertech.esper.common.internal.epl.pattern.core.EvalRootForgeNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterForgeNode;
import com.espertech.esper.common.internal.filterspec.MatchedEventMapMeta;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * Specification for building an event stream out of a pattern statement and views staggered onto the
 * pattern statement.
 * <p>
 * The pattern statement is represented by the top EvalNode evaluation node.
 * A pattern statement contains tagged events (i.e. a=A -&gt; b=B).
 * Thus the resulting event type is has properties "a" and "b" of the type of A and B.
 */
public class PatternStreamSpecCompiled extends StreamSpecBase implements StreamSpecCompiled {
    private final EvalRootForgeNode root;
    private final LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes;       // Stores types for filters with tags, single event
    private final LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes;       // Stores types for filters with tags, array event
    private final LinkedHashSet<String> allTags;
    private final boolean suppressSameEventMatches;
    private final boolean discardPartialsOnMatch;
    private static final long serialVersionUID = 1268004301792124753L;

    public PatternStreamSpecCompiled(EvalRootForgeNode root, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTags, ViewSpec[] viewSpecs, String optionalStreamName, StreamSpecOptions streamSpecOptions, boolean suppressSameEventMatches, boolean discardPartialsOnMatch) {
        super(optionalStreamName, viewSpecs, streamSpecOptions);
        this.suppressSameEventMatches = suppressSameEventMatches;
        this.discardPartialsOnMatch = discardPartialsOnMatch;
        this.root = root;
        this.allTags = allTags;

        LinkedHashMap<String, Pair<EventType, String>> copy = new LinkedHashMap<String, Pair<EventType, String>>();
        copy.putAll(taggedEventTypes);
        this.taggedEventTypes = copy;

        copy = new LinkedHashMap<String, Pair<EventType, String>>();
        copy.putAll(arrayEventTypes);
        this.arrayEventTypes = copy;
    }

    /**
     * Returns the pattern expression evaluation node for the top pattern operator.
     *
     * @return parent pattern expression node
     */
    public EvalRootForgeNode getRoot() {
        return root;
    }

    public boolean isConsumingFilters() {
        return isConsumingFiltersRecursive(root);
    }

    /**
     * Returns event types tagged in the pattern expression.
     *
     * @return map of tag and event type tagged in pattern expression
     */
    public LinkedHashMap<String, Pair<EventType, String>> getTaggedEventTypes() {
        return taggedEventTypes;
    }

    /**
     * Returns event types tagged in the pattern expression under a repeat-operator.
     *
     * @return map of tag and event type tagged in pattern expression, repeated an thus producing array events
     */
    public LinkedHashMap<String, Pair<EventType, String>> getArrayEventTypes() {
        return arrayEventTypes;
    }

    public MatchedEventMapMeta getMatchedEventMapMeta() {
        String[] tags = new String[allTags.size()];
        EventType[] eventTypes = new EventType[allTags.size()];
        int count = 0;
        for (String tag : allTags) {
            tags[count] = tag;
            EventType eventType = null;
            Pair<EventType, String> nonArray = taggedEventTypes.get(tag);
            if (nonArray != null) {
                eventType = nonArray.getFirst();
            } else {
                Pair<EventType, String> array = arrayEventTypes.get(tag);
                if (array != null) {
                    eventType = array.getFirst();
                }
            }
            if (eventType == null) {
                throw new IllegalStateException("Failed to find tag '" + tag + "' among type information");
            }
            eventTypes[count++] = eventType;
        }
        String[] arrayTags = arrayEventTypes.isEmpty() ? null : arrayEventTypes.keySet().toArray(new String[0]);
        return new MatchedEventMapMeta(tags, eventTypes, arrayTags);
    }

    public LinkedHashSet<String> getAllTags() {
        return allTags;
    }

    public boolean isSuppressSameEventMatches() {
        return suppressSameEventMatches;
    }

    public boolean isDiscardPartialsOnMatch() {
        return discardPartialsOnMatch;
    }

    private boolean isConsumingFiltersRecursive(EvalForgeNode evalNode) {
        if (evalNode instanceof EvalFilterForgeNode) {
            return ((EvalFilterForgeNode) evalNode).getConsumptionLevel() != null;
        }
        boolean consumption = false;
        for (EvalForgeNode child : evalNode.getChildNodes()) {
            consumption = consumption || isConsumingFiltersRecursive(child);
        }
        return consumption;
    }
}
