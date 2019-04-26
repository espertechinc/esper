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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommon;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonDBRef;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.suite.epl.database.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.util.SupportDataSourceFactory;
import com.espertech.esper.regressionlib.support.util.SupportDatabaseService;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.Properties;

public class TestSuiteEPLDatabase extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEPLDatabaseJoin() {
        RegressionRunner.run(session, EPLDatabaseJoin.executions());
    }

    public void testEPLDatabase2StreamOuterJoin() {
        RegressionRunner.run(session, EPLDatabase2StreamOuterJoin.executions());
    }

    public void testEPLDatabaseOuterJoinWCache() {
        RegressionRunner.run(session, new EPLDatabaseOuterJoinWCache());
    }

    public void testEPLDatabase3StreamOuterJoin() {
        RegressionRunner.run(session, EPLDatabase3StreamOuterJoin.executions());
    }

    public void testEPLDatabaseDataSourceFactory() {
        RegressionRunner.run(session, new EPLDatabaseDataSourceFactory());
    }

    public void testEPLDatabaseJoinInsertInto() {
        RegressionRunner.run(session, new EPLDatabaseJoinInsertInto());
    }

    public void testEPLDatabaseJoinOptions() {
        RegressionRunner.run(session, EPLDatabaseJoinOptions.executions());
    }

    public void testEPLDatabaseJoinOptionUppercase() {
        RegressionRunner.run(session, new EPLDatabaseJoinOptionUppercase());
    }

    public void testEPLDatabaseJoinOptionLowercase() {
        RegressionRunner.run(session, new EPLDatabaseJoinOptionLowercase());
    }

    public void testEPLDatabaseJoinPerfNoCache() {
        RegressionRunner.run(session, new EPLDatabaseJoinPerfNoCache());
    }

    public void testEPLDatabaseJoinPerfWithCache() {
        RegressionRunner.run(session, EPLDatabaseJoinPerfWithCache.executions());
    }

    public void testEPLDatabaseHintHook() {
        RegressionRunner.run(session, EPLDatabaseHintHook.executions());
    }

    public void testEPLDatabaseNoJoinIteratePerf() {
        RegressionRunner.run(session, new EPLDatabaseNoJoinIteratePerf());
    }

    public void testEPLDatabaseNoJoinIterate() {
        RegressionRunner.run(session, EPLDatabaseNoJoinIterate.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBeanTwo.class, SupportBean_A.class,
            SupportBeanRange.class, SupportBean_S0.class, SupportBeanComplexProps.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        ConfigurationCommon common = configuration.getCommon();
        common.addVariable("myvariableOCC", int.class, 10);
        common.addVariable("myvariableIPC", String.class, "x10");
        common.addVariable("myvariableORC", int.class, 10);

        ConfigurationCommonDBRef configDBWithRetain = new ConfigurationCommonDBRef();
        configDBWithRetain.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDBWithRetain.setConnectionLifecycleEnum(ConfigurationCommonDBRef.ConnectionLifecycleEnum.RETAIN);
        configuration.getCommon().addDatabaseReference("MyDBWithRetain", configDBWithRetain);

        Properties props = new Properties();
        props.put("driverClassName", SupportDatabaseService.DRIVER);
        props.put("url", SupportDatabaseService.FULLURL);
        props.put("username", SupportDatabaseService.DBUSER);
        props.put("password", SupportDatabaseService.DBPWD);
        ConfigurationCommonDBRef configDBWithPooledWithLRU100 = new ConfigurationCommonDBRef();
        configDBWithPooledWithLRU100.setDataSourceFactory(props, SupportDataSourceFactory.class.getName());
        configDBWithPooledWithLRU100.setConnectionLifecycleEnum(ConfigurationCommonDBRef.ConnectionLifecycleEnum.POOLED);
        configDBWithPooledWithLRU100.setLRUCache(100);
        configuration.getCommon().addDatabaseReference("MyDBWithPooledWithLRU100", configDBWithPooledWithLRU100);

        ConfigurationCommonDBRef configDBWithTxnIso1WithReadOnly = new ConfigurationCommonDBRef();
        configDBWithTxnIso1WithReadOnly.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDBWithTxnIso1WithReadOnly.setConnectionLifecycleEnum(ConfigurationCommonDBRef.ConnectionLifecycleEnum.RETAIN);
        configDBWithTxnIso1WithReadOnly.setConnectionCatalog("test");
        configDBWithTxnIso1WithReadOnly.setConnectionReadOnly(true);
        configDBWithTxnIso1WithReadOnly.setConnectionTransactionIsolation(1);
        configDBWithTxnIso1WithReadOnly.setConnectionAutoCommit(true);
        configuration.getCommon().addDatabaseReference("MyDBWithTxnIso1WithReadOnly", configDBWithTxnIso1WithReadOnly);

        ConfigurationCommonDBRef dbconfigLowerCase = getDBConfig();
        dbconfigLowerCase.setColumnChangeCase(ConfigurationCommonDBRef.ColumnChangeCaseEnum.LOWERCASE);
        dbconfigLowerCase.addSqlTypesBinding(java.sql.Types.INTEGER, "string");
        configuration.getCommon().addDatabaseReference("MyDBLowerCase", dbconfigLowerCase);

        ConfigurationCommonDBRef dbconfigUpperCase = getDBConfig();
        dbconfigUpperCase.setColumnChangeCase(ConfigurationCommonDBRef.ColumnChangeCaseEnum.UPPERCASE);
        configuration.getCommon().addDatabaseReference("MyDBUpperCase", dbconfigUpperCase);

        ConfigurationCommonDBRef dbconfigPlain = getDBConfig();
        configuration.getCommon().addDatabaseReference("MyDBPlain", dbconfigPlain);

        ConfigurationCommonDBRef configDBPooled = new ConfigurationCommonDBRef();
        configDBPooled.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDBPooled.setConnectionLifecycleEnum(ConfigurationCommonDBRef.ConnectionLifecycleEnum.POOLED);
        configuration.getCommon().addDatabaseReference("MyDBPooled", configDBPooled);

        ConfigurationCommonDBRef configDBWithLRU100000 = new ConfigurationCommonDBRef();
        configDBWithLRU100000.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDBWithLRU100000.setConnectionLifecycleEnum(ConfigurationCommonDBRef.ConnectionLifecycleEnum.RETAIN);
        configDBWithLRU100000.setLRUCache(100000);
        configuration.getCommon().addDatabaseReference("MyDBWithLRU100000", configDBWithLRU100000);

        ConfigurationCommonDBRef configDBWithExpiryTime = new ConfigurationCommonDBRef();
        configDBWithExpiryTime.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDBWithExpiryTime.setConnectionCatalog("test");
        configDBWithExpiryTime.setExpiryTimeCache(60, 120);
        configuration.getCommon().addDatabaseReference("MyDBWithExpiryTime", configDBWithExpiryTime);

        configuration.getCommon().getLogging().setEnableQueryPlan(true);
        configuration.getCommon().getLogging().setEnableJDBC(true);
    }

    protected static ConfigurationCommonDBRef getDBConfig() {
        ConfigurationCommonDBRef configDB = new ConfigurationCommonDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationCommonDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setConnectionCatalog("test");
        configDB.setConnectionReadOnly(true);
        configDB.setConnectionAutoCommit(true);
        return configDB;
    }
}
