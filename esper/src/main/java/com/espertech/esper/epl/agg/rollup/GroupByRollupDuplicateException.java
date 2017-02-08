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
package com.espertech.esper.epl.agg.rollup;

public class GroupByRollupDuplicateException extends Exception {
    private static final long serialVersionUID = -8222680920615261088L;
    private int[] indexes;

    public GroupByRollupDuplicateException(int[] indexes) {
        this.indexes = indexes;
    }

    public int[] getIndexes() {
        return indexes;
    }
}
