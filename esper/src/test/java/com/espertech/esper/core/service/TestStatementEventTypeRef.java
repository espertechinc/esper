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
package com.espertech.esper.core.service;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.util.CollectionUtil;
import junit.framework.TestCase;

import java.util.HashSet;

public class TestStatementEventTypeRef extends TestCase {
    private StatementEventTypeRefImpl service;

    public void setUp() {
        service = new StatementEventTypeRefImpl();
    }

    public void testFlowNoRemoveType() {
        addReference("s0", "e1");
        assertTrue(service.isInUse("e1"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e1").toArray(), new Object[]{"s0"});

        addReference("s0", "e2");
        assertTrue(service.isInUse("e2"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e2").toArray(), new Object[]{"s0"});

        addReference("s1", "e1");
        assertTrue(service.isInUse("e1"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e1").toArray(), new Object[]{"s0", "s1"});

        addReference("s1", "e1");
        assertTrue(service.isInUse("e1"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e1").toArray(), new Object[]{"s0", "s1"});

        assertFalse(service.isInUse("e3"));
        addReference("s2", "e3");
        assertTrue(service.isInUse("e3"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e3").toArray(), new Object[]{"s2"});

        service.removeReferencesStatement("s2");
        assertFalse(service.isInUse("e3"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e3").toArray(), new Object[0]);

        service.removeReferencesStatement("s0");
        assertTrue(service.isInUse("e1"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e1").toArray(), new Object[]{"s1"});

        service.removeReferencesStatement("s1");
        assertFalse(service.isInUse("e1"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e1").toArray(), new Object[0]);

        HashSet<String> values = new HashSet<String>();
        values.add("e5");
        values.add("e6");
        service.addReferences("s4", CollectionUtil.toArray(values));

        assertTrue(service.isInUse("e5"));
        assertTrue(service.isInUse("e6"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e5").toArray(), new Object[]{"s4"});

        service.removeReferencesStatement("s4");

        assertFalse(service.isInUse("e5"));
        assertFalse(service.isInUse("e6"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e5").toArray(), new Object[0]);

        assertEquals(0, service.getTypeToStmt().size());
        assertEquals(0, service.getTypeToStmt().size());
    }

    public void testFlowRemoveType() {
        addReference("s0", "e1");
        addReference("s1", "e1");
        addReference("s2", "e2");

        assertTrue(service.isInUse("e1"));
        service.removeReferencesType("e1");
        assertFalse(service.isInUse("e1"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e1").toArray(), new Object[0]);

        assertTrue(service.isInUse("e2"));
        service.removeReferencesType("e2");
        assertFalse(service.isInUse("e2"));

        service.removeReferencesType("e3");

        assertEquals(0, service.getTypeToStmt().size());
        assertEquals(0, service.getTypeToStmt().size());
    }

    public void testInvalid() {
        service.removeReferencesStatement("s1");

        addReference("s2", "e2");

        assertTrue(service.isInUse("e2"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e2").toArray(), new Object[]{"s2"});

        service.removeReferencesStatement("s2");

        assertFalse(service.isInUse("e2"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e2").toArray(), new Object[0]);

        service.removeReferencesStatement("s2");

        assertFalse(service.isInUse("e2"));
        EPAssertionUtil.assertEqualsAnyOrder(service.getStatementNamesForType("e2").toArray(), new Object[0]);

        assertEquals(0, service.getTypeToStmt().size());
        assertEquals(0, service.getTypeToStmt().size());
    }

    private void addReference(String stmtName, String typeName) {
        HashSet<String> set = new HashSet<String>();
        set.add(typeName);
        service.addReferences(stmtName, CollectionUtil.toArray(set));
    }
}
