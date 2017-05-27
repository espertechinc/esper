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
package com.espertech.esper.regression.event.revision;

import com.espertech.esper.client.ConfigurationRevisionEventType;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;

public class ExecEventRevisionMerge implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionMergeDeclared(epService);
        runAssertionMergeNonNull(epService);
        runAssertionMergeExists(epService);
        runAssertionNestedPropertiesNoDelta(epService);
    }

    private void runAssertionMergeDeclared(EPServiceProvider epService) {
        Map<String, Object> fullType = makeMap(new Object[][]{{"p1", String.class}, {"p2", String.class}, {"p3", String.class}, {"pf", String.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("FullTypeOne", fullType);

        Map<String, Object> deltaType = makeMap(new Object[][]{{"p1", String.class}, {"p2", String.class}, {"p3", String.class}, {"pd", String.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("DeltaTypeOne", deltaType);

        ConfigurationRevisionEventType revEvent = new ConfigurationRevisionEventType();
        revEvent.addNameBaseEventType("FullTypeOne");
        revEvent.addNameDeltaEventType("DeltaTypeOne");
        revEvent.setPropertyRevision(ConfigurationRevisionEventType.PropertyRevision.MERGE_DECLARED);
        revEvent.setKeyPropertyNames(new String[]{"p1"});
        epService.getEPAdministrator().getConfiguration().addRevisionEventType("MyExistsRevisionOne", revEvent);

        epService.getEPAdministrator().createEPL("create window MyWinOne#time(10 sec) as select * from MyExistsRevisionOne");
        epService.getEPAdministrator().createEPL("insert into MyWinOne select * from FullTypeOne");
        epService.getEPAdministrator().createEPL("insert into MyWinOne select * from DeltaTypeOne");

        String[] fields = "p1,p2,p3,pf,pd".split(",");
        EPStatement consumerOne = epService.getEPAdministrator().createEPL("select irstream * from MyWinOne");
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        consumerOne.addListener(listenerOne);
        EPAssertionUtil.assertEqualsAnyOrder(consumerOne.getEventType().getPropertyNames(), fields);

        epService.getEPRuntime().sendEvent(makeMap("p1,p2,p3,pf", "10,20,30,f0"), "FullTypeOne");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"10", "20", "30", "f0", null});

        epService.getEPRuntime().sendEvent(makeMap("p1,p2", "10,21"), "DeltaTypeOne");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "20", "30", "f0", null});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "21", null, "f0", null});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pf", "10,32,f1"), "FullTypeOne");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "21", null, "f0", null});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", null, "32", "f1", null});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pd", "10,33,pd3"), "DeltaTypeOne");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", null, "32", "f1", null});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", null, "33", "f1", "pd3"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p2,p3,pf", "10,22,34,f2"), "FullTypeOne");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", null, "33", "f1", "pd3"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "22", "34", "f2", "pd3"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1", "10"), "FullTypeOne");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "22", "34", "f2", "pd3"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", null, null, null, "pd3"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p2,p3,pf,pd", "10,23,35,pfx,pd4"), "DeltaTypeOne");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", null, null, null, "pd3"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "23", "35", null, "pd4"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p2", "10,null"), "DeltaTypeOne");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "23", "35", null, "pd4"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", null, null, null, null});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pd,pf", "10,36,pdx,f4"), "FullTypeOne");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", null, null, null, null});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", null, "36", "f4", null});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pd", "10,null,pd5"), "DeltaTypeOne");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", null, "36", "f4", null});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", null, null, "f4", "pd5"});
        listenerOne.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMergeNonNull(EPServiceProvider epService) {
        Map<String, Object> fullType = makeMap(new Object[][]{{"p1", String.class}, {"p2", String.class}, {"p3", String.class}, {"pf", String.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("FullTypeTwo", fullType);

        Map<String, Object> deltaType = makeMap(new Object[][]{{"p1", String.class}, {"p2", String.class}, {"p3", String.class}, {"pd", String.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("DeltaTypeTwo", deltaType);

        ConfigurationRevisionEventType revEvent = new ConfigurationRevisionEventType();
        revEvent.addNameBaseEventType("FullTypeTwo");
        revEvent.addNameDeltaEventType("DeltaTypeTwo");
        revEvent.setPropertyRevision(ConfigurationRevisionEventType.PropertyRevision.MERGE_NON_NULL);
        revEvent.setKeyPropertyNames(new String[]{"p1"});
        epService.getEPAdministrator().getConfiguration().addRevisionEventType("MyExistsRevisionTwo", revEvent);

        epService.getEPAdministrator().createEPL("create window MyWinTwo#time(10 sec) as select * from MyExistsRevisionTwo");
        epService.getEPAdministrator().createEPL("insert into MyWinTwo select * from FullTypeTwo");
        epService.getEPAdministrator().createEPL("insert into MyWinTwo select * from DeltaTypeTwo");

        String[] fields = "p1,p2,p3,pf,pd".split(",");
        EPStatement consumerOne = epService.getEPAdministrator().createEPL("select irstream * from MyWinTwo");
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        consumerOne.addListener(listenerOne);
        EPAssertionUtil.assertEqualsAnyOrder(consumerOne.getEventType().getPropertyNames(), fields);

        epService.getEPRuntime().sendEvent(makeMap("p1,p2,p3,pf", "10,20,30,f0"), "FullTypeTwo");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"10", "20", "30", "f0", null});

        epService.getEPRuntime().sendEvent(makeMap("p1,p2", "10,21"), "DeltaTypeTwo");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "20", "30", "f0", null});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "21", "30", "f0", null});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pf", "10,32,f1"), "FullTypeTwo");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "21", "30", "f0", null});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "21", "32", "f1", null});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pd", "10,33,pd3"), "DeltaTypeTwo");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "21", "32", "f1", null});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "21", "33", "f1", "pd3"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p2,p3,pf", "10,22,34,f2"), "FullTypeTwo");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "21", "33", "f1", "pd3"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "22", "34", "f2", "pd3"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1", "10"), "FullTypeTwo");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "22", "34", "f2", "pd3"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "22", "34", "f2", "pd3"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p2,p3,pf,pd", "10,23,35,pfx,pd4"), "DeltaTypeTwo");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "22", "34", "f2", "pd3"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "23", "35", "f2", "pd4"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p2", "10,null"), "DeltaTypeTwo");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "23", "35", "f2", "pd4"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "23", "35", "f2", "pd4"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pd,pf", "10,36,pdx,f4"), "FullTypeTwo");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "23", "35", "f2", "pd4"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "23", "36", "f4", "pd4"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pd", "10,null,pd5"), "DeltaTypeTwo");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "23", "36", "f4", "pd4"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "23", "36", "f4", "pd5"});
        listenerOne.reset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMergeExists(EPServiceProvider epService) {
        Map<String, Object> fullType = makeMap(new Object[][]{{"p1", String.class}, {"p2", String.class}, {"p3", String.class}, {"pf", String.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("FullTypeThree", fullType);

        Map<String, Object> deltaType = makeMap(new Object[][]{{"p1", String.class}, {"p2", String.class}, {"p3", String.class}, {"pd", String.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("DeltaTypeThree", deltaType);

        ConfigurationRevisionEventType revEvent = new ConfigurationRevisionEventType();
        revEvent.addNameBaseEventType("FullTypeThree");
        revEvent.addNameDeltaEventType("DeltaTypeThree");
        revEvent.setPropertyRevision(ConfigurationRevisionEventType.PropertyRevision.MERGE_EXISTS);
        revEvent.setKeyPropertyNames(new String[]{"p1"});
        epService.getEPAdministrator().getConfiguration().addRevisionEventType("MyExistsRevisionThree", revEvent);

        epService.getEPAdministrator().createEPL("create window MyWinThree#time(10 sec) as select * from MyExistsRevisionThree");
        epService.getEPAdministrator().createEPL("insert into MyWinThree select * from FullTypeThree");
        epService.getEPAdministrator().createEPL("insert into MyWinThree select * from DeltaTypeThree");

        String[] fields = "p1,p2,p3,pf,pd".split(",");
        EPStatement consumerOne = epService.getEPAdministrator().createEPL("select irstream * from MyWinThree");
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        consumerOne.addListener(listenerOne);
        EPAssertionUtil.assertEqualsAnyOrder(consumerOne.getEventType().getPropertyNames(), fields);

        epService.getEPRuntime().sendEvent(makeMap("p1,p2,p3,pf", "10,20,30,f0"), "FullTypeThree");
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"10", "20", "30", "f0", null});

        epService.getEPRuntime().sendEvent(makeMap("p1,p2", "10,21"), "DeltaTypeThree");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "20", "30", "f0", null});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "21", "30", "f0", null});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pf", "10,32,f1"), "FullTypeThree");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "21", "30", "f0", null});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "21", "32", "f1", null});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pd", "10,33,pd3"), "DeltaTypeThree");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "21", "32", "f1", null});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "21", "33", "f1", "pd3"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p2,p3,pf", "10,22,34,f2"), "FullTypeThree");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "21", "33", "f1", "pd3"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "22", "34", "f2", "pd3"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1", "10"), "FullTypeThree");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "22", "34", "f2", "pd3"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "22", "34", "f2", "pd3"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p2,p3,pf,pd", "10,23,35,pfx,pd4"), "DeltaTypeThree");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "22", "34", "f2", "pd3"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", "23", "35", "f2", "pd4"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p2", "10,null"), "DeltaTypeThree");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", "23", "35", "f2", "pd4"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", null, "35", "f2", "pd4"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pd,pf", "10,36,pdx,f4"), "FullTypeThree");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", null, "35", "f2", "pd4"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", null, "36", "f4", "pd4"});
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(makeMap("p1,p3,pd", "10,null,pd5"), "DeltaTypeThree");
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"10", null, "36", "f4", "pd4"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"10", null, null, "f4", "pd5"});
        listenerOne.reset();
    }

    private void runAssertionNestedPropertiesNoDelta(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("Nested", SupportBeanComplexProps.class);

        ConfigurationRevisionEventType revEvent = new ConfigurationRevisionEventType();
        revEvent.addNameBaseEventType("Nested");
        revEvent.setPropertyRevision(ConfigurationRevisionEventType.PropertyRevision.MERGE_DECLARED);
        revEvent.setKeyPropertyNames(new String[]{"simpleProperty"});
        epService.getEPAdministrator().getConfiguration().addRevisionEventType("NestedRevision", revEvent);

        epService.getEPAdministrator().createEPL("create window MyWinFour#time(10 sec) as select * from NestedRevision");
        epService.getEPAdministrator().createEPL("insert into MyWinFour select * from Nested");

        String[] fields = "key,f1".split(",");
        String stmtText = "select irstream simpleProperty as key, nested.nestedValue as f1 from MyWinFour";
        EPStatement consumerOne = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        consumerOne.addListener(listenerOne);
        EPAssertionUtil.assertEqualsAnyOrder(consumerOne.getEventType().getPropertyNames(), fields);

        epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), fields, new Object[]{"simple", "nestedValue"});

        SupportBeanComplexProps bean = SupportBeanComplexProps.makeDefaultBean();
        bean.getNested().setNestedValue("val2");
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listenerOne.getLastOldData()[0], fields, new Object[]{"simple", "nestedValue"});
        EPAssertionUtil.assertProps(listenerOne.getLastNewData()[0], fields, new Object[]{"simple", "val2"});
        listenerOne.reset();
    }

    private Map<String, Object> makeMap(Object[][] entries) {
        Map result = new HashMap<String, Object>();
        for (int i = 0; i < entries.length; i++) {
            result.put(entries[i][0], entries[i][1]);
        }
        return result;
    }

    private Map<String, Object> makeMap(String keysList, String valuesList) {
        String[] keys = keysList.split(",");
        String[] values = valuesList.split(",");

        Map result = new HashMap<String, Object>();
        for (int i = 0; i < keys.length; i++) {
            if (values[i].equals("null")) {
                result.put(keys[i], null);
            } else {
                result.put(keys[i], values[i]);
            }
        }
        return result;
    }
}
