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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.util.SafeIterator;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.CompilerOptions;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompiler;
import com.espertech.esper.runtime.client.*;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.client.scopetest.SupportSubscriber;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Regression testing environment.
 * <p>
 *     All regression-related compile, deploy and assertion should be performed using this environment.
 * </p>
 * <p>
 *     Implementations can decide to execute local or remote. Remote is useful when tests need a JVM with a different classpath or platform.
 * </p>
 * <p>
 *     Use this environment's "assert" methods in replacement of Junit assert, allowing the environment to control assertion.
 * </p>
 */
public interface RegressionEnvironment {
    Configuration getConfiguration();

    EPCompiler getCompiler();

    EPCompiled compile(String epl, CompilerArguments arguments);

    EPCompiled compile(String epl);

    EPCompiled compile(String epl, RegressionPath path);

    EPCompiled compile(EPStatementObjectModel model, CompilerArguments args);

    EPCompiled compile(boolean soda, String epl, CompilerArguments arguments);

    EPCompiled compile(String epl, Consumer<CompilerOptions> options);

    EPCompiled compileWCheckedEx(String epl) throws EPCompileException;

    EPCompiled compileWCheckedEx(String epl, RegressionPath path) throws EPCompileException;

    EPCompiled compileWRuntimePath(String epl);

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

    RegressionEnvironment deploy(EPCompiled compiled);

    RegressionEnvironment deploy(EPCompiled compiled, DeploymentOptions options);

    RegressionEnvironment rollout(List<EPDeploymentRolloutCompiled> items, RolloutOptions options);

    String deployGetId(EPCompiled compiled);

    RegressionEnvironment undeployAll();

    RegressionEnvironment undeployModuleContaining(String statementName);

    RegressionEnvironment undeploy(String deploymentId);

    EPFireAndForgetQueryResult compileExecuteFAF(String query, RegressionPath path);
    EPFireAndForgetQueryResult compileExecuteFAF(String query);
    void compileExecuteFAFNoResult(String query, RegressionPath path);

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

    RegressionEnvironment advanceTimeSpan(long msec, long resolution);

    RegressionEnvironment addListener(String statementName);

    RegressionEnvironment addListener(String statementName, SupportListener listener);

    RegressionEnvironment setSubscriber(String statementName);

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

    /**
     * Assert iterator results with order as provided.
     * Fails if the statement cannot be found.
     * @param statementName statement name
     * @param fields property names
     * @param expecteds expected values
     */
    void assertPropsPerRowIterator(String statementName, String[] fields, Object[][] expecteds);

    /**
     * Assert iterator results without order by finding matches.
     * Fails if the statement cannot be found.
     * @param statementName statement name
     * @param fields property names
     * @param expecteds expected values
     */
    void assertPropsPerRowIteratorAnyOrder(String statementName, String[] fields, Object[][] expecteds);

    /**
     * Assert all listener last-invocation new-events, with order as provided, with old-events and other invocations ignored
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fields property names
     * @param expecteds expected values
     */
    void assertPropsPerRowLastNew(String statementName, String[] fields, Object[][] expecteds);

    /**
     * Assert all listener last-invocation new-events, with any order (finds matches), with old-events and other invocations ignored
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fields property names
     * @param expecteds expected values
     */
    void assertPropsPerRowLastNewAnyOrder(String statementName, String[] fields, Object[][] expecteds);

    /**
     * Assert all listener last-invocation old-events, with new-events and other invocations ignored
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fields property names
     * @param expecteds expected values
     */
    void assertPropsPerRowLastOld(String statementName, String[] fields, Object[][] expecteds);

    /**
     * Assert all listener last-invocation new-events and old-events must be none, and other invocations ignored
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fields property names
     * @param expecteds expected values
     */
    void assertPropsPerRowNewOnly(String statementName, String[] fields, Object[][] expecteds);

    /**
     * Assert all listener new-events of all invocations and old-events ignored
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fields property names
     * @param expecteds expected values
     */
    void assertPropsPerRowNewFlattened(String statementName, String[] fields, Object[][] expecteds);

    /**
     * Assert all listener old-events of all invocations and new-events ignored
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fields property names
     * @param expecteds expected values
     */
    void assertPropsPerRowOldFlattened(String statementName, String[] fields, Object[][] expecteds);

    /**
     * Assert all listener new-events and old-events expecting a single invocation that provided these events
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fields property names
     * @param newExpected new-events expected values
     * @param oldExpected old-events expected values
     */
    void assertPropsPerRowIRPair(String statementName, String[] fields, Object[][] newExpected, Object[][] oldExpected);

    /**
     * Assert all listener-accumulated new-events and old-events (any number of invocations).
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fields property names
     * @param newExpected new-events expected values
     * @param oldExpected old-events expected values
     */
    void assertPropsPerRowIRPairFlattened(String statementName, String[] fields, Object[][] newExpected, Object[][] oldExpected);

    /**
     * Assert listener that one new event and no old event is received and in a single invocation to the listener
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fields property names
     * @param expecteds expected values
     */
    void assertPropsNew(String statementName, String[] fields, Object[] expecteds);

    /**
     * Assert listener that one old event and no new event is received and in a single invocation to the listener
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fields property names
     * @param expecteds expected values
     */
    void assertPropsOld(String statementName, String[] fields, Object[] expecteds);

    /**
     * Assert listener that one new event and one old event is received and in a single invocation to the listener
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fields property names
     * @param newExpected new-events expected values
     * @param oldExpected old-events expected values
     */
    void assertPropsIRPair(String statementName, String[] fields, Object[] newExpected, Object[] oldExpected);

    /**
     * Assert listener that one new event and one old event is received and in a single invocation to the listener
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param nameAndValuePairsNew names and values to assert against in new events
     * @param nameAndValuePairsOld names and values to assert against in old events
     */
    void assertPropsNV(String statementName, Object[][] nameAndValuePairsNew, Object[][] nameAndValuePairsOld);

    /**
     * Assert listener was invoked (not asserting the output received)
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     */
    void assertListenerInvoked(String statementName);

    /**
     * Assert listener was not invoked
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     */
    void assertListenerNotInvoked(String statementName);

    /**
     * Assert listener was or was not invoked
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param expected flag indicating invoked or not
     */
    void assertListenerInvokedFlag(String statementName, boolean expected);

    /**
     * Assert listener was or was not invoked
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param expected flag indicating invoked or not
     * @param message message
     */
    void assertListenerInvokedFlag(String statementName, boolean expected, String message);

    /**
     * Assert against a statement; Finds the statement by name and passes it to assertor.
     * Fails if the statement cannot be found.
     * @param statementName statement name
     * @param assertor receives the statement
     */
    void assertStatement(String statementName, Consumer<EPStatement> assertor);

    /**
     * Assert against a statement's listener; Finds the statement by name and passes its listener to assertor.
     * Fails if the statement cannot be found. Fails when there is no listener or there are multiple listeners for the statement.
     * Fails if the single listener is not a {@link SupportListener}.
     * @param statementName statement name
     * @param assertor receives the listener
     */
    void assertListener(String statementName, Consumer<SupportListener> assertor);

    /**
     * Assert against a statement's subscriber; Finds the statement by name and passes its subscriber to assertor.
     * Fails if the statement cannot be found. Fails when there is no subscriber.
     * Fails if the subscriber is not a {@link SupportSubscriber}.
     * @param statementName statement name
     * @param assertor receives the listener
     */
    void assertSubscriber(String statementName, Consumer<SupportSubscriber> assertor);

    /**
     * Assert listener that one new event and no old event is received and in a single invocation to the listener,
     * and passes the event to the assertor.
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param assertor receives the new event
     */
    void assertEventNew(String statementName, Consumer<EventBean> assertor);

    /**
     * Assert listener that one old event and no new event is received and in a single invocation to the listener,
     * and passes the event to the assertor.
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param assertor receives the new event
     */
    void assertEventOld(String statementName, Consumer<EventBean> assertor);

    /**
     * Assert against a statement's iterator; Finds the statement by name and passes its iterator to assertor.
     * Fails if the statement cannot be found.
     * @param statementName statement name
     * @param assertor receives the listener
     */
    void assertIterator(String statementName, Consumer<Iterator<EventBean>> assertor);

    /**
     * Assert against a statement's safe-iterator; Finds the statement by name and passes its safe-iterator to assertor.
     * Fails if the statement cannot be found.
     * @param statementName statement name
     * @param assertor receives the listener
     */
    void assertSafeIterator(String statementName, Consumer<SafeIterator<EventBean>> assertor);

    /**
     * Assert listener that one new event and no old event is received and compares field value.
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fieldName property name
     * @param expected expected value
     */
    void assertEqualsNew(String statementName, String fieldName, Object expected);

    /**
     * Assert listener that one old event and no new event is received and compares field value.
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     * @param fieldName property name
     * @param expected expected value
     */
    void assertEqualsOld(String statementName, String fieldName, Object expected);

    /**
     * Assert against the current timetime, passing the runtime to the assertor.
     * Implementations may simply ignore this when not asserting.
     * @param assertor assertion logic
     */
    void assertRuntime(Consumer<EPRuntime> assertor);

    /**
     * Assert when-available.
     * Implementations may simply ignore this when not asserting.
     * @param runnable assertion logic
     */
    void assertThat(Runnable runnable);

    /**
     * Reset listener.
     * Fails if the statement cannot be found or the statement does not have a single {@link SupportListener} listener.
     * @param statementName statement name
     */
    void listenerReset(String statementName);

    /**
     * Assert statement property types
     * Fails if the statement cannot be found.
     * @param statementName statement name
     * @param fields property names
     * @param classes property types
     */
    default void assertStmtTypes(String statementName, String[] fields, EPTypeClass[] classes) {
        assertStatement(statementName, statement -> SupportEventPropUtil.assertTypes(statement.getEventType(), fields, classes));
    }

    /**
     * Assert statement property type
     * Fails if the statement cannot be found.
     * @param statementName statement name
     * @param field property name
     * @param clazz property type
     */
    default void assertStmtType(String statementName, String field, EPTypeClass clazz) {
        assertStatement(statementName, statement -> SupportEventPropUtil.assertTypes(statement.getEventType(), field, clazz));
    }

    /**
     * Assert statement property types are all the same type
     * Fails if the statement cannot be found.
     * @param statementName statement name
     * @param fields property names
     * @param clazz property type
     */
    default void assertStmtTypesAllSame(String statementName, String[] fields, EPTypeClass clazz) {
        assertStatement(statementName, statement -> SupportEventPropUtil.assertTypesAllSame(statement.getEventType(), fields, clazz));
    }

    void runtimeSetVariable(String statementNameOfDeployment, String variableName, Object value);
    Schema runtimeAvroSchemaPreconfigured(String eventTypeName);
    Schema runtimeAvroSchemaByDeployment(String statementNameToFind, String eventTypeName);

    void tryInvalidCompile(String epl, String message);
    void tryInvalidCompile(RegressionPath path, String epl, String message);
    void tryInvalidCompileFAF(RegressionPath path, String epl, String message);
}
