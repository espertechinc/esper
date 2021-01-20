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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ModuleItem;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.SafeIterator;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.CompilerOptions;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompiler;
import com.espertech.esper.regressionlib.support.util.SupportAdminUtil;
import com.espertech.esper.runtime.client.*;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.client.scopetest.SupportSubscriber;
import com.espertech.esper.runtime.client.stage.EPStage;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.*;
import static org.junit.Assert.*;

public abstract class RegressionEnvironmentBase implements RegressionEnvironment {
    protected Configuration configuration;
    protected EPRuntime runtime;

    public abstract EPCompiler getCompiler();

    public Module parseModule(String moduleText) {
        try {
            return getCompiler().parseModule(moduleText);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public EPCompiled compileFAF(String query, RegressionPath path) {
        CompilerArguments args = getArgsNoExport(path);
        try {
            return getCompiler().compileQuery(query, args);
        } catch (Throwable t) {
            throw notExpected(t);
        }
    }

    public EPCompiled compileFAF(EPStatementObjectModel model, RegressionPath path) {
        CompilerArguments args = getArgsNoExport(path);
        try {
            return getCompiler().compileQuery(model, args);
        } catch (Throwable t) {
            throw notExpected(t);
        }
    }

    public EPFireAndForgetQueryResult compileExecuteFAF(String query, RegressionPath path) {
        EPCompiled compiled = compileFAF(query, path);
        return runtime().getFireAndForgetService().executeQuery(compiled);
    }

    public void compileExecuteFAFNoResult(String query, RegressionPath path) {
        compileExecuteFAF(query, path);
    }

    public RegressionEnvironment addListener(String statementName) {
        return null;
    }

    public EPFireAndForgetQueryResult compileExecuteFAF(EPStatementObjectModel model, RegressionPath path) {
        EPCompiled compiled = compileFAF(model, path);
        return runtime().getFireAndForgetService().executeQuery(compiled);
    }

    public EPFireAndForgetQueryResult executeQuery(EPCompiled compiled) {
        return runtime.getFireAndForgetService().executeQuery(compiled);
    }

    public RegressionEnvironmentBase(Configuration configuration, EPRuntime runtime) {
        this.configuration = configuration;
        this.runtime = runtime;
    }

    public RegressionEnvironment deploy(EPCompiled compiled) {
        tryDeploy(compiled);
        return this;
    }

    public RegressionEnvironment deploy(EPCompiled compiled, DeploymentOptions options) {
        try {
            runtime.getDeploymentService().deploy(compiled, options);
        } catch (EPDeployException ex) {
            throw notExpected(ex);
        }
        return this;
    }

    public String deployGetId(EPCompiled compiled) {
        try {
            return runtime().getDeploymentService().deploy(compiled).getDeploymentId();
        } catch (EPDeployException ex) {
            throw notExpected(ex);
        }
    }

    public RegressionEnvironment sendEventObjectArray(Object[] oa, String typeName) {
        runtime.getEventService().sendEventObjectArray(oa, typeName);
        return this;
    }

    public RegressionEnvironment sendEventBean(Object event) {
        runtime.getEventService().sendEventBean(event, event.getClass().getSimpleName());
        return this;
    }

    public RegressionEnvironment sendEventBeanStage(String stageUri, Object event) {
        if (stageUri == null) {
            return sendEventBean(event);
        }
        EPStage stage = runtime.getStageService().getExistingStage(stageUri);
        if (stage == null) {
            throw new RuntimeException("Failed to find stage '" + stageUri + "'");
        }
        stage.getEventService().sendEventBean(event, event.getClass().getSimpleName());
        return this;
    }

    public RegressionEnvironment sendEventBean(Object event, String typeName) {
        runtime.getEventService().sendEventBean(event, typeName);
        return this;
    }

    public RegressionEnvironment sendEventMap(Map<String, Object> values, String typeName) {
        runtime.getEventService().sendEventMap(values, typeName);
        return this;
    }

    public RegressionEnvironment sendEventXMLDOM(Node document, String typeName) {
        runtime.getEventService().sendEventXMLDOM(document, typeName);
        return this;
    }

    public RegressionEnvironment sendEventAvro(GenericData.Record theEvent, String typeName) {
        runtime.getEventService().sendEventAvro(theEvent, typeName);
        return this;
    }

    public RegressionEnvironment sendEventJson(String json, String typeName) {
        runtime.getEventService().sendEventJson(json, typeName);
        return this;
    }

    public RegressionEnvironment milestone(int num) {
        return null;
    }

    public RegressionEnvironment milestoneInc(AtomicInteger counter) {
        return null;
    }

    public boolean isHA() {
        return false;
    }

    public boolean isHA_Releasing() {
        return false;
    }

    public SupportListener listenerNew() {
        return null;
    }

    public RegressionEnvironment advanceTimeSpan(long msec) {
        runtime.getEventService().advanceTimeSpan(msec);
        return this;
    }

    public RegressionEnvironment advanceTimeSpan(long msec, long resolution) {
        runtime.getEventService().advanceTimeSpan(msec, resolution);
        return this;
    }

    public RegressionEnvironment advanceTime(long msec) {
        runtime.getEventService().advanceTime(msec);
        return this;
    }

    public RegressionEnvironment advanceTimeStage(String stageUri, long msec) {
        if (stageUri == null) {
            advanceTime(msec);
            return this;
        }
        EPStage stage = runtime.getStageService().getExistingStage(stageUri);
        if (stage == null) {
            throw new RuntimeException("Failed to find stage '" + stageUri + "'");
        }
        stage.getEventService().advanceTime(msec);
        return this;
    }

    public SupportListener listener(String statementName) {
        return getRequireStatementListener(statementName, runtime);
    }

    public SupportSubscriber subscriber(String statementName) {
        return getRequireStatementSubscriber(statementName, runtime);
    }

    public SupportListener listenerStage(String stageUri, String statementName) {
        return getRequireStatementListener(statementName, stageUri, runtime);
    }

    public String deploymentId(String statementName) {
        EPStatementSPI statement = (EPStatementSPI) getRequireStatement(statementName, runtime);
        return statement.getStatementContext().getDeploymentId();
    }

    public RegressionEnvironment undeployAll() {
        try {
            runtime.getDeploymentService().undeployAll();

            String[] stageURIs = runtime.getStageService().getStageURIs();
            for (String uri : stageURIs) {
                EPStage stage = runtime.getStageService().getExistingStage(uri);
                stage.destroy();
            }
        } catch (EPUndeployException ex) {
            throw notExpected(ex);
        }
        return this;
    }

    public RegressionEnvironment undeploy(String deploymentId) {
        try {
            runtime.getDeploymentService().undeploy(deploymentId);
        } catch (EPUndeployException ex) {
            throw notExpected(ex);
        }
        return this;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public EPStatement statement(String statementName) {
        return SupportAdminUtil.getStatement(statementName, runtime);
    }

    public Iterator<EventBean> iterator(String statementName) {
        return SupportAdminUtil.getRequireStatement(statementName, runtime).iterator();
    }

    public RegressionEnvironment compileDeployAddListenerMileZero(String epl, String statementName) {
        return compileDeployAddListenerMile(epl, statementName, 0);
    }

    public RegressionEnvironment setSubscriber(String statementName) {
        getRequireStatement(statementName, runtime).setSubscriber(new SupportSubscriber());
        return this;
    }

    public RegressionEnvironment compileDeploy(boolean soda, String epl, RegressionPath path) {
        if (!soda) {
            compileDeploy(epl, path);
        } else {
            EPStatementObjectModel model = eplToModel(epl);
            assertEquals(epl, model.toEPL());
            compileDeploy(model, path);
        }
        return this;
    }

    public EPCompiled compile(String epl, RegressionPath path) {
        CompilerArguments args = getArgsWithExportToPath(path);
        EPCompiled compiled = compile(epl, args);
        path.add(compiled);
        return compiled;
    }

    public RegressionEnvironment compileDeploy(EPStatementObjectModel model, RegressionPath path) {
        CompilerArguments args = getArgsWithExportToPath(path);
        EPCompiled compiled = compile(model, args);
        path.add(compiled);

        deploy(compiled);
        return this;
    }

    public RegressionEnvironment compileDeploy(EPStatementObjectModel model) {
        CompilerArguments args = new CompilerArguments(getConfiguration());
        EPCompiled compiled = compile(model, args);
        deploy(compiled);
        return this;
    }

    public RegressionEnvironment compileDeployAddListenerMile(String epl, String statementName, int milestone) {
        EPCompiled compiled = tryCompile(epl, null);
        deploy(compiled).addListener(statementName);
        if (milestone != -1) {
            milestone(milestone);
        }
        return this;
    }

    public EPCompiled compile(boolean soda, String epl, CompilerArguments arguments) {
        if (!soda) {
            compile(epl, arguments);
        }
        EPStatementObjectModel copy = eplToModel(epl);
        assertEquals(epl, copy.toEPL());
        arguments.setConfiguration(configuration);
        return compile(copy, arguments);
    }

    public RegressionEnvironment compileDeploy(boolean soda, String epl) {
        if (!soda) {
            compileDeploy(epl);
        } else {
            eplToModelCompileDeploy(epl);
        }
        return this;
    }

    public RegressionEnvironment compileDeploy(String epl) {
        EPCompiled compiled = tryCompile(epl, null);
        deploy(compiled);
        return this;
    }

    public RegressionEnvironment compileDeploy(String epl, Consumer<CompilerOptions> options) {
        EPCompiled compiled = tryCompile(epl, options);
        deploy(compiled);
        return this;
    }

    public RegressionEnvironment compileDeploy(String epl, RegressionPath path) {
        CompilerArguments args = getArgsWithExportToPath(path);
        EPCompiled compiled = compile(epl, args);
        path.add(compiled);

        deploy(compiled);
        return this;
    }

    public RegressionEnvironment eplToModelCompileDeploy(String epl) {
        EPStatementObjectModel copy = eplToModel(epl);

        assertEquals(epl.trim(), copy.toEPL());

        EPCompiled compiled = compile(copy, new CompilerArguments(getConfiguration()));
        EPDeployment result = tryDeploy(compiled);

        EPStatement stmt = result.getStatements()[0];
        assertEquals(epl.trim(), stmt.getProperty(StatementProperty.EPL));
        return this;
    }

    public RegressionEnvironment eplToModelCompileDeploy(String epl, RegressionPath path) {
        EPStatementObjectModel copy = eplToModel(epl);

        assertEquals(epl.trim(), copy.toEPL());

        CompilerArguments args = getArgsWithExportToPath(path);

        EPCompiled compiled = compile(copy, args);
        path.add(compiled);

        EPDeployment result = tryDeploy(compiled);

        assertEquals(epl.trim(), result.getStatements()[0].getProperty(StatementProperty.EPL));
        return this;
    }

    public EPCompiled compile(EPStatementObjectModel model, CompilerArguments args) {
        try {
            Module module = new Module();
            module.getItems().add(new ModuleItem(model));
            module.setModuleText(model.toEPL());
            return getCompiler().compile(module, args);
        } catch (Throwable t) {
            throw notExpected(t);
        }
    }

    public RegressionEnvironment undeployModuleContaining(String statementName) {
        String[] deployments = runtime.getDeploymentService().getDeployments();
        try {
            for (String deployment : deployments) {
                EPDeployment info = runtime.getDeploymentService().getDeployment(deployment);
                for (EPStatement stmt : info.getStatements()) {
                    if (stmt.getName().equals(statementName)) {
                        runtime.getDeploymentService().undeploy(deployment);
                        return this;
                    }
                }
            }
        } catch (EPUndeployException ex) {
            throw notExpected(ex);
        }
        fail("Failed to find deployment with statement '" + statementName + "'");
        return this;
    }

    public RegressionEnvironment addListener(String statementName, SupportListener listener) {
        getAssertStatement(statementName).addListener(listener);
        return this;
    }

    protected EPStatement getAssertStatement(String statementName) {
        return SupportAdminUtil.getRequireStatement(statementName, runtime);
    }

    public EPCompiled compile(String epl, CompilerArguments arguments) {
        try {
            arguments.setConfiguration(configuration);
            return getCompiler().compile(epl, arguments);
        } catch (EPCompileException t) {
            throw notExpected(t);
        }
    }

    public EPCompiled compile(String epl, Consumer<CompilerOptions> options) {
        return tryCompile(epl, options);
    }

    public EPCompiled compileWBusPublicType(String epl) {
        return tryCompile(epl, compilerOptions -> compilerOptions
            .setBusModifierEventType(ctx -> EventTypeBusModifier.BUS)
            .setAccessModifierEventType(ctx -> NameAccessModifier.PUBLIC)
            .setAccessModifierNamedWindow(ctx -> NameAccessModifier.PUBLIC)
            .setAccessModifierTable(ctx -> NameAccessModifier.PUBLIC));
    }

    public EPCompiled compile(String epl) {
        return tryCompile(epl, null);
    }

    public EPStatementObjectModel eplToModel(String epl) {
        try {
            EPStatementObjectModel model = getCompiler().eplToModel(epl, getConfiguration());
            return SerializableObjectCopier.copyMayFail(model); // copy to test serializability
        } catch (EPCompileException t) {
            throw notExpected(t);
        }
    }

    public String runtimeURI() {
        return runtime.getURI();
    }

    public EPRuntime runtime() {
        return runtime;
    }

    public EPDeploymentService deployment() {
        return runtime.getDeploymentService();
    }

    public EPEventService eventService() {
        return runtime.getEventService();
    }

    public EPStageService stageService() {
        return runtime.getStageService();
    }

    public EPCompiled compileWCheckedEx(String epl) throws EPCompileException {
        return compileWCheckedEx(epl, null);
    }

    public EPCompiled compileWCheckedEx(String epl, RegressionPath path) throws EPCompileException {
        CompilerArguments args = new CompilerArguments(getConfiguration());
        if (path != null) {
            args.getPath().addAll(path.getCompileds());
        }
        return getCompiler().compile(epl, args);
    }

    public EPCompiled compileWRuntimePath(String epl) {
        return compile(epl, new CompilerArguments(runtime().getRuntimePath()));
    }

    public Module readModule(String filename) {
        try {
            return getCompiler().readModule(filename, this.getClass().getClassLoader());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public EPCompiled readCompile(String fileName) {
        Module module = readModule(fileName);
        return compile(module);
    }

    public EPCompiled compile(Module module) {
        try {
            return getCompiler().compile(module, new CompilerArguments(configuration));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public RegressionEnvironment rollout(List<EPDeploymentRolloutCompiled> items, RolloutOptions options) {
        try {
            runtime.getDeploymentService().rollout(items, options);
            return this;
        } catch (EPDeployException ex) {
            throw notExpected(ex);
        }
    }

    public void assertPropsPerRowIterator(String statementName, String[] fields, Object[][] expecteds) {
        EPAssertionUtil.assertPropsPerRow(iterator(statementName), fields, expecteds);
    }

    public void assertPropsPerRowIteratorAnyOrder(String statementName, String[] fields, Object[][] expecteds) {
        EPAssertionUtil.assertPropsPerRowAnyOrder(iterator(statementName), fields, expecteds);
    }

    public void assertPropsNew(String statementName, String[] fields, Object[] expecteds) {
        EPAssertionUtil.assertProps(listener(statementName).assertOneGetNewAndReset(), fields, expecteds);
    }

    public void assertPropsOld(String statementName, String[] fields, Object[] expecteds) {
        EPAssertionUtil.assertProps(listener(statementName).assertOneGetOldAndReset(), fields, expecteds);
    }

    public void assertPropsPerRowLastNew(String statementName, String[] fields, Object[][] expecteds) {
        EPAssertionUtil.assertPropsPerRow(listener(statementName).getAndResetLastNewData(), fields, expecteds);
    }

    public void assertPropsPerRowNewOnly(String statementName, String[] fields, Object[][] expecteds) {
        SupportListener listener = listener(statementName);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, expecteds);
        assertNull(listener.getLastOldData());
        listener.reset();
    }

    public void assertPropsPerRowLastNewAnyOrder(String statementName, String[] fields, Object[][] expecteds) {
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener(statementName).getAndResetLastNewData(), fields, expecteds);
    }

    public void assertPropsPerRowLastOld(String statementName, String[] fields, Object[][] expecteds) {
        EPAssertionUtil.assertPropsPerRow(listener(statementName).getAndResetLastOldData(), fields, expecteds);
    }

    public void assertPropsIRPair(String statementName, String[] fields, Object[] newExpected, Object[] oldExpected) {
        EPAssertionUtil.assertProps(listener(statementName).assertPairGetIRAndReset(), fields, newExpected, oldExpected);
    }

    public void assertListenerInvoked(String statementName) {
        assertTrue(listener(statementName).getIsInvokedAndReset());
    }

    public void assertListenerNotInvoked(String statementName) {
        assertFalse(listener(statementName).isInvoked());
    }

    public void assertListenerInvokedFlag(String statementName, boolean expected) {
        assertEquals(expected, listener(statementName).getIsInvokedAndReset());
    }

    public void assertListenerInvokedFlag(String statementName, boolean expected, String message) {
        assertEquals(message, expected, listener(statementName).getIsInvokedAndReset());
    }

    public void listenerReset(String statementName) {
        listener(statementName).reset();
    }

    public void assertPropsPerRowIRPair(String statementName, String[] fields, Object[][] newExpected, Object[][] oldExpected) {
        SupportListener listener = listener("s0");
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, newExpected);
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, oldExpected);
        listener.reset();
    }

    public void assertPropsPerRowIRPairFlattened(String statementName, String[] fields, Object[][] newExpected, Object[][] oldExpected) {
        SupportListener listener = listener("s0");
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), fields, newExpected);
        EPAssertionUtil.assertPropsPerRow(listener.getOldDataListFlattened(), fields, oldExpected);
        listener.reset();
    }

    public void assertPropsNV(String statementName, Object[][] nameAndValuePairsNew, Object[][] nameAndValuePairsOld) {
        SupportListener listener = listener(statementName);
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(1, listener.getOldDataList().size());
        EPAssertionUtil.assertNameValuePairs(listener.getLastNewData(), nameAndValuePairsNew);
        EPAssertionUtil.assertNameValuePairs(listener.getLastOldData(), nameAndValuePairsOld);
        listener.reset();
    }

    public void assertStatement(String statementName, Consumer<EPStatement> assertor) {
        assertor.accept(statement(statementName));
    }

    public void assertThat(Runnable runnable) {
        runnable.run();
    }

    public void assertRuntime(Consumer<EPRuntime> assertor) {
        assertor.accept(runtime);
    }

    public void assertListener(String statementName, Consumer<SupportListener> assertor) {
        assertor.accept(listener(statementName));
    }

    public void assertSubscriber(String statementName, Consumer<SupportSubscriber> assertor) {
        assertor.accept(subscriber(statementName));
    }

    public void assertIterator(String statementName, Consumer<Iterator<EventBean>> assertor) {
        assertor.accept(iterator(statementName));
    }

    public void assertSafeIterator(String statementName, Consumer<SafeIterator<EventBean>> assertor) {
        assertor.accept(statement(statementName).safeIterator());
    }

    public void assertEventNew(String statementName, Consumer<EventBean> assertor) {
        assertor.accept(listener(statementName).assertOneGetNewAndReset());
    }

    public void assertEventOld(String statementName, Consumer<EventBean> assertor) {
        assertor.accept(listener(statementName).assertOneGetOldAndReset());
    }

    public void assertPropsPerRowNewFlattened(String statementName, String[] fields, Object[][] expecteds) {
        SupportListener listener = listener(statementName);
        EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), fields, expecteds);
        listener.reset();
    }

    public void assertPropsPerRowOldFlattened(String statementName, String[] fields, Object[][] expecteds) {
        SupportListener listener = listener(statementName);
        EPAssertionUtil.assertPropsPerRow(listener.getOldDataListFlattened(), fields, expecteds);
        listener.reset();
    }

    public void assertEqualsNew(String statementName, String fieldName, Object expected) {
        assertEquals(expected, listener(statementName).assertOneGetNewAndReset().get(fieldName));
    }

    public void assertEqualsOld(String statementName, String fieldName, Object expected) {
        assertEquals(expected, listener(statementName).assertOneGetOldAndReset().get(fieldName));
    }

    public void tryInvalidCompile(String epl, String message) {
        try {
            compileWCheckedEx(epl);
            fail();
        } catch (EPCompileException ex) {
            SupportMessageAssertUtil.assertMessage(ex, message);
        }
    }

    public void tryInvalidCompile(RegressionPath path, String epl, String message) {
        try {
            compileWCheckedEx(epl, path);
            fail();
        } catch (EPCompileException ex) {
            SupportMessageAssertUtil.assertMessage(ex, message);
        }
    }

    public void runtimeSetVariable(String statementNameOfDeployment, String variableName, Object value) {
        String deploymentId = statementNameOfDeployment == null ? null : deploymentId(statementNameOfDeployment);
        runtime().getVariableService().setVariableValue(deploymentId, variableName, value);
    }

    public Schema runtimeAvroSchemaPreconfigured(String eventTypeName) {
        return SupportAvroUtil.getAvroSchema(runtime.getEventTypeService().getEventTypePreconfigured(eventTypeName));
    }

    public Schema runtimeAvroSchemaByDeployment(String statementNameToFind, String eventTypeName) {
        String deploymentId = deploymentId(statementNameToFind);
        EventType eventType = runtime.getEventTypeService().getEventType(deploymentId, eventTypeName);
        if (eventType == null) {
            throw new IllegalArgumentException("Failed to find event type '" + eventTypeName + "' at deployment id '" + deploymentId + "'");
        }
        return SupportAvroUtil.getAvroSchema(eventType);
    }

    private EPDeployment tryDeploy(EPCompiled compiled) {
        try {
            return runtime.getDeploymentService().deploy(compiled);
        } catch (EPDeployException ex) {
            throw notExpected(ex);
        }
    }

    private EPCompiled tryCompile(String epl, Consumer<CompilerOptions> options) {
        try {
            if (options == null) {
                return getCompiler().compile(epl, new CompilerArguments(this.getConfiguration()));
            }

            CompilerArguments args = new CompilerArguments(getConfiguration());
            options.accept(args.getOptions());

            return getCompiler().compile(epl, args);
        } catch (Throwable t) {
            throw notExpected(t);
        }
    }

    private CompilerArguments getArgsNoExport(RegressionPath path) {
        CompilerArguments args = new CompilerArguments(getConfiguration());
        args.getPath().getCompileds().addAll(path.getCompileds());
        return args;
    }

    private CompilerArguments getArgsWithExportToPath(RegressionPath path) {
        CompilerArguments args = new CompilerArguments(getConfiguration());
        args.getPath().getCompileds().addAll(path.getCompileds());
        return args;
    }

    private RuntimeException notExpected(Throwable t) {
        throw new RuntimeException("Test failed due to exception: " + t.getMessage(), t);
    }
}
