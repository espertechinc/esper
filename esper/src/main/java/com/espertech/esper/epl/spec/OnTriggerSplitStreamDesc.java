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

import java.util.List;

/**
 * Specification for the on-select splitstream statement.
 */
public class OnTriggerSplitStreamDesc extends OnTriggerDesc {
    private boolean isFirst;
    private final List<OnTriggerSplitStream> splitStreams;
    private static final long serialVersionUID = 794886832792005103L;

    /**
     * Ctor.
     *
     * @param onTriggerType type of trigger
     * @param isFirst       true for use the first-matching where clause, false for all
     * @param splitStreams  streams
     */
    public OnTriggerSplitStreamDesc(OnTriggerType onTriggerType, boolean isFirst, List<OnTriggerSplitStream> splitStreams) {
        super(onTriggerType);
        this.isFirst = isFirst;
        this.splitStreams = splitStreams;
    }

    /**
     * Returns the remaining insert-into and select-clauses in the split-stream clause.
     *
     * @return clauses.
     */
    public List<OnTriggerSplitStream> getSplitStreams() {
        return splitStreams;
    }

    /**
     * Returns indicator whether only the first or all where-clauses are triggering.
     *
     * @return first or all
     */
    public boolean isFirst() {
        return isFirst;
    }
}
