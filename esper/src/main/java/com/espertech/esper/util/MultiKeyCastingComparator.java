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
package com.espertech.esper.util;

import com.espertech.esper.collection.MultiKeyUntyped;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A comparator on multikeys. The multikeys must contain the same
 * number of values.
 */
public final class MultiKeyCastingComparator implements Comparator<Object>, MetaDefItem, Serializable {
    private static final long serialVersionUID = 2914561149171499446L;
    private final Comparator<MultiKeyUntyped> comparator;

    public MultiKeyCastingComparator(Comparator<MultiKeyUntyped> comparator) {
        this.comparator = comparator;
    }

    public final int compare(Object firstValues, Object secondValues) {
        return comparator.compare((MultiKeyUntyped) firstValues, (MultiKeyUntyped) secondValues);
    }
}
