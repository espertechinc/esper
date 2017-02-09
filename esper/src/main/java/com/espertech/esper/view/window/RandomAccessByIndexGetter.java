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
package com.espertech.esper.view.window;

/**
 * Getter that provides an index at runtime.
 */
public class RandomAccessByIndexGetter implements RandomAccessByIndexObserver {
    private RandomAccessByIndex randomAccessByIndex;

    public RandomAccessByIndexGetter() {
    }

    /**
     * Returns the index for access.
     *
     * @return index
     */
    public RandomAccessByIndex getAccessor() {
        return randomAccessByIndex;
    }

    public void updated(RandomAccessByIndex randomAccessByIndex) {
        this.randomAccessByIndex = randomAccessByIndex;
    }
}
