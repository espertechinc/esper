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
package com.espertech.esper.regressionlib.framework;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.CompilerOptions;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompiler;
import com.espertech.esper.runtime.client.*;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.apache.avro.generic.GenericData;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public interface RegressionEnvironment {
    Configuration getConfiguration();

    EPCompiler getCompiler();

    EPCompiled compile(String epl, CompilerArguments arguments);

    EPCompiled compile(String epl);

    EPCompiled compile(String epl, RegressionPath path);

    EPCompiled compile(EPStatementObjectModel model, CompilerArguments args);

    EPCompiled compile(boolean soda, String epl, CompilerArguments arguments);

    EPCompiled compile(String epl, Consumer<CompilerOptions> options);

    EPCompiled compileWBusPublicType(String epl);

    EPCompiled compileWCheckedEx(String epl) throws EPCompileException;

    EPCompiled compileWCheckedEx(String epl, RegressionPath path) throws EPCompileException;

    EPCompiled compileFAF(String query, RegressionPath path);

    EPCompiled compileFAF(EPStatementObjectModel model, RegressionPath path);

    EPCompiled compile(Module module);

    Module readModule(String filename);

    EPCompiled readCompile(String filename);

    Module parseModule(String moduleText);

    RegressionEnvironment compileDeployAddListenerMileZero(String epl, String statementName);

    RegressionEnvironment compileDeployAddListenerMile(String epl, String statementName, int milestone);

    RegressionEnvironment compileDeploy(String epl);

    RegressionEnvironment compileDeploy(boolean soda, String epl);

    RegressionEnvironment compileDeploy(String epl, Consumer<CompilerOptions> options);

    RegressionEnvironment compileDeploy(boolean soda, String epl, RegressionPath path);

    RegressionEnvironment compileDeploy(String epl, RegressionPath path);

    RegressionEnvironment compileDeploy(EPStatementObjectModel model);

    RegressionEnvironment compileDeploy(EPStatementObjectModel model, RegressionPath path);

    RegressionEnvironment compileDeployWBusPublicType(String epl, RegressionPath path);

    RegressionEnvironment deploy(EPCompiled compiled);

    RegressionEnvironment deploy(EPCompiled compiled, DeploymentOptions options);

    RegressionEnvironment rollout(List<EPDeploymentRolloutCompiled> items, RolloutOptions options);

    String deployGetId(EPCompiled compiled);

    RegressionEnvironment undeployAll();

    RegressionEnvironment undeployModuleContaining(String statementName);

    RegressionEnvironment undeploy(String deploymentId);

    EPFireAndForgetQueryResult compileExecuteFAF(String query, RegressionPath path);

    EPFireAndForgetQueryResult compileExecuteFAF(EPStatementObjectModel model, RegressionPath path);

    RegressionEnvironment sendEventObjectArray(Object[] oa, String typeName);

    RegressionEnvironment sendEventBean(Object event);

    RegressionEnvironment sendEventBean(Object event, String typeName);

    RegressionEnvironment sendEventBeanStage(String stageUri, Object event);

    RegressionEnvironment sendEventMap(Map<String, Object> values, String typeName);

    RegressionEnvironment sendEventXMLDOM(org.w3c.dom.Node document, String typeName);

    RegressionEnvironment sendEventAvro(GenericData.Record theEvent, String typeName);

    RegressionEnvironment sendEventJson(String json, String typeName);

    RegressionEnvironment advanceTime(long msec);

    RegressionEnvironment advanceTimeStage(String stageUri, long msec);

    RegressionEnvironment advanceTimeSpan(long msec);

    RegressionEnvironment addListener(String statementName);

    RegressionEnvironment addListener(String statementName, SupportListener listener);

    RegressionEnvironment milestone(int num);

    RegressionEnvironment milestoneInc(AtomicInteger counter);

    EPStatement statement(String statementName);

    Iterator<EventBean> iterator(String statementName);

    SupportListener listener(String statementName);

    SupportListener listenerStage(String stageUri, String statementName);

    String deploymentId(String statementName);

    RegressionEnvironment eplToModelCompileDeploy(String epl);

    RegressionEnvironment eplToModelCompileDeploy(String epl, RegressionPath path);

    EPStatementObjectModel eplToModel(String epl);

    boolean isHA();

    boolean isHA_Releasing();

    String runtimeURI();

    EPRuntime runtime();

    EPEventService eventService();

    EPDeploymentService deployment();

    SupportListener listenerNew();

    EPStageService stageService();

    void assertPropsPerRowIterator(String statementName, String[] fields, Object[][] expecteds);
    void assertPropsPerRowIteratorAnyOrder(String statementName, String[] fields, Object[][] expecteds);

    void assertPropsPerRowLastNew(String statementName, String[] fields, Object[][] expecteds);
    void assertPropsPerRowLastOld(String statementName, String[] fields, Object[][] expecteds);
    void assertPropsPerRowNewFlattened(String statementName, String[] fields, Object[][] expecteds);
    void assertPropsPerRowOldFlattened(String statementName, String[] fields, Object[][] expecteds);
    void assertPropsPerRowIRPair(String statementName, String[] fields, Object[][] newExpected, Object[][] oldExpected);
    void assertPropsPerRowIRPairFlattened(String statementName, String[] fields, Object[][] newExpected, Object[][] oldExpected);

    void assertPropsNew(String statementName, String[] fields, Object[] expecteds);
    void assertPropsOld(String statementName, String[] fields, Object[] expecteds);
    void assertPropsIRPair(String statementName, String[] fields, Object[] newExpected, Object[] oldExpected);
    void assertPropsNV(String statementName, Object[][] nameAndValuePairsIStream, Object[][] nameAndValuePairsRStream);

    void assertListenerInvoked(String statementName);
    void assertListenerNotInvoked(String statementName);
    void assertListenerInvokedFlag(String statementName, boolean expected);
    void assertStatement(String statementName, Consumer<EPStatement> assertor);
    void assertListener(String statementName, Consumer<SupportListener> assertor);
    void assertEventNew(String statementName, Consumer<EventBean> assertor);
    void assertEventOld(String statementName, Consumer<EventBean> assertor);
    void assertIterator(String statementName, Consumer<Iterator<EventBean>> assertor);
    void assertEqualsNew(String statementName, String fieldName, Object expected);
    void assertRuntime(Consumer<EPRuntime> assertor);
    void assertThis(Runnable runnable);
    void listenerReset(String statementName);

    void runtimeSetVariable(String statementNameOfDeployment, String variableName, Object value);

    void tryInvalidCompile(String epl, String message);
    void tryInvalidCompile(RegressionPath path, String epl, String message);
}
