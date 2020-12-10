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
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.epl.resultset.agggrouped.ResultSetProcessorAggregateGroupedForge;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorType;
import com.espertech.esper.common.internal.epl.resultset.rowforall.ResultSetProcessorRowForAllForge;
import com.espertech.esper.common.internal.epl.resultset.rowperevent.ResultSetProcessorRowPerEventForge;
import com.espertech.esper.common.internal.epl.resultset.rowpergroup.ResultSetProcessorRowPerGroupForge;
import com.espertech.esper.common.internal.epl.resultset.rowpergrouprollup.ResultSetProcessorRowPerGroupRollupForge;
import com.espertech.esper.common.internal.epl.resultset.simple.ResultSetProcessorSimpleForge;
import com.espertech.esper.common.internal.fabric.FabricCharge;

public class StateMgmtSettingsProviderResultSetDefault implements StateMgmtSettingsProviderResultSet {
    public final static StateMgmtSettingsProviderResultSetDefault INSTANCE = new StateMgmtSettingsProviderResultSetDefault();

    private StateMgmtSettingsProviderResultSetDefault() {
    }

    public StateMgmtSetting outputLimited(FabricCharge fabricCharge, StatementRawInfo raw, EventType[] eventTypes, EventType resultEventType) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting outputCount(FabricCharge fabricCharge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting outputTime(FabricCharge fabricCharge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting outputExpression(FabricCharge fabricCharge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting outputFirst(FabricCharge fabricCharge, ResultSetProcessorType resultSetProcessorType, EventType[] typesPerStream) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting outputAfter(FabricCharge fabricCharge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rowForAllOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowForAllForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rowForAllOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowForAllForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting aggGroupedOutputFirst(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorAggregateGroupedForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting aggGroupedOutputAllOpt(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorAggregateGroupedForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting aggGroupedOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorAggregateGroupedForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting aggGroupedOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorAggregateGroupedForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rowPerEventOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerEventForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rowPerEventOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerEventForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting simpleOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorSimpleForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting simpleOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorSimpleForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rollupOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupRollupForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rollupOutputFirst(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupRollupForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rollupOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupRollupForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rollupOutputSnapshot(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupRollupForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rowPerGroupOutputFirst(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rowPerGroupOutputAllOpt(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rowPerGroupOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rowPerGroupOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rowPerGroupUnbound(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }
}
