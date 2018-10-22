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
package com.espertech.esper.common.internal.epl.lookupplan;

import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryCustomKeyForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryCustomOperationForge;

import java.util.Collections;
import java.util.Map;

public class SubordPropPlan {
    private final Map<String, SubordPropHashKeyForge> hashProps;
    private final Map<String, SubordPropRangeKeyForge> rangeProps;
    private final SubordPropInKeywordSingleIndex inKeywordSingleIndex;
    private final SubordPropInKeywordMultiIndex inKeywordMultiIndex;
    private final Map<QueryGraphValueEntryCustomKeyForge, QueryGraphValueEntryCustomOperationForge> customIndexOps;

    public SubordPropPlan() {
        hashProps = Collections.<String, SubordPropHashKeyForge>emptyMap();
        rangeProps = Collections.<String, SubordPropRangeKeyForge>emptyMap();
        inKeywordSingleIndex = null;
        inKeywordMultiIndex = null;
        customIndexOps = null;
    }

    public SubordPropPlan(Map<String, SubordPropHashKeyForge> hashProps, Map<String, SubordPropRangeKeyForge> rangeProps, SubordPropInKeywordSingleIndex inKeywordSingleIndex, SubordPropInKeywordMultiIndex inKeywordMultiIndex, Map<QueryGraphValueEntryCustomKeyForge, QueryGraphValueEntryCustomOperationForge> customIndexOps) {
        this.hashProps = hashProps;
        this.rangeProps = rangeProps;
        this.inKeywordSingleIndex = inKeywordSingleIndex;
        this.inKeywordMultiIndex = inKeywordMultiIndex;
        this.customIndexOps = customIndexOps;
    }

    public Map<String, SubordPropRangeKeyForge> getRangeProps() {
        return rangeProps;
    }

    public Map<String, SubordPropHashKeyForge> getHashProps() {
        return hashProps;
    }

    public SubordPropInKeywordSingleIndex getInKeywordSingleIndex() {
        return inKeywordSingleIndex;
    }

    public SubordPropInKeywordMultiIndex getInKeywordMultiIndex() {
        return inKeywordMultiIndex;
    }

    public Map<QueryGraphValueEntryCustomKeyForge, QueryGraphValueEntryCustomOperationForge> getCustomIndexOps() {
        return customIndexOps;
    }
}
