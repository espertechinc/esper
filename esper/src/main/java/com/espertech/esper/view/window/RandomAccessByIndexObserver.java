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
 * For indicating that the collection has been updated.
 */
public interface RandomAccessByIndexObserver {
    /**
     * Callback to indicate an update
     *
     * @param randomAccessByIndex is the collection
     */
    public void updated(RandomAccessByIndex randomAccessByIndex);
}
