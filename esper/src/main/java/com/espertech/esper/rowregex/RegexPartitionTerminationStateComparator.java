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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.event.EventBeanUtility;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public class RegexPartitionTerminationStateComparator implements Comparator<RegexNFAStateEntry> {
    private final int[] multimatchStreamNumToVariable;
    private final LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams;

    public RegexPartitionTerminationStateComparator(int[] multimatchStreamNumToVariable, LinkedHashMap<String, Pair<Integer, Boolean>> variableStreams) {
        this.multimatchStreamNumToVariable = multimatchStreamNumToVariable;
        this.variableStreams = variableStreams;
    }

    public int compare(RegexNFAStateEntry o1, RegexNFAStateEntry o2) {
        return compareTerminationStateToEndState(o1, o2) ? 0 : 1;
    }

    // End-state may have less events then the termination state
    public boolean compareTerminationStateToEndState(RegexNFAStateEntry terminationState, RegexNFAStateEntry endState) {
        if (terminationState.getMatchBeginEventSeqNo() != endState.getMatchBeginEventSeqNo()) {
            return false;
        }
        for (Map.Entry<String, Pair<Integer, Boolean>> entry : variableStreams.entrySet()) {
            int stream = entry.getValue().getFirst();
            boolean multi = entry.getValue().getSecond();
            if (multi) {
                EventBean[] termStreamEvents = EventRowRegexNFAViewUtil.getMultimatchArray(multimatchStreamNumToVariable, terminationState, stream);
                EventBean[] endStreamEvents = EventRowRegexNFAViewUtil.getMultimatchArray(multimatchStreamNumToVariable, endState, stream);
                if (endStreamEvents != null) {
                    if (termStreamEvents == null) {
                        return false;
                    }
                    for (int i = 0; i < endStreamEvents.length; i++) {
                        if (termStreamEvents.length > i && !EventBeanUtility.eventsAreEqualsAllowNull(endStreamEvents[i], termStreamEvents[i])) {
                            return false;
                        }
                    }
                }
            } else {
                EventBean termStreamEvent = terminationState.getEventsPerStream()[stream];
                EventBean endStreamEvent = endState.getEventsPerStream()[stream];
                if (!EventBeanUtility.eventsAreEqualsAllowNull(endStreamEvent, termStreamEvent)) {
                    return false;
                }
            }
        }
        return true;
    }
}
