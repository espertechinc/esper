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

import java.io.Serializable;
import java.util.Comparator;

/**
 * A comparator on objects that takes a boolean array for ascending/descending.
 */
public final class ObjectComparator implements Comparator<Object>, Serializable {
    private static final long serialVersionUID = -2139033245746311007L;
    private final boolean isDescendingValue;

    /**
     * Ctor.
     *
     * @param isDescendingValue ascending or descending
     */
    public ObjectComparator(boolean isDescendingValue) {
        this.isDescendingValue = isDescendingValue;
    }

    public final int compare(Object firstValue, Object secondValue) {
        return CollectionUtil.compareValues(firstValue, secondValue, isDescendingValue);
    }
}
