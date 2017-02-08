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

import com.espertech.esper.epl.table.mgmt.TableStateInstanceUngrouped;

import java.util.concurrent.locks.Lock;

public class TableAndLockUngrouped {
    private final Lock lock;
    private final TableStateInstanceUngrouped ungrouped;

    public TableAndLockUngrouped(Lock lock, TableStateInstanceUngrouped ungrouped) {
        this.lock = lock;
        this.ungrouped = ungrouped;
    }

    public Lock getLock() {
        return lock;
    }

    public TableStateInstanceUngrouped getUngrouped() {
        return ungrouped;
    }
}
