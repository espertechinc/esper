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
import com.espertech.esper.filterspec.MatchedEventMapMeta;
import com.espertech.esper.pattern.EvalFactoryNode;

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
    private final EvalFactoryNode evalFactoryNode;
    private final LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes;       // Stores types for filters with tags, single event
    private final LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes;       // Stores types for filters with tags, array event
    private final LinkedHashSet<String> allTags;
    private final boolean suppressSameEventMatches;
    private final boolean discardPartialsOnMatch;
    private static final long serialVersionUID = 1268004301792124753L;

    public PatternStreamSpecCompiled(EvalFactoryNode evalFactoryNode, LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes, LinkedHashSet<String> allTags, ViewSpec[] viewSpecs, String optionalStreamName, StreamSpecOptions streamSpecOptions, boolean suppressSameEventMatches, boolean discardPartialsOnMatch) {
        super(optionalStreamName, viewSpecs, streamSpecOptions);
        this.suppressSameEventMatches = suppressSameEventMatches;
        this.discardPartialsOnMatch = discardPartialsOnMatch;
        this.evalFactoryNode = evalFactoryNode;
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
    public EvalFactoryNode getEvalFactoryNode() {
        return evalFactoryNode;
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
        String[] tags = allTags.toArray(new String[allTags.size()]);
        return new MatchedEventMapMeta(tags, !arrayEventTypes.isEmpty());
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
}
