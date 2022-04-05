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
package com.espertech.esper.common.client.metric;

/**
 * Represents a statement in the query API for statement metrics
 */
public class EPMetricsStatement {
    private final StatementMetric metric;

    /**
     * Ctor.
     * @param metric metric
     */
    public EPMetricsStatement(StatementMetric metric) {
        this.metric = metric;
    }

    /**
     * Returns the metrics object
     * @return metrics
     */
    public StatementMetric getMetric() {
        return metric;
    }
}
