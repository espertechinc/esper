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
package com.espertech.esper.epl.lookup;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class SubordPropPlan implements Serializable {
    private static final long serialVersionUID = -8857080184548049650L;

    private final Map<String, SubordPropHashKey> hashProps;
    private final Map<String, SubordPropRangeKey> rangeProps;
    private final SubordPropInKeywordSingleIndex inKeywordSingleIndex;
    private final SubordPropInKeywordMultiIndex inKeywordMultiIndex;

    public SubordPropPlan() {
        hashProps = Collections.<String, SubordPropHashKey>emptyMap();
        rangeProps = Collections.<String, SubordPropRangeKey>emptyMap();
        inKeywordSingleIndex = null;
        inKeywordMultiIndex = null;
    }

    public SubordPropPlan(Map<String, SubordPropHashKey> hashProps, Map<String, SubordPropRangeKey> rangeProps, SubordPropInKeywordSingleIndex inKeywordSingleIndex, SubordPropInKeywordMultiIndex inKeywordMultiIndex) {
        this.hashProps = hashProps;
        this.rangeProps = rangeProps;
        this.inKeywordSingleIndex = inKeywordSingleIndex;
        this.inKeywordMultiIndex = inKeywordMultiIndex;
    }

    public Map<String, SubordPropRangeKey> getRangeProps() {
        return rangeProps;
    }

    public Map<String, SubordPropHashKey> getHashProps() {
        return hashProps;
    }

    public SubordPropInKeywordSingleIndex getInKeywordSingleIndex() {
        return inKeywordSingleIndex;
    }

    public SubordPropInKeywordMultiIndex getInKeywordMultiIndex() {
        return inKeywordMultiIndex;
    }
}
