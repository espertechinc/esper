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
import java.text.Collator;
import java.util.Comparator;

/**
 * A comparator on objects that takes a boolean array for ascending/descending.
 */
public final class ObjectCollatingComparator implements Comparator<Object>, Serializable {
    private static final long serialVersionUID = 2147404623473097358L;
    private final boolean isDescendingValue;
    private transient Collator collator = null;

    /**
     * Ctor.
     *
     * @param isDescendingValue ascending or descending
     */
    public ObjectCollatingComparator(boolean isDescendingValue) {
        this.isDescendingValue = isDescendingValue;
        collator = Collator.getInstance();
    }

    public final int compare(Object firstValue, Object secondValue) {
        return CollectionUtil.compareValuesCollated(firstValue, secondValue, isDescendingValue, collator);
    }
}
