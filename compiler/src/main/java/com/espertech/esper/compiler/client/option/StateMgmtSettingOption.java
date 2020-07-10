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
package com.espertech.esper.compiler.client.option;

import com.espertech.esper.common.client.annotation.AppliesTo;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.statemgmtsettings.StateMgmtSettingsProxy;

/**
 * Implement this interface to provide or override the state management settings, for use with high-availability only.
 */
public interface StateMgmtSettingOption extends StateMgmtSettingsProxy {
    /**
     * Return a state management setting.
     *
     * @param env information about the state management setting that is being determined
     * @return setting
     */
    StateMgmtSetting getValue(StateMgmtSettingContext env);

    default StateMgmtSetting configure(StatementRawInfo raw, AppliesTo appliesTo, StateMgmtSetting setting) {
        return getValue(new StateMgmtSettingContext(raw, appliesTo, setting));
    }
}
