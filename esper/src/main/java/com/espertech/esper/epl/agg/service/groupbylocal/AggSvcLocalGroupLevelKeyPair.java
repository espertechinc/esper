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
package com.espertech.esper.epl.agg.service.groupbylocal;

public class AggSvcLocalGroupLevelKeyPair {
    private final int level;
    private final Object key;

    public AggSvcLocalGroupLevelKeyPair(int level, Object key) {
        this.level = level;
        this.key = key;
    }

    public int getLevel() {
        return level;
    }

    public Object getKey() {
        return key;
    }
}
