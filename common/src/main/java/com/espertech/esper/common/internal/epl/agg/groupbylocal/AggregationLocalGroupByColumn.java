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
package com.espertech.esper.common.internal.epl.agg.groupbylocal;

public class AggregationLocalGroupByColumn {
    private final boolean defaultGroupLevel;
    private final int fieldNum;
    private final int levelNum;

    public AggregationLocalGroupByColumn(boolean defaultGroupLevel, int fieldNum, int levelNum) {
        this.defaultGroupLevel = defaultGroupLevel;
        this.fieldNum = fieldNum;
        this.levelNum = levelNum;
    }

    public boolean isDefaultGroupLevel() {
        return defaultGroupLevel;
    }

    public int getFieldNum() {
        return fieldNum;
    }

    public int getLevelNum() {
        return levelNum;
    }
}
