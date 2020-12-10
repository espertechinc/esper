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
package com.espertech.esper.common.internal.epl.resultset.core;

import com.espertech.esper.common.internal.epl.agg.core.AggregationAttributionKey;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAttributionKeyStatement;

public class ResultSetProcessorAttributionKeyStatement implements ResultSetProcessorAttributionKey  {
    public final static ResultSetProcessorAttributionKeyStatement INSTANCE = new ResultSetProcessorAttributionKeyStatement();

    private ResultSetProcessorAttributionKeyStatement() {
    }

    public AggregationAttributionKey getAggregationAttributionKey() {
        return AggregationAttributionKeyStatement.INSTANCE;
    }
}
