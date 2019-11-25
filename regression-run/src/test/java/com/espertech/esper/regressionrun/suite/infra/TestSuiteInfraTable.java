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
package com.espertech.esper.regressionrun.suite.infra;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.suite.infra.tbl.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportCountBackAggregationFunctionForge;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportSimpleWordCSVForge;
import com.espertech.esper.regressionlib.support.extend.aggmultifunc.SupportAggMFMultiRTForge;
import com.espertech.esper.regressionlib.support.extend.aggmultifunc.SupportReferenceCountedMapForge;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

// see INFRA suite for additional Table tests
public class TestSuiteInfraTable extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testInfraTableInsertInto() {
        RegressionRunner.run(session, InfraTableInsertInto.executions());
    }

    public void testInfraTableIntoTable() {
        RegressionRunner.run(session, InfraTableIntoTable.executions());
    }

    public void testInfraTableOnMerge() {
        RegressionRunner.run(session, InfraTableOnMerge.executions());
    }

    public void testInfraTableAccessAggregationState() {
        RegressionRunner.run(session, InfraTableAccessAggregationState.executions());
    }

    public void testInfraTableAccessCore() {
        RegressionRunner.run(session, InfraTableAccessCore.executions());
    }

    public void testInfraTableNonAccessDotSubqueryAndJoin() {
        RegressionRunner.run(session, new InfraTableNonAccessDotSubqueryAndJoin());
    }

    public void testInfraTableContext() {
        RegressionRunner.run(session, InfraTableContext.executions());
    }

    public void testInfraTableCountMinSketch() {
        RegressionRunner.run(session, InfraTableCountMinSketch.executions());
    }

    public void testInfraTableAccessDotMethod() {
        RegressionRunner.run(session, InfraTableAccessDotMethod.executions());
    }

    public void testInfraTableDocSamples() {
        RegressionRunner.run(session, InfraTableDocSamples.executions());
    }

    public void testInfraTableFAFExecuteQuery() {
        RegressionRunner.run(session, InfraTableFAFExecuteQuery.executions());
    }

    public void testInfraTableFilters() {
        RegressionRunner.run(session, new InfraTableFilters());
    }

    public void testInfraTableInvalid() {
        RegressionRunner.run(session, InfraTableInvalid.executions());
    }

    public void testInfraTableIterate() {
        RegressionRunner.run(session, new InfraTableIterate());
    }

    public void testInfraTableJoin() {
        RegressionRunner.run(session, InfraTableJoin.executions());
    }

    public void testInfraTableOnDelete() {
        RegressionRunner.run(session, InfraTableOnDelete.executions());
    }

    public void testInfraTableOnSelect() {
        RegressionRunner.run(session, new InfraTableOnSelect());
    }

    public void testInfraTableOnUpdate() {
        RegressionRunner.run(session, InfraTableOnUpdate.executions());
    }

    public void testInfraTableOutputRateLimiting() {
        RegressionRunner.run(session, new InfraTableOutputRateLimiting());
    }

    public void testInfraTablePlugInAggregation() {
        RegressionRunner.run(session, InfraTablePlugInAggregation.executions());
    }

    public void testInfraTableRollup() {
        RegressionRunner.run(session, InfraTableRollup.executions());
    }

    public void testInfraTableSubquery() {
        RegressionRunner.run(session, InfraTableSubquery.executions());
    }

    public void testInfraTableUpdateAndIndex() {
        RegressionRunner.run(session, InfraTableUpdateAndIndex.executions());
    }

    public void testInfraTableWNamedWindow() {
        RegressionRunner.run(session, new InfraTableWNamedWindow());
    }

    public void testInfraTableSelect() {
        RegressionRunner.run(session, InfraTableSelect.executions());
    }

    public void testInfraTableMTAccessReadMergeWriteInsertDeleteRowVisible() {
        RegressionRunner.run(session, new InfraTableMTAccessReadMergeWriteInsertDeleteRowVisible());
    }

    public void testInfraTableMTGroupedAccessReadIntoTableWriteAggColConsistency() {
        RegressionRunner.run(session, new InfraTableMTGroupedAccessReadIntoTableWriteAggColConsistency());
    }

    public void testInfraTableMTGroupedAccessReadIntoTableWriteNewRowCreation() {
        RegressionRunner.run(session, new InfraTableMTGroupedAccessReadIntoTableWriteNewRowCreation());
    }

    public void testInfraTableMTGroupedFAFReadFAFWriteChain() {
        RegressionRunner.run(session, new InfraTableMTGroupedFAFReadFAFWriteChain());
    }

    public void testInfraTableMTGroupedJoinReadMergeWriteSecondaryIndexUpd() {
        RegressionRunner.run(session, new InfraTableMTGroupedJoinReadMergeWriteSecondaryIndexUpd());
    }

    public void testInfraTableMTGroupedSubqueryReadInsertIntoWriteConcurr() {
        RegressionRunner.run(session, new InfraTableMTGroupedSubqueryReadInsertIntoWriteConcurr());
    }

    public void testInfraTableMTGroupedSubqueryReadMergeWriteSecondaryIndexUpd() {
        RegressionRunner.run(session, new InfraTableMTGroupedSubqueryReadMergeWriteSecondaryIndexUpd());
    }

    public void testInfraTableMTGroupedWContextIntoTableWriteAsContextTable() {
        RegressionRunner.run(session, new InfraTableMTGroupedWContextIntoTableWriteAsContextTable());
    }

    public void testInfraTableMTGroupedWContextIntoTableWriteAsSharedTable() {
        RegressionRunner.run(session, new InfraTableMTGroupedWContextIntoTableWriteAsSharedTable());
    }

    public void testInfraTableMTUngroupedAccessReadInotTableWriteIterate() {
        RegressionRunner.run(session, new InfraTableMTUngroupedAccessReadInotTableWriteIterate());
    }

    public void testInfraTableMTUngroupedAccessReadMergeWrite() {
        RegressionRunner.run(session, new InfraTableMTUngroupedAccessReadMergeWrite());
    }

    public void testInfraTableMTUngroupedJoinColumnConsistency() {
        RegressionRunner.run(session, new InfraTableMTUngroupedJoinColumnConsistency());
    }

    public void testInfraTableMTUngroupedSubqueryReadMergeWriteColumnUpd() {
        RegressionRunner.run(session, new InfraTableMTUngroupedSubqueryReadMergeWriteColumnUpd());
    }

    public void testInfraTableMTUngroupedIntoTableWriteMultiWriterAgg() {
        RegressionRunner.run(session, new InfraTableMTUngroupedIntoTableWriteMultiWriterAgg());
    }

    public void testInfraTableMTUngroupedAccessWithinRowFAFConsistency() {
        RegressionRunner.run(session, new InfraTableMTUngroupedAccessWithinRowFAFConsistency());
    }

    public void testInfraTableMTUngroupedAccessReadIntoTableWriteFilterUse() {
        RegressionRunner.run(session, new InfraTableMTUngroupedAccessReadIntoTableWriteFilterUse());
    }

    public void testInfraTableResetAggregationState() {
        RegressionRunner.run(session, InfraTableResetAggregationState.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class,
            SupportIntrusionEvent.class, SupportTrafficEvent.class, SupportMySortValueEvent.class,
            SupportBean_S2.class, SupportBeanSimple.class, SupportByteArrEventStringId.class,
            SupportBeanRange.class, SupportTwoKeyEvent.class, SupportCtorSB2WithObjectArray.class,
            Support10ColEvent.class, SupportTopGroupSubGroupEvent.class, SupportBeanNumeric.class,
            SupportEventWithManyArray.class, SupportEventWithManyArray.class, SupportEventWithIntArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configuration.getCompiler().addPlugInSingleRowFunction("singlerow", InfraTableInvalid.class.getName(), "mySingleRowFunction");
        configuration.getCompiler().addPlugInSingleRowFunction("pluginServiceEventBean", InfraTableSelect.class.getName(), "myServiceEventBean");
        configuration.getCompiler().addPlugInSingleRowFunction("toIntArray", InfraTableOnUpdate.class.getName(), "toIntArray");

        configuration.getCompiler().addPlugInAggregationFunctionForge("myaggsingle", SupportCountBackAggregationFunctionForge.class.getName());
        configuration.getCompiler().addPlugInAggregationFunctionForge("csvWords", SupportSimpleWordCSVForge.class.getName());

        ConfigurationCompilerPlugInAggregationMultiFunction config = new ConfigurationCompilerPlugInAggregationMultiFunction(
            "referenceCountedMap".split(","), SupportReferenceCountedMapForge.class.getName());
        configuration.getCompiler().addPlugInAggregationMultiFunction(config);
        ConfigurationCompilerPlugInAggregationMultiFunction configMultiFuncAgg = new ConfigurationCompilerPlugInAggregationMultiFunction("se1".split(","), SupportAggMFMultiRTForge.class.getName());
        configuration.getCompiler().addPlugInAggregationMultiFunction(configMultiFuncAgg);

        configuration.getCommon().getLogging().setEnableQueryPlan(true);
        configuration.getCommon().addImport(SupportStaticMethodLib.class);

        configuration.getCompiler().getByteCode().setAllowSubscriber(true);
    }
}
