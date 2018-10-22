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
package com.espertech.esper.common.internal.supportunit.util;

import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValuePairHashKeyIndexForge;

public class SupportQueryGraphTestUtil {

    public static Object[] getStrictKeyProperties(QueryGraphForge graph, int lookup, int indexed) {
        QueryGraphValueForge val = graph.getGraphValue(lookup, indexed);
        QueryGraphValuePairHashKeyIndexForge pair = val.getHashKeyProps();
        return pair.getStrictKeys();
    }

    public static Object[] getIndexProperties(QueryGraphForge graph, int lookup, int indexed) {
        QueryGraphValueForge val = graph.getGraphValue(lookup, indexed);
        QueryGraphValuePairHashKeyIndexForge pair = val.getHashKeyProps();
        return pair.getIndexed();
    }
}
