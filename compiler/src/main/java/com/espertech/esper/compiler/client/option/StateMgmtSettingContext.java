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
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.statemgmtsettings.StateMgmtSettingBucket;

/**
 * For internal-use-only and subject-to-change-between-versions: Provides the environment to {@link StateMgmtSettingOption}.
 */
public class StateMgmtSettingContext extends StatementOptionContextBase {

    private final AppliesTo appliesTo;
    private final StateMgmtSettingBucket configured;

    /**
     * Ctor.
     *
     * @param raw        statement info
     * @param appliesTo  applies
     * @param configured config
     */
    public StateMgmtSettingContext(StatementRawInfo raw, AppliesTo appliesTo, StateMgmtSettingBucket configured) {
        super(raw);
        this.appliesTo = appliesTo;
        this.configured = configured;
    }

    /**
     * For internal-use-only and subject-to-change-between-versions: Returns applies-to
     *
     * @return applies-to
     */
    public AppliesTo getAppliesTo() {
        return appliesTo;
    }

    /**
     * For internal-use-only and subject-to-change-between-versions: Returns settings
     *
     * @return settings
     */
    public StateMgmtSettingBucket getConfigured() {
        return configured;
    }
}
