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
import com.espertech.esper.client.hook.*;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationValidationContext;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.newInstance;

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

        public AggregationFunctionFactoryCodegenType getCodegenType() {
            return AggregationFunctionFactoryCodegenType.CODEGEN_MANAGED;
        }

        public void rowMemberCodegen(AggregationFunctionFactoryCodegenRowMemberContext context) {
            // no members
        }

        public void applyEnterCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
            InvalidAggTest.applyEnterCodegenManaged(context);
        }

        public void applyLeaveCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
            // no code
        }

        public void applyEnterCodegenUnmanaged(AggregationFunctionFactoryCodegenRowApplyContextUnmanaged context) {
        }

        public void applyLeaveCodegenUnmanaged(AggregationFunctionFactoryCodegenRowApplyContextUnmanaged context) {
        }

        public void clearCodegen(AggregationFunctionFactoryCodegenRowClearContext context) {
        }

        public void getValueCodegen(AggregationFunctionFactoryCodegenRowGetValueContext context) {
            context.getMethod().getBlock().methodReturn(constantNull());
        }
    }

    public static class InvalidAggTest implements AggregationMethod {

        @Override
        public void enter(Object value) {
            throw new RuntimeException("Sample exception");
        }

        public static void applyEnterCodegenManaged(AggregationFunctionFactoryCodegenRowApplyContextManaged context) {
            context.getMethod().getBlock().methodThrow(newInstance(RuntimeException.class, constant("Sample exception")));
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
