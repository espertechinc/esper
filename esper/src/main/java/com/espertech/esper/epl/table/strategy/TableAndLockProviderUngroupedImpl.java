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
package com.espertech.esper.epl.table.strategy;

public class TableAndLockProviderUngroupedImpl implements TableAndLockProviderUngrouped {
    private final TableAndLockUngrouped pair;

    public TableAndLockProviderUngroupedImpl(TableAndLockUngrouped pair) {
        this.pair = pair;
    }

    public TableAndLockUngrouped get() {
        return pair;
    }
}
