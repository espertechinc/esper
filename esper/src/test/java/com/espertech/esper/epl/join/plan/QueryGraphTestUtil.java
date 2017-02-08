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
package com.espertech.esper.epl.join.plan;

public class QueryGraphTestUtil {

    public static Object[] getStrictKeyProperties(QueryGraph graph, int lookup, int indexed) {
        QueryGraphValue val = graph.getGraphValue(lookup, indexed);
        QueryGraphValuePairHashKeyIndex pair = val.getHashKeyProps();
        return pair.getStrictKeys();
    }

    public static Object[] getIndexProperties(QueryGraph graph, int lookup, int indexed) {
        QueryGraphValue val = graph.getGraphValue(lookup, indexed);
        QueryGraphValuePairHashKeyIndex pair = val.getHashKeyProps();
        return pair.getIndexed();
    }
}
