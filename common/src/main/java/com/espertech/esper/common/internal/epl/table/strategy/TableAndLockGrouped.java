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
package com.espertech.esper.common.internal.epl.table.strategy;

import com.espertech.esper.common.internal.epl.table.core.TableInstanceGrouped;

import java.util.concurrent.locks.Lock;

public class TableAndLockGrouped {
    private final Lock lock;
    private final TableInstanceGrouped grouped;

    public TableAndLockGrouped(Lock lock, TableInstanceGrouped grouped) {
        this.lock = lock;
        this.grouped = grouped;
    }

    public Lock getLock() {
        return lock;
    }

    public TableInstanceGrouped getGrouped() {
        return grouped;
    }
}
