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
package com.espertech.esper.epl.table.mgmt;

import com.espertech.esper.epl.table.upd.TableUpdateStrategyReceiver;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;

public class TableUpdateStrategyReceiverDesc {

    private final TableUpdateStrategyReceiver receiver;
    private final EventBeanUpdateHelper updateHelper;
    private final boolean onMerge;

    public TableUpdateStrategyReceiverDesc(TableUpdateStrategyReceiver receiver, EventBeanUpdateHelper updateHelper, boolean onMerge) {
        this.receiver = receiver;
        this.updateHelper = updateHelper;
        this.onMerge = onMerge;
    }

    public TableUpdateStrategyReceiver getReceiver() {
        return receiver;
    }

    public EventBeanUpdateHelper getUpdateHelper() {
        return updateHelper;
    }

    public boolean isOnMerge() {
        return onMerge;
    }
}
