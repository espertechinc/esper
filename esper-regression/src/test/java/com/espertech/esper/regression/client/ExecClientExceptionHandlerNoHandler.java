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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationValidationContext;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecClientExceptionHandlerNoHandler implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getExceptionHandling().getHandlerFactories().clear();
        configuration.addPlugInAggregationFunctionFactory("myinvalidagg", InvalidAggTestFactory.class.getName());
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String epl = "@Name('ABCName') select myinvalidagg() from SupportBean";
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPRuntime().sendEvent(new SupportBean());
    }

    public static class InvalidAggTestFactory implements AggregationFunctionFactory {

        @Override
        public void validate(AggregationValidationContext validationContext) {
        }

        @Override
        public Class getValueType() {
            return null;
        }

        public void setFunctionName(String functionName) {

        }

        public AggregationMethod newAggregator() {
            return new InvalidAggTest();
        }
    }

    public static class InvalidAggTest implements AggregationMethod {

        @Override
        public void enter(Object value) {
            throw new RuntimeException("Sample exception");
        }

        @Override
        public void leave(Object value) {
        }

        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public void clear() {
        }
    }

}
