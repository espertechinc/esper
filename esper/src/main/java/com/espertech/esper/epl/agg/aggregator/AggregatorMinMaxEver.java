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
package com.espertech.esper.epl.agg.aggregator;

import com.espertech.esper.type.MinMaxTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Min/max aggregator for all values, not considering events leaving the aggregation (i.e. ever).
 */
public class AggregatorMinMaxEver implements AggregationMethod {
    private static final Logger log = LoggerFactory.getLogger(AggregatorMinMaxEver.class);

    protected final MinMaxTypeEnum minMaxTypeEnum;

    protected Comparable currentMinMax;

    /**
     * Ctor.
     *
     * @param minMaxTypeEnum - enum indicating to return minimum or maximum values
     */
    public AggregatorMinMaxEver(MinMaxTypeEnum minMaxTypeEnum) {
        this.minMaxTypeEnum = minMaxTypeEnum;
    }

    public void clear() {
        currentMinMax = null;
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }
        if (currentMinMax == null) {
            currentMinMax = (Comparable) object;
            return;
        }
        if (minMaxTypeEnum == MinMaxTypeEnum.MAX) {
            if (currentMinMax.compareTo(object) < 0) {
                currentMinMax = (Comparable) object;
            }
        } else {
            if (currentMinMax.compareTo(object) > 0) {
                currentMinMax = (Comparable) object;
            }
        }
    }

    public void leave(Object object) {
        // no-op, this is designed to handle min-max ever
        log.warn(".leave Received remove stream, none was expected");
    }

    public Object getValue() {
        return currentMinMax;
    }

    public void setCurrentMinMax(Comparable currentMinMax) {
        this.currentMinMax = currentMinMax;
    }

    public Comparable getCurrentMinMax() {
        return currentMinMax;
    }
}
