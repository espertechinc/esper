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
package com.espertech.esper.view.std;

public class GroupByViewAgedEntry {
    private final Object subviewHolder;
    private long lastUpdateTime;

    public GroupByViewAgedEntry(Object subviewHolder, long lastUpdateTime) {
        this.subviewHolder = subviewHolder;
        this.lastUpdateTime = lastUpdateTime;
    }

    public Object getSubviewHolder() {
        return subviewHolder;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
