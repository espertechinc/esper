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
package com.espertech.esper.regression.dataflow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.graph.SupportGenericOutputOpWPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecDataflowTypes implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionBeanType(epService);
        runAssertionMapType(epService);
    }

    private void runAssertionBeanType(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addImport(SupportBean.class);
        epService.getEPAdministrator().createEPL("create schema SupportBean SupportBean");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SupportBean> {}" +
                "MySupportBeanOutputOp(outstream) {}" +
                "SupportGenericOutputOpWPort(outstream) {}");

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{new SupportBean("E1", 1)});
        MySupportBeanOutputOp outputOne = new MySupportBeanOutputOp();
        SupportGenericOutputOpWPort<SupportBean> outputTwo = new SupportGenericOutputOpWPort<SupportBean>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(source, outputOne, outputTwo));
        EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        dfOne.run();

        EPAssertionUtil.assertPropsPerRow(outputOne.getAndReset().toArray(), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}});
        Pair<List<SupportBean>, List<Integer>> received = outputTwo.getAndReset();
        EPAssertionUtil.assertPropsPerRow(received.getFirst().toArray(), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertEqualsExactOrder(new Integer[]{0}, received.getSecond().toArray());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMapType(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addImport(SupportBean.class);
        epService.getEPAdministrator().createEPL("create map schema MyMap (p0 String, p1 int)");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<MyMap> {}" +
                "MyMapOutputOp(outstream) {}" +
                "DefaultSupportCaptureOp(outstream) {}");

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{makeMap("E1", 1)});
        MyMapOutputOp outputOne = new MyMapOutputOp();
        DefaultSupportCaptureOp<SupportBean> outputTwo = new DefaultSupportCaptureOp<SupportBean>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(source, outputOne, outputTwo));
        EPDataFlowInstance dfOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        dfOne.run();

        EPAssertionUtil.assertPropsPerRow(outputOne.getAndReset().toArray(), "p0,p1".split(","), new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertPropsPerRow(outputTwo.getAndReset().get(0).toArray(), "p0,p1".split(","), new Object[][]{{"E1", 1}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private Map<String, Object> makeMap(String p0, int p1) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("p0", p0);
        map.put("p1", p1);
        return map;
    }

    public static class MySupportBeanOutputOp {
        private List<SupportBean> received = new ArrayList<SupportBean>();

        public synchronized void onInput(SupportBean event) {
            received.add(event);
        }

        public synchronized List<SupportBean> getAndReset() {
            List<SupportBean> result = received;
            received = new ArrayList<SupportBean>();
            return result;
        }
    }

    public static class MyMapOutputOp {
        private List<Map<String, Object>> received = new ArrayList<Map<String, Object>>();

        public synchronized void onInput(Map<String, Object> event) {
            received.add(event);
        }

        public synchronized List<Map<String, Object>> getAndReset() {
            List<Map<String, Object>> result = received;
            received = new ArrayList<Map<String, Object>>();
            return result;
        }
    }
}
