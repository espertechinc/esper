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
package com.espertech.esper.regressionrun.suite.epl;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonDBRef;
import com.espertech.esper.regressionlib.suite.epl.database.EPLDatabaseQueryResultCache;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.util.SupportDatabaseService;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.Properties;

public class TestSuiteEPLDatabaseWConfig extends TestCase {

    public void testEPLDatabaseQueryResultCache() {
        run(new EPLDatabaseQueryResultCache(false, null, 1d, Double.MAX_VALUE, 5000L, 1000, false));
        run(new EPLDatabaseQueryResultCache(true, 100, null, null, 2000L, 1000, false));
        run(new EPLDatabaseQueryResultCache(true, 100, null, null, 7000L, 25000, false));
        run(new EPLDatabaseQueryResultCache(false, null, 2d, 2d, 7000L, 25000, false));
        run(new EPLDatabaseQueryResultCache(false, null, 1d, 1d, 7000L, 25000, true));
    }

    private void run(EPLDatabaseQueryResultCache exec) {
        RegressionSession session = RegressionRunner.session();
        ConfigurationCommonDBRef configDB = getDefaultConfig();
        if (exec.isLru()) {
            configDB.setLRUCache(exec.getLruSize());
        } else {
            configDB.setExpiryTimeCache(exec.getExpiryMaxAgeSeconds(), exec.getExpiryPurgeIntervalSeconds());
        }
        session.getConfiguration().getCommon().addDatabaseReference("MyDB", configDB);
        session.getConfiguration().getCommon().addEventType(SupportBean_S0.class);

        RegressionRunner.run(session, exec);

        session.destroy();
    }

    private ConfigurationCommonDBRef getDefaultConfig() {
        ConfigurationCommonDBRef configDB = new ConfigurationCommonDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationCommonDBRef.ConnectionLifecycleEnum.RETAIN);
        return configDB;
    }
}
