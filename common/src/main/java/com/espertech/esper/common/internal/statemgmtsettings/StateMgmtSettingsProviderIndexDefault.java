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
package com.espertech.esper.common.internal.statemgmtsettings;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.*;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionCompileTime;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanAttributionKey;
import com.espertech.esper.common.internal.fabric.FabricCharge;

public class StateMgmtSettingsProviderIndexDefault implements StateMgmtSettingsProviderIndex {
    public final static StateMgmtSettingsProviderIndexDefault INSTANCE = new StateMgmtSettingsProviderIndexDefault();

    private StateMgmtSettingsProviderIndexDefault() {
    }

    public StateMgmtSetting unindexed(FabricCharge fabricCharge, QueryPlanAttributionKey attributionKey, EventType eventType, StatementRawInfo raw) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting indexHash(FabricCharge fabricCharge, QueryPlanAttributionKey attributionKey, String indexName, EventType eventType, StateMgmtIndexDescHash indexDesc, StatementRawInfo raw) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting indexInSingle(FabricCharge fabricCharge, QueryPlanAttributionKey attributionKey, EventType eventType, StateMgmtIndexDescInSingle indexDesc, StatementRawInfo raw) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting indexInMulti(FabricCharge fabricCharge, QueryPlanAttributionKey attributionKey, EventType eventType, StateMgmtIndexDescInMulti indexDesc, StatementRawInfo raw) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting sorted(FabricCharge fabricCharge, QueryPlanAttributionKey attributionKey, String indexName, EventType eventType, StateMgmtIndexDescSorted indexDesc, StatementRawInfo raw) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting composite(FabricCharge fabricCharge, QueryPlanAttributionKey attributionKey, String indexName, EventType eventType, StateMgmtIndexDescComposite indexDesc, StatementRawInfo raw) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting advanced(FabricCharge fabricCharge, QueryPlanAttributionKey attributionKey, String indexName, EventType eventType, EventAdvancedIndexProvisionCompileTime advancedIndexProvisionDesc, StatementRawInfo raw) {
        return StateMgmtSettingDefault.INSTANCE;
    }
}
