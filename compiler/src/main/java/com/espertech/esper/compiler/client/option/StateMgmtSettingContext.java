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

/**
 * Provides the environment to {@link StateMgmtSettingOption}.
 */
public class StateMgmtSettingContext extends StatementOptionContextBase {

    private final AppliesTo appliesTo;
    private final StateMgmtSetting configured;

    public StateMgmtSettingContext(StatementRawInfo raw, AppliesTo appliesTo, StateMgmtSetting configured) {
        super(raw);
        this.appliesTo = appliesTo;
        this.configured = configured;
    }

    public AppliesTo getAppliesTo() {
        return appliesTo;
    }

    public StateMgmtSetting getConfigured() {
        return configured;
    }
}
