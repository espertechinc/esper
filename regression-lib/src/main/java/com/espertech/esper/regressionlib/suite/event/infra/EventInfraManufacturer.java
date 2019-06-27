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
package com.espertech.esper.regressionlib.suite.event.infra;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class EventInfraManufacturer implements RegressionExecution {
    public static final String XML_TYPENAME = EventInfraManufacturer.class.getSimpleName() + "XML";
    public static final String AVRO_TYPENAME = EventInfraManufacturer.class.getSimpleName() + "AVRO";

    public void run(RegressionEnvironment env) {
        // Bean
        runAssertion(env, "create schema BeanEvent as " + MyLocalBeanEvent.class.getName(),
            und -> {
                MyLocalBeanEvent bean = (MyLocalBeanEvent) und;
                assertEquals("a", bean.getP1());
                assertEquals(1, bean.getP2());
            });

        // Map
        runAssertion(env, "create map schema MapEvent(p1 string, p2 int)",
            und -> {
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{"a", 1}, ((Map) und).values());
            });

        // Object-array
        runAssertion(env, "create objectarray schema MapEvent(p1 string, p2 int)",
            und -> {
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{"a", 1}, (Object[]) und);
            });

        // Avro
        runAssertion(env, "select * from " + AVRO_TYPENAME, und -> {
            GenericData.Record rec = (GenericData.Record) und;
            assertEquals("a", rec.get("p1"));
            assertEquals(1, rec.get("p2"));
        });

        // Json
        runAssertion(env, "create json schema JsonEvent(p1 string, p2 int)",
            und -> {
                EPAssertionUtil.assertEqualsExactOrder(new Object[]{"a", 1}, ((Map) und).values());
            });

        // Json-Class-Provided
        runAssertion(env, "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') create json schema JsonEvent()",
            und -> {
                MyLocalJsonProvided received = (MyLocalJsonProvided) und;
                assertEquals("a", received.p1);
                assertEquals(1, received.p2);
            });
    }

    private void runAssertion(RegressionEnvironment env, String epl, Consumer<Object> underlyingAssertion) {
        env.compileDeploy("@public @name('schema') " + epl);

        EventTypeSPI type = (EventTypeSPI) env.deployment().getDeployment(env.deploymentId("schema")).getStatements()[0].getEventType();

        Set<WriteablePropertyDescriptor> writables = EventTypeUtility.getWriteableProperties(type, true, true);
        WriteablePropertyDescriptor[] props = new WriteablePropertyDescriptor[2];
        props[0] = findProp(writables, "p1");
        props[1] = findProp(writables, "p2");

        EPRuntimeSPI spi = (EPRuntimeSPI) env.runtime();
        EventBeanManufacturer manufacturer;
        try {
            EventBeanManufacturerForge forge = EventTypeUtility.getManufacturer(type, props, spi.getServicesContext().getClasspathImportServiceRuntime(), true, spi.getServicesContext().getEventTypeAvroHandler());
            manufacturer = forge.getManufacturer(spi.getServicesContext().getEventBeanTypedEventFactory());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        EventBean event = manufacturer.make(new Object[]{"a", 1});
        underlyingAssertion.accept(event.getUnderlying());
        assertSame(event.getEventType(), type);

        Object underlying = manufacturer.makeUnderlying(new Object[]{"a", 1});
        underlyingAssertion.accept(underlying);

        env.undeployAll();
    }

    private WriteablePropertyDescriptor findProp(Set<WriteablePropertyDescriptor> writables, String propertyName) {
        for (WriteablePropertyDescriptor prop : writables) {
            if (prop.getPropertyName().equals(propertyName)) {
                return prop;
            }
        }
        fail();
        return null;
    }

    public static class MyLocalBeanEvent {
        private String p1;
        private int p2;

        public String getP1() {
            return p1;
        }

        public void setP1(String p1) {
            this.p1 = p1;
        }

        public int getP2() {
            return p2;
        }

        public void setP2(int p2) {
            this.p2 = p2;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public String p1;
        public int p2;
    }
}
