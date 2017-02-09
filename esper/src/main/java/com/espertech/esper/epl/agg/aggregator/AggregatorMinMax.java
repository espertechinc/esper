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

import com.espertech.esper.collection.SortedRefCountedSet;
import com.espertech.esper.type.MinMaxTypeEnum;

/**
 * Min/max aggregator for all values.
 */
public class AggregatorMinMax implements AggregationMethod {
    protected final MinMaxTypeEnum minMaxTypeEnum;

    protected SortedRefCountedSet<Object> refSet;

    /**
     * Ctor.
     *
     * @param minMaxTypeEnum - enum indicating to return minimum or maximum values
     */
    public AggregatorMinMax(MinMaxTypeEnum minMaxTypeEnum) {
        this.minMaxTypeEnum = minMaxTypeEnum;
        this.refSet = new SortedRefCountedSet<Object>();
    }

    public void clear() {
        refSet.clear();
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }
        refSet.add(object);
    }

    public void leave(Object object) {
        if (object == null) {
            return;
        }
        refSet.remove(object);
    }

    public Object getValue() {
        if (minMaxTypeEnum == MinMaxTypeEnum.MAX) {
            return refSet.maxValue();
        } else {
            return refSet.minValue();
        }
    }

    public SortedRefCountedSet<Object> getRefSet() {
        return refSet;
    }
}
