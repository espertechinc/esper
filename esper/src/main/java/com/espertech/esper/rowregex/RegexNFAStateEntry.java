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

/**
 * State for a partial NFA match.
 */
public class RegexNFAStateEntry {
    private final int matchBeginEventSeqNo;
    private final long matchBeginEventTime;
    private RegexNFAState state;
    private final EventBean[] eventsPerStream;
    private final int[] greedycountPerState;
    private final MultimatchState[] optionalMultiMatches;
    private final Object partitionKey;
    private int matchEndEventSeqNo;

    /**
     * Ctor.
     *
     * @param matchBeginEventSeqNo the event number where the match started
     * @param matchBeginEventTime  the time the first match occured
     * @param state                the current match state
     * @param eventsPerStream      events for each single-match variable
     * @param greedycountPerState  number of greedy matches over all variables
     * @param optionalMultiMatches matches for multirow-variables
     * @param partitionKey         key of partition
     */
    public RegexNFAStateEntry(int matchBeginEventSeqNo,
                              long matchBeginEventTime,
                              RegexNFAState state,
                              EventBean[] eventsPerStream,
                              int[] greedycountPerState,
                              MultimatchState[] optionalMultiMatches,
                              Object partitionKey) {
        this.matchBeginEventSeqNo = matchBeginEventSeqNo;
        this.matchBeginEventTime = matchBeginEventTime;
        this.state = state;
        this.eventsPerStream = eventsPerStream;
        this.greedycountPerState = greedycountPerState;
        this.optionalMultiMatches = optionalMultiMatches;
        this.partitionKey = partitionKey;
    }

    /**
     * Returns the event number of the first matching event.
     *
     * @return event number
     */
    public int getMatchBeginEventSeqNo() {
        return matchBeginEventSeqNo;
    }

    /**
     * Returns the time of the first matching event.
     *
     * @return time
     */
    public long getMatchBeginEventTime() {
        return matchBeginEventTime;
    }

    /**
     * Returns the partial matches.
     *
     * @return state
     */
    public RegexNFAState getState() {
        return state;
    }

    /**
     * Returns the single-variable matches.
     *
     * @return match events
     */
    public EventBean[] getEventsPerStream() {
        return eventsPerStream;
    }

    /**
     * Returns the multirow-variable matches, if any.
     *
     * @return matches
     */
    public MultimatchState[] getOptionalMultiMatches() {
        return optionalMultiMatches;
    }

    /**
     * Returns the count of greedy matches per state.
     *
     * @return greedy counts
     */
    public int[] getGreedycountPerState() {
        return greedycountPerState;
    }

    /**
     * Sets the match end event number.
     *
     * @param matchEndEventSeqNo match end event num
     */
    public void setMatchEndEventSeqNo(int matchEndEventSeqNo) {
        this.matchEndEventSeqNo = matchEndEventSeqNo;
    }

    /**
     * Returns the match end event number.
     *
     * @return num
     */
    public int getMatchEndEventSeqNo() {
        return matchEndEventSeqNo;
    }

    /**
     * Returns the partition key.
     *
     * @return key
     */
    public Object getPartitionKey() {
        return partitionKey;
    }

    public String toString() {
        return "Entry " + state.toString();
    }

    public void setState(RegexNFAState state) {
        this.state = state;
    }
}
