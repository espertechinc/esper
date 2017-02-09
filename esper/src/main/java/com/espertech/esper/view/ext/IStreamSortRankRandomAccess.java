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
package com.espertech.esper.view.ext;

import com.espertech.esper.view.window.RandomAccessByIndex;

import java.util.TreeMap;

/**
 * Provides random access into a rank-window's data.
 */
public interface IStreamSortRankRandomAccess extends RandomAccessByIndex {
    void refresh(TreeMap<Object, Object> sortedEvents, int currentSize, int maxSize);
}
