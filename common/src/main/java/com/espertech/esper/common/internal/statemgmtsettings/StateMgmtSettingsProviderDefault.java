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

import com.espertech.esper.common.client.annotation.AppliesTo;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;

public class StateMgmtSettingsProviderDefault implements StateMgmtSettingsProvider {
    public final static StateMgmtSettingsProviderDefault INSTANCE = new StateMgmtSettingsProviderDefault();

    private StateMgmtSettingsProviderDefault() {
    }

    public StateMgmtSetting getView(StatementRawInfo raw, int streamNumber, boolean subquery, boolean grouped, AppliesTo appliesTo) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting getPattern(StatementRawInfo raw, int streamNum, AppliesTo appliesTo) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting getResultSet(StatementRawInfo raw, AppliesTo appliesTo) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting getContext(StatementRawInfo raw, String contextName, AppliesTo contextCategory) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting getAggregation(StatementRawInfo raw, AppliesTo appliesTo) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting getIndex(StatementRawInfo raw, AppliesTo appliesTo) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting getRowRecog(StatementRawInfo raw, AppliesTo appliesTo) {
        return StateMgmtSettingDefault.INSTANCE;
    }
}
