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

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteClient extends TestCase {
    public void testExecClientAdapterLoader() {
        RegressionRunner.run(new ExecClientAdapterLoader());
    }

    public void testExecClientAggregationFunctionPlugIn() {
        RegressionRunner.run(new ExecClientAggregationFunctionPlugIn());
    }

    public void testExecClientAggregationMultiFunctionPlugIn() {
        RegressionRunner.run(new ExecClientAggregationMultiFunctionPlugIn());
    }

    public void testExecClientAudit() {
        RegressionRunner.run(new ExecClientAudit());
    }

    public void testExecClientCodegen() {
        RegressionRunner.run(new ExecClientCodegen());
    }

    public void testExecClientMicrosecondResolution() {
        RegressionRunner.run(new ExecClientMicrosecondResolution());
    }

    public void testExecClientConfigurationOperations() {
        RegressionRunner.run(new ExecClientConfigurationOperations());
    }

    public void testExecClientDeployAdmin() {
        RegressionRunner.run(new ExecClientDeployAdmin());
    }

    public void testExecClientDeployOrder() {
        RegressionRunner.run(new ExecClientDeployOrder());
    }

    public void testExecClientDeployParse() {
        RegressionRunner.run(new ExecClientDeployParse());
    }

    public void testExecClientDeployRedefinition() {
        RegressionRunner.run(new ExecClientDeployRedefinition());
    }

    public void testExecClientEPAdministrator() {
        RegressionRunner.run(new ExecClientEPAdministrator());
    }

    public void testExecClientEPAdministratorPerformance() {
        RegressionRunner.run(new ExecClientEPAdministratorPerformance());
    }

    public void testExecClientEPStatement() {
        RegressionRunner.run(new ExecClientEPStatement());
    }

    public void testExecClientEPStatementObjectModel() {
        RegressionRunner.run(new ExecClientEPStatementObjectModel());
    }

    public void testExecClientEPStatementSubstitutionParams() {
        RegressionRunner.run(new ExecClientEPStatementSubstitutionParams());
    }

    public void testExecClientEPServiceProvider() {
        RegressionRunner.run(new ExecClientEPServiceProvider());
    }

    public void testExecClientEPServiceProviderMetricsJMX() {
        RegressionRunner.run(new ExecClientEPServiceProviderMetricsJMX());
    }

    public void testExecClientExceptionHandler() {
        RegressionRunner.run(new ExecClientExceptionHandler());
    }

    public void testExecClientExceptionHandlerNoHandler() {
        RegressionRunner.run(new ExecClientExceptionHandlerNoHandler());
    }

    public void testExecClientExceptionHandlerGetCtx() {
        RegressionRunner.run(new ExecClientExceptionHandlerGetCtx());
    }

    public void testExecClientInvalidSyntaxMsg() {
        RegressionRunner.run(new ExecClientInvalidSyntaxMsg());
    }

    public void testExecClientIsolationUnit() {
        RegressionRunner.run(new ExecClientIsolationUnit());
    }

    public void testExecClientIsolationUnitConfig() {
        RegressionRunner.run(new ExecClientIsolationUnitConfig());
    }

    public void testExecClientMetricsReportingNW() {
        RegressionRunner.run(new ExecClientMetricsReportingNW());
    }

    public void testExecClientMetricsReportingEngineMetrics() {
        RegressionRunner.run(new ExecClientMetricsReportingEngineMetrics());
    }

    public void testExecClientMetricsReportingStmtGroups() {
        RegressionRunner.run(new ExecClientMetricsReportingStmtGroups());
    }

    public void testExecClientMetricsReportingStmtMetrics() {
        RegressionRunner.run(new ExecClientMetricsReportingStmtMetrics());
    }

    public void testExecClientMetricsReportingDisableRuntime() {
        RegressionRunner.run(new ExecClientMetricsReportingDisableRuntime());
    }

    public void testExecClientMetricsReportingDisableStatement() {
        RegressionRunner.run(new ExecClientMetricsReportingDisableStatement());
    }

    public void testExecClientPatternGuardPlugIn() {
        RegressionRunner.run(new ExecClientPatternGuardPlugIn());
    }

    public void testExecClientPriorityAndDropInstructions() {
        RegressionRunner.run(new ExecClientPriorityAndDropInstructions());
    }

    public void testExecClientSingleRowFunctionPlugIn() {
        RegressionRunner.run(new ExecClientSingleRowFunctionPlugIn());
    }

    public void testExecClientSolutionPatternPortScan() {
        RegressionRunner.run(new ExecClientSolutionPatternPortScan());
    }

    public void testExecClientStatementAnnotation() {
        RegressionRunner.run(new ExecClientStatementAnnotation());
    }

    public void testExecClientStatementAnnotationImport() {
        RegressionRunner.run(new ExecClientStatementAnnotationImport());
    }

    public void testExecClientStatementAwareListener() {
        RegressionRunner.run(new ExecClientStatementAwareListener());
    }

    public void testExecClientSubscriberBind() {
        RegressionRunner.run(new ExecClientSubscriberBind());
    }

    public void testExecClientSubscriberInvalid() {
        RegressionRunner.run(new ExecClientSubscriberInvalid());
    }

    public void testExecClientSubscriberMgmt() {
        RegressionRunner.run(new ExecClientSubscriberMgmt());
    }

    public void testExecClientSubscriberPerf() {
        RegressionRunner.run(new ExecClientSubscriberPerf());
    }

    public void testExecClientThreadedConfigInbound() {
        RegressionRunner.run(new ExecClientThreadedConfigInbound());
    }

    public void testExecClientThreadedConfigInboundFastShutdown() {
        RegressionRunner.run(new ExecClientThreadedConfigInboundFastShutdown());
    }

    public void testExecClientThreadedConfigOutbound() {
        RegressionRunner.run(new ExecClientThreadedConfigOutbound());
    }

    public void testExecClientThreadedConfigRoute() {
        RegressionRunner.run(new ExecClientThreadedConfigRoute());
    }

    public void testExecClientThreadedConfigTimer() {
        RegressionRunner.run(new ExecClientThreadedConfigTimer());
    }

    public void testExecClientTimeControlEvent() {
        RegressionRunner.run(new ExecClientTimeControlEvent());
    }

    public void testExecClientConfigurationTransients() {
        RegressionRunner.run(new ExecClientConfigurationTransients());
    }

    public void testExecClientUnmatchedListener() {
        RegressionRunner.run(new ExecClientUnmatchedListener());
    }

    public void testExecClientViewPlugin() {
        RegressionRunner.run(new ExecClientViewPlugin());
    }

    public void testExecClientVirtualDataWindow() {
        RegressionRunner.run(new ExecClientVirtualDataWindow());
    }

    public void testExecClientVirtualDataWindowLateConsume() {
        RegressionRunner.run(new ExecClientVirtualDataWindowLateConsume());
    }

    public void testExecClientVirtualDataWindowToLookup() {
        RegressionRunner.run(new ExecClientVirtualDataWindowToLookup());
    }
}
