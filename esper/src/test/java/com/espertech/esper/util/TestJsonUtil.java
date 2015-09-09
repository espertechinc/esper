/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.util;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.EngineImportServiceImpl;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.TimeZone;

public class TestJsonUtil extends TestCase {

    private EngineImportService engineImportService;

    protected void setUp() {
        engineImportService = new EngineImportServiceImpl(false, false, false, false, null, TimeZone.getDefault(), ConfigurationEngineDefaults.ThreadingProfile.NORMAL);
    }

    protected void tearDown() {
        engineImportService = null;
    }

    public void testUnmarshal() throws Exception {
        String json;
        Container result;

        json = "{'name':'c0', 'def': {'defString':'a', 'defBoolPrimitive':true, 'defintprimitive':10, 'defintboxed':20}}";
        result = (Container) JsonUtil.parsePopulate(json, Container.class, engineImportService);
        assertEquals("c0", result.getName());
        assertEquals((Integer) 20, result.getDef().getDefIntBoxed());
        assertEquals("a", result.getDef().getDefString());
        assertEquals(10, result.getDef().getDefIntPrimitive());
        assertEquals(true, result.getDef().isDefBoolPrimitive());

        json = "{\"name\":\"c1\",\"abc\":{'class':'TestJsonUtil$BImpl', \"bIdOne\":\"bidentone\",\"bIdTwo\":\"bidenttwo\"}}";
        result = (Container) JsonUtil.parsePopulate(json, Container.class, engineImportService);
        assertEquals("c1", result.getName());
        BImpl bimpl = (BImpl) result.getAbc();
        assertEquals("bidentone", bimpl.getbIdOne());
        assertEquals("bidenttwo", bimpl.getbIdTwo());

        json = "{\"name\":\"c2\",\"abc\":{'class':'com.espertech.esper.util.TestJsonUtil$AImpl'}}";
        result = (Container) JsonUtil.parsePopulate(json, Container.class, engineImportService);
        assertEquals("c2", result.getName());
        assertTrue(result.getAbc() instanceof AImpl);

        json = "{'booleanArray': [true, false, true], 'integerArray': [1], 'objectArray': [1, 'abc']}";
        DEF defOne = (DEF) JsonUtil.parsePopulate(json, DEF.class, engineImportService);
        EPAssertionUtil.assertEqualsExactOrder(new boolean[] {true, false, true}, defOne.getBooleanArray());
        EPAssertionUtil.assertEqualsExactOrder(new Object[] {1}, defOne.getIntegerArray());
        EPAssertionUtil.assertEqualsExactOrder(new Object[] {1, "abc"}, defOne.getObjectArray());

        json = "{defString:'a'}";
        DEF defTwo = (DEF) JsonUtil.parsePopulate(json, DEF.class, engineImportService);
        assertNull(defTwo.getObjectArray());

        json = "{'objectArray':[]}";
        DEF defThree = (DEF) JsonUtil.parsePopulate(json, DEF.class, engineImportService);
        assertEquals(0, defThree.getObjectArray().length);

        // note: notation for "field: value" does not require quotes around the field name
        json = "{objectArray:[ [1,2] ]}";
        defThree = (DEF) JsonUtil.parsePopulate(json, DEF.class, engineImportService);
        assertEquals(1, defThree.getObjectArray().length);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 2}, (Collection) defThree.getObjectArray()[0]);
    }

    public void testInvalid() {
        tryInvalid(Container.class, "'name'",
                "Failed to map value to object of type com.espertech.esper.util.TestJsonUtil$Container, expected Json Map/Object format, received String");

        tryInvalid(Container.class, "null",
                "Failed to map value to object of type com.espertech.esper.util.TestJsonUtil$Container, expected Json Map/Object format, received null");

        tryInvalid(NoCtor.class, "{a:1}",
                "Exception instantiating class com.espertech.esper.util.TestJsonUtil$NoCtor, please make sure the class has a public no-arg constructor (and for inner classes is declared static)");

        tryInvalid(ExceptionCtor.class, "{a:1}",
                "Exception instantiating class com.espertech.esper.util.TestJsonUtil$ExceptionCtor: Test exception");

        tryInvalid(DEF.class, "{'dummy': 'def'}",
                "Failed to find writable property 'dummy' for class com.espertech.esper.util.TestJsonUtil$DEF");

        tryInvalid(DEF.class, "{'defString': 1}",
                "Property 'defString' of class com.espertech.esper.util.TestJsonUtil$DEF expects an java.lang.String but receives a value of type java.lang.Integer");

        tryInvalid(DEF.class, "{'booleanArray': 1}",
                "Property 'booleanArray' of class com.espertech.esper.util.TestJsonUtil$DEF expects an array but receives a value of type java.lang.Integer");

        tryInvalid(DEF.class, "{'booleanArray': [1, 2]}",
                "Property 'booleanArray (array element)' of class boolean(Array) expects an boolean but receives a value of type java.lang.Integer");

        tryInvalid(DEF.class, "{'defString': [1, 2]}",
                "Property 'defString' of class com.espertech.esper.util.TestJsonUtil$DEF expects an java.lang.String but receives a value of type java.util.ArrayList");

        tryInvalid(Container.class, "{'abc': 'def'}",
                "Property 'abc' of class com.espertech.esper.util.TestJsonUtil$Container expects an com.espertech.esper.util.TestJsonUtil$ABC but receives a value of type java.lang.String");

        tryInvalid(Container.class, "{'abc': {a:1}}",
                "Failed to find implementation for interface com.espertech.esper.util.TestJsonUtil$ABC, for interfaces please specified the 'class' field that provides the class name either as a simple class name or fully qualified");

        tryInvalid(Container.class, "{'abc': {'class' : 'x.y.z'}}",
                "Failed to find implementation for interface com.espertech.esper.util.TestJsonUtil$ABC, could not find class by name 'x.y.z'");

        tryInvalid(Container.class, "{'abc': {'class' : 'xyz'}}",
                "Failed to find implementation for interface com.espertech.esper.util.TestJsonUtil$ABC, could not find class by name 'com.espertech.esper.util.xyz'");

        tryInvalid(Container.class, "{'abc': {'class' : 'java.lang.String'}}",
                "Failed to find implementation for interface com.espertech.esper.util.TestJsonUtil$ABC, class java.lang.String does not implement the interface");
    }

    private void tryInvalid(Class container, String json, String expected) {
        try {
            JsonUtil.parsePopulate(json, container, engineImportService);
            fail();
        }
        catch (ExprValidationException ex) {
            assertEquals(expected, ex.getMessage());
        }
    }

    public static class Container {
        private String name;
        private ABC abc;
        private DEF def;

        public Container() {
        }

        public Container(String name, ABC abc, DEF def) {
            this.name = name;
            this.abc = abc;
            this.def = def;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ABC getAbc() {
            return abc;
        }

        public void setAbc(ABC abc) {
            this.abc = abc;
        }

        public DEF getDef() {
            return def;
        }

        public void setDef(DEF def) {
            this.def = def;
        }
    }

    public static class DEF {
        private String defString;
        private boolean defBoolPrimitive;
        private int defIntPrimitive;
        private Integer defIntBoxed;
        private boolean[] booleanArray;
        private Object[] objectArray;
        private Integer[] integerArray;

        public DEF() {
        }

        public DEF(String defString, boolean defBoolPrimitive, int defIntPrimitive, Integer defIntBoxed) {
            this.defString = defString;
            this.defBoolPrimitive = defBoolPrimitive;
            this.defIntPrimitive = defIntPrimitive;
            this.defIntBoxed = defIntBoxed;
        }

        public boolean[] getBooleanArray() {
            return booleanArray;
        }

        public void setBooleanArray(boolean[] booleanArray) {
            this.booleanArray = booleanArray;
        }

        public Object[] getObjectArray() {
            return objectArray;
        }

        public void setObjectArray(Object[] objectArray) {
            this.objectArray = objectArray;
        }

        public Integer[] getIntegerArray() {
            return integerArray;
        }

        public void setIntegerArray(Integer[] integerArray) {
            this.integerArray = integerArray;
        }

        public String getDefString() {
            return defString;
        }

        public void setDefString(String defString) {
            this.defString = defString;
        }

        public boolean isDefBoolPrimitive() {
            return defBoolPrimitive;
        }

        public void setDefBoolPrimitive(boolean defBoolPrimitive) {
            this.defBoolPrimitive = defBoolPrimitive;
        }

        public int getDefIntPrimitive() {
            return defIntPrimitive;
        }

        public void setDefIntPrimitive(int defIntPrimitive) {
            this.defIntPrimitive = defIntPrimitive;
        }

        public Integer getDefIntBoxed() {
            return defIntBoxed;
        }

        public void setDefIntBoxed(Integer defIntBoxed) {
            this.defIntBoxed = defIntBoxed;
        }
    }

    public interface ABC {
    }

    public static class AImpl implements ABC {
        private String aid;

        public AImpl() {
        }

        public AImpl(String aid) {
            this.aid = aid;
        }

        public String getAid() {
            return aid;
        }

        public void setAid(String aid) {
            this.aid = aid;
        }
    }

    public static class BImpl implements ABC {
        private String bIdOne;
        private String bIdTwo;

        public BImpl() {
        }

        public BImpl(String bIdOne, String bIdTwo) {
            this.bIdOne = bIdOne;
            this.bIdTwo = bIdTwo;
        }

        public String getbIdOne() {
            return bIdOne;
        }

        public String getbIdTwo() {
            return bIdTwo;
        }

        public void setbIdOne(String bIdOne) {
            this.bIdOne = bIdOne;
        }

        public void setbIdTwo(String bIdTwo) {
            this.bIdTwo = bIdTwo;
        }
    }

    public static class NoCtor {
        public NoCtor(String dummy) {
        }
    }

    public static class ExceptionCtor {
        public ExceptionCtor() {
            throw new RuntimeException("Test exception");
        }
    }
}
