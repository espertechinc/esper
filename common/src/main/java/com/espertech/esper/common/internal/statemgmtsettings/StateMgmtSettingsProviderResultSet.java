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

public interface StateMgmtSettingsProviderResultSet {
    StateMgmtSetting simpleOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorSimpleForge forge);
    StateMgmtSetting simpleOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorSimpleForge forge);

    StateMgmtSetting rowForAllOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowForAllForge forge);
    StateMgmtSetting rowForAllOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowForAllForge forge);

    StateMgmtSetting aggGroupedOutputFirst(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorAggregateGroupedForge forge);
    StateMgmtSetting aggGroupedOutputAllOpt(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorAggregateGroupedForge forge);
    StateMgmtSetting aggGroupedOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorAggregateGroupedForge forge);
    StateMgmtSetting aggGroupedOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorAggregateGroupedForge forge);

    StateMgmtSetting rowPerEventOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerEventForge forge);
    StateMgmtSetting rowPerEventOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerEventForge forge);

    StateMgmtSetting rowPerGroupOutputFirst(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupForge forge);
    StateMgmtSetting rowPerGroupOutputAllOpt(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupForge forge);
    StateMgmtSetting rowPerGroupOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupForge forge);
    StateMgmtSetting rowPerGroupOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupForge forge);
    StateMgmtSetting rowPerGroupUnbound(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupForge forge);

    StateMgmtSetting rollupOutputLast(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupRollupForge forge);
    StateMgmtSetting rollupOutputAll(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupRollupForge forge);
    StateMgmtSetting rollupOutputSnapshot(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupRollupForge forge);
    StateMgmtSetting rollupOutputFirst(FabricCharge fabricCharge, StatementRawInfo raw, ResultSetProcessorRowPerGroupRollupForge forge);

    StateMgmtSetting outputLimited(FabricCharge fabricCharge, StatementRawInfo raw, EventType[] eventTypes, EventType resultEventType);
    StateMgmtSetting outputCount(FabricCharge fabricCharge);
    StateMgmtSetting outputTime(FabricCharge fabricCharge);
    StateMgmtSetting outputExpression(FabricCharge fabricCharge);
    StateMgmtSetting outputFirst(FabricCharge fabricCharge, ResultSetProcessorType resultSetProcessorType, EventType[] typesPerStream);
    StateMgmtSetting outputAfter(FabricCharge fabricCharge);
}
