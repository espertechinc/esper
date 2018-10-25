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
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import org.junit.Assert;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.Assert.fail;

public class SupportMessageAssertUtil {
    public static void tryInvalidFAFCompile(RegressionEnvironment env, RegressionPath path, String epl, String message) {
        try {
            compileFAFInternal(env, path, epl);
            fail();
        } catch (EPCompileException ex) {
            assertMessage(ex, message);
        }
    }

    public static void tryInvalidCompile(RegressionEnvironment env, String epl, String message) {
        try {
            env.compileWCheckedEx(epl);
            fail();
        } catch (EPCompileException ex) {
            assertMessage(ex, message);
        }
    }

    public static void tryInvalidCompile(RegressionEnvironment env, RegressionPath path, String epl, String message) {
        try {
            env.compileWCheckedEx(epl, path);
            fail();
        } catch (EPCompileException ex) {
            assertMessage(ex, message);
        }
    }

    public static void assertMessageContains(Throwable ex, String message) {
        if (!ex.getMessage().contains(message)) {
            fail("Does not contain text: '" + message + "' in text \n text:" + ex.getMessage());
        }
        if (message.trim().length() == 0) {
            ex.printStackTrace();
            fail("empty expected message");
        }
    }

    public static void assertMessage(Throwable ex, String message) {
        if (message.equals("skip")) {
            return; // skip message validation
        }
        if (message.length() > 10) {
            // Comment-in for logging: log.error("Exception: " + ex.getMessage(), ex);
            if (!ex.getMessage().startsWith(message)) {
                ex.printStackTrace();
                fail("\nExpected:" + message + "\nReceived:" + ex.getMessage());
            }
        } else {
            // Comment-in for logging: log.error("Exception: " + ex.getMessage(), ex);
            ex.printStackTrace();
            fail("No assertion provided, received: " + ex.getMessage());
        }
    }

    public static void assertMessage(String exceptionMessage, String expected) {
        if (expected.equals("skip")) {
            return; // skip message validation
        }
        if (expected.length() > 10) {
            // Comment-in for logging: log.error("Exception: " + ex.getMessage(), ex);
            if (!exceptionMessage.startsWith(expected)) {
                System.out.println(exceptionMessage);
                fail("\nExpected:" + expected + "\nReceived:" + exceptionMessage);
            }
        } else {
            // Comment-in for logging: log.error("Exception: " + ex.getMessage(), ex);
            System.out.println(exceptionMessage);
            fail("No assertion provided, received: " + exceptionMessage);
        }
    }

    public static void tryInvalidIterate(RegressionEnvironment env, String epl, String message) {
        env.compileDeploy(epl);
        try {
            env.statement("s0").iterator();
            fail();
        } catch (UnsupportedOperationException ex) {
            assertMessage(ex, message);
        }
        env.undeployAll();
    }

    public static void tryInvalidDeploy(RegressionEnvironment env, EPCompiled unit, String expected) {
        try {
            env.runtime().getDeploymentService().deploy(unit);
            fail();
        } catch (EPDeployException ex) {
            assertMessage(ex, expected);
        }
    }

    public static void tryInvalidDeploy(RegressionEnvironment env, RegressionPath path, String epl, String expected) {
        EPCompiled compiled = env.compile(epl, path);
        try {
            env.runtime().getDeploymentService().deploy(compiled);
            fail();
        } catch (EPDeployException ex) {
            assertMessage(ex, expected);
        }
        path.getCompileds().remove(compiled);
    }

    public static void tryInvalidProperty(EventBean event, String propertyName) {
        try {
            event.get(propertyName);
            Assert.fail();
        } catch (PropertyAccessException ex) {
            // expected
            assertMessage(ex, "Property named '" + propertyName + "' is not a valid property name for this type");
        }
    }

    public static void tryInvalidGetFragment(EventBean event, String propertyName) {
        try {
            event.getFragment(propertyName);
            Assert.fail();
        } catch (PropertyAccessException ex) {
            // expected
            assertMessage(ex, "Property named '" + propertyName + "' is not a valid property name for this type");
        }
    }

    public static void tryInvalidConfigurationCompiler(Configuration config, Consumer<Configuration> configurer, String expected) {
        config.getCommon().addEventType(SupportBean.class);
        configurer.accept(config);

        try {
            EPCompilerProvider.getCompiler().compile("select * from SupportBean", new CompilerArguments(config));
            fail();
        } catch (EPCompileException ex) {
            SupportMessageAssertUtil.assertMessage(ex, expected);
        }
    }

    public static void tryInvalidConfigurationRuntime(Configuration config, Consumer<Configuration> configurer, String expected) {
        config.getCommon().addEventType(SupportBean.class);
        configurer.accept(config);

        try {
            EPRuntimeProvider.getRuntime(UUID.randomUUID().toString(), config);
            fail();
        } catch (ConfigurationException ex) {
            SupportMessageAssertUtil.assertMessage(ex, expected);
        }
    }

    public static void tryInvalidConfigurationCompileAndRuntime(Configuration configuration, Consumer<Configuration> configurer, String expected) {
        tryInvalidConfigurationCompiler(configuration, configurer,
            "Failed compiler startup: " + expected);
        tryInvalidConfigurationRuntime(configuration, configurer,
            "Failed runtime startup: " + expected);
    }


    private static void compileFAFInternal(RegressionEnvironment env, RegressionPath path, String epl) throws EPCompileException {
        CompilerArguments args = new CompilerArguments(env.getConfiguration());
        args.getPath().addAll(path.getCompileds());
        EPCompilerProvider.getCompiler().compileQuery(epl, args);
    }
}
