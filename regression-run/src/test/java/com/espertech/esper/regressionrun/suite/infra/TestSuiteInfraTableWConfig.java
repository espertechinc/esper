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

import com.espertech.esper.regressionlib.suite.infra.tbl.InfraTableMTGroupedMergeReadMergeWriteSecondaryIndexUpd;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportTopGroupSubGroupEvent;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

// see INFRA suite for additional Table tests
public class TestSuiteInfraTableWConfig extends TestCase {
    public void testInfraTableMTGroupedMergeReadMergeWriteSecondaryIndexUpd() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getRuntime().getExecution().setFairlock(true);
        session.getConfiguration().getCommon().addEventType(SupportTopGroupSubGroupEvent.class);
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCommon().addEventType(SupportBean_S0.class);
        RegressionRunner.run(session, new InfraTableMTGroupedMergeReadMergeWriteSecondaryIndexUpd());
        session.destroy();
    }
}
