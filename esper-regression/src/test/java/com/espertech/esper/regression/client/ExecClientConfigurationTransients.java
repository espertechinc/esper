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

import com.espertech.esper.client.*;
import com.espertech.esper.client.util.ClassForNameProvider;
import com.espertech.esper.client.util.ClassForNameProviderDefault;
import com.espertech.esper.client.util.ClassLoaderProvider;
import com.espertech.esper.client.util.FastClassClassLoaderProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.MyAnnotationSimple;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecClientConfigurationTransients implements RegressionExecution {
    private final static String SERVICE_NAME = "TEST_SERVICE_NAME";
    private final static int SECRET_VALUE = 12345;

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);

        // add service (not serializable, transient configuration)
        HashMap<String, Object> transients = new HashMap<String, Object>();
        transients.put(SERVICE_NAME, new MyLocalService(SECRET_VALUE));
        configuration.setTransientConfiguration(transients);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionConfigAvailable(epService);
        runAssertionClassForNameForbiddenClass(epService);
        runAssertionClassLoader(epService);
    }

    private void runAssertionConfigAvailable(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean");
        MyListener listener = new MyListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(SECRET_VALUE, listener.getSecretValue());

        stmt.destroy();
    }

    private void runAssertionClassForNameForbiddenClass(EPServiceProvider epService) {

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        String epl = "select java.lang.System.exit(-1) from SupportBean";

        epService.getEPAdministrator().getConfiguration().getTransientConfiguration().put(ClassForNameProvider.NAME, new MyClassForNameProvider());
        SupportMessageAssertUtil.tryInvalid(epService, epl,
                "Error starting statement: Failed to validate select-clause expression 'java.lang.System.exit(-1)': Failed to resolve 'java.lang.System.exit' to");

        epService.getEPAdministrator().getConfiguration().getTransientConfiguration().put(ClassForNameProvider.NAME, ClassForNameProviderDefault.INSTANCE);
        epService.getEPAdministrator().createEPL(epl);
    }

    private void runAssertionClassLoader(EPServiceProvider epService) {
        ConfigurationOperations ops = epService.getEPAdministrator().getConfiguration();

        MyClassLoaderProvider.setInvoked(false);
        MyFastClassClassLoaderProvider.setClazz(null);
        ops.getTransientConfiguration().put(ClassLoaderProvider.NAME, new MyClassLoaderProvider());
        ops.getTransientConfiguration().put(FastClassClassLoaderProvider.NAME, new MyFastClassClassLoaderProvider());
        ops.addImport(MyAnnotationSimple.class);
        ops.addEventType(SupportBean.class);

        String epl = "@MyAnnotationSimple select java.lang.System.exit(-1) from SupportBean";
        epService.getEPAdministrator().createEPL(epl);

        assertTrue(MyClassLoaderProvider.isInvoked());
        assertEquals(System.class, MyFastClassClassLoaderProvider.getClazz());
    }

    public static class MyLocalService {
        private final int secretValue;

        MyLocalService(int secretValue) {
            this.secretValue = secretValue;
        }

        int getSecretValue() {
            return secretValue;
        }
    }

    public static class MyListener implements StatementAwareUpdateListener {
        private int secretValue;

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {
            MyLocalService svc = (MyLocalService) epServiceProvider.getEPAdministrator().getConfiguration().getTransientConfiguration().get(SERVICE_NAME);
            secretValue = svc.getSecretValue();
        }

        int getSecretValue() {
            return secretValue;
        }
    }

    private static class MyClassForNameProvider implements ClassForNameProvider {
        public Class classForName(String className) throws ClassNotFoundException {
            if (className.equals("java.lang.System")) {
                throw new UnsupportedOperationException("Access to class '" + className + " is not permitted");
            }
            return ClassForNameProviderDefault.INSTANCE.classForName(className);
        }
    }

    public static class MyClassLoaderProvider implements ClassLoaderProvider {
        private static boolean invoked;

        public ClassLoader classloader() {
            invoked = true;
            return Thread.currentThread().getContextClassLoader();
        }

        public static boolean isInvoked() {
            return invoked;
        }

        static void setInvoked(boolean invoked) {
            MyClassLoaderProvider.invoked = invoked;
        }
    }

    public static class MyFastClassClassLoaderProvider implements FastClassClassLoaderProvider {
        private static Class clazz;

        public ClassLoader classloader(Class clazz) {
            MyFastClassClassLoaderProvider.clazz = clazz;
            return Thread.currentThread().getContextClassLoader();
        }

        public static Class getClazz() {
            return clazz;
        }

        public static void setClazz(Class clazz) {
            MyFastClassClassLoaderProvider.clazz = clazz;
        }
    }
}
