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
package com.espertech.esper.view.internal;

import com.espertech.esper.collection.MixedEventBeanAndCollectionIteratorBase;

import java.util.ArrayDeque;

public final class TimeWindowIterator extends MixedEventBeanAndCollectionIteratorBase {
    /**
     * Ctor.
     *
     * @param window is the time-slotted collection
     */
    public TimeWindowIterator(ArrayDeque<TimeWindowPair> window) {
        super(window.iterator());
        init();
    }

    protected Object getValue(Object iteratorKeyValue) {
        return ((TimeWindowPair) iteratorKeyValue).getEventHolder();
    }
}
