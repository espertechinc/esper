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

import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeForge;

import java.io.StringWriter;
import java.util.Collection;

public class SubordPropRangeKeyForge {
    private final QueryGraphValueEntryRangeForge rangeInfo;
    private final Class coercionType;

    public SubordPropRangeKeyForge(QueryGraphValueEntryRangeForge rangeInfo, Class coercionType) {
        this.rangeInfo = rangeInfo;
        this.coercionType = coercionType;
    }

    public Class getCoercionType() {
        return coercionType;
    }

    public QueryGraphValueEntryRangeForge getRangeInfo() {
        return rangeInfo;
    }

    public String toQueryPlan() {
        return " info " + rangeInfo.toQueryPlan() + " coercion " + coercionType;
    }

    public static String toQueryPlan(Collection<SubordPropRangeKeyForge> rangeDescs) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (SubordPropRangeKeyForge key : rangeDescs) {
            writer.append(delimiter);
            writer.append(key.toQueryPlan());
            delimiter = ", ";
        }
        return writer.toString();
    }
}
