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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTableFactory;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.strategy.*;

import java.util.concurrent.locks.Lock;

public class TableImpl extends TableBase {
    public TableImpl(TableMetaData metaData) {
        super(metaData);
    }

    protected PropertyHashedEventTableFactory setupPrimaryKeyIndexFactory() {
        return new PropertyHashedEventTableFactory(0, metaData.getKeyColumns(), true, metaData.getTableName(), primaryKeyGetter, primaryKeyObjectArrayTransform);
    }

    public TableInstance getTableInstance(int agentInstanceId) {
        return super.getTableInstanceNoRemake(agentInstanceId);
    }

    public TableInstance getTableInstanceNoContext() {
        return super.getTableInstanceNoContextNoRemake();
    }

    public TableAndLockProvider getStateProvider(int agentInstanceId, boolean writesToTables) {
        TableInstance instance = getTableInstance(agentInstanceId);
        Lock lock = writesToTables ? instance.getTableLevelRWLock().writeLock() : instance.getTableLevelRWLock().readLock();
        if (instance instanceof TableInstanceGrouped) {
            return new TableAndLockProviderGroupedImpl(new TableAndLockGrouped(lock, (TableInstanceGrouped) instance));
        } else {
            return new TableAndLockProviderUngroupedImpl(new TableAndLockUngrouped(lock, (TableInstanceUngrouped) instance));
        }
    }
}
