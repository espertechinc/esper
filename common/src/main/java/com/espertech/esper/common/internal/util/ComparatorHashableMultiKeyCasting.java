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

import com.espertech.esper.common.client.util.HashableMultiKey;

import java.util.Comparator;

/**
 * A comparator on multikeys. The multikeys must contain the same number of values.
 */
public final class ComparatorHashableMultiKeyCasting implements Comparator<Object> {
    private final Comparator<HashableMultiKey> comparator;

    public ComparatorHashableMultiKeyCasting(Comparator<HashableMultiKey> comparator) {
        this.comparator = comparator;
    }

    public final int compare(Object firstValues, Object secondValues) {
        return comparator.compare((HashableMultiKey) firstValues, (HashableMultiKey) secondValues);
    }
}
