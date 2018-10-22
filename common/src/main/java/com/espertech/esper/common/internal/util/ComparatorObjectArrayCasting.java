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
package com.espertech.esper.common.internal.util;

import java.util.Comparator;

/**
 * A comparator on multikeys. The multikeys must contain the same number of values.
 */
public final class ComparatorObjectArrayCasting implements Comparator<Object> {
    private final Comparator<Object[]> comparator;

    public ComparatorObjectArrayCasting(Comparator<Object[]> comparator) {
        this.comparator = comparator;
    }

    public final int compare(Object firstValues, Object secondValues) {
        return comparator.compare((Object[]) firstValues, (Object[]) secondValues);
    }
}
