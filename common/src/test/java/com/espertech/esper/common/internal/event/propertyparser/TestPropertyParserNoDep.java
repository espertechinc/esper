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
package com.espertech.esper.common.internal.event.propertyparser;

import com.espertech.esper.common.internal.event.property.*;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.espertech.esper.common.internal.event.propertyparser.PropertyParserNoDep.parseMappedProperty;

public class TestPropertyParserNoDep extends TestCase {

    public void testParse() {
        Property property;
        List<Property> nested;

        property = PropertyParser.parseAndWalk("a", false);
        assertEquals("a", ((SimpleProperty) property).getPropertyNameAtomic());

        property = PropertyParser.parseAndWalk("i[1]", false);
        assertEquals("i", ((IndexedProperty) property).getPropertyNameAtomic());
        assertEquals(1, ((IndexedProperty) property).getIndex());

        property = PropertyParser.parseAndWalk("m('key')", false);
        assertEquals("m", ((MappedProperty) property).getPropertyNameAtomic());
        assertEquals("key", ((MappedProperty) property).getKey());

        property = PropertyParser.parseAndWalk("a.b[2].c('m')", false);
        nested = ((NestedProperty) property).getProperties();
        assertEquals(3, nested.size());
        assertEquals("a", ((SimpleProperty) nested.get(0)).getPropertyNameAtomic());
        assertEquals("b", ((IndexedProperty) nested.get(1)).getPropertyNameAtomic());
        assertEquals(2, ((IndexedProperty) nested.get(1)).getIndex());
        assertEquals("c", ((MappedProperty) nested.get(2)).getPropertyNameAtomic());
        assertEquals("m", ((MappedProperty) nested.get(2)).getKey());

        property = PropertyParser.parseAndWalk("a", true);
        assertEquals("a", ((DynamicSimpleProperty) property).getPropertyNameAtomic());

        property = PropertyParser.parseAndWalk("`order`.p0", false);
        nested = ((NestedProperty) property).getProperties();
        assertEquals(2, nested.size());
        assertEquals("order", ((SimpleProperty) nested.get(0)).getPropertyNameAtomic());
        assertEquals("p0", ((SimpleProperty) nested.get(1)).getPropertyNameAtomic());

        property = PropertyParser.parseAndWalk("`jim's strings`.p0", false);
        nested = ((NestedProperty) property).getProperties();
        assertEquals(2, nested.size());
        assertEquals("jim's strings", ((SimpleProperty) nested.get(0)).getPropertyNameAtomic());
        assertEquals("p0", ((SimpleProperty) nested.get(1)).getPropertyNameAtomic());

        property = PropertyParser.parseAndWalk("`children's books`[0]", false);
        IndexedProperty indexed = (IndexedProperty) property;
        assertEquals(0, indexed.getIndex());
        assertEquals("children's books", indexed.getPropertyNameAtomic());

        property = PropertyParser.parseAndWalk("x\\.y", false);
        assertEquals("x.y", ((SimpleProperty) property).getPropertyNameAtomic());
        property = PropertyParser.parseAndWalk("x\\.\\.y", false);
        assertEquals("x..y", ((SimpleProperty) property).getPropertyNameAtomic());
    }

    public void testParseMapKey() throws Exception {
        assertEquals("a", tryKey("a"));
    }

    public void testParseMappedProp() {
        MappedPropertyParseResult result = parseMappedProperty("a.b('c')");
        assertEquals("a", result.getClassName());
        assertEquals("b", result.getMethodName());
        assertEquals("c", result.getArgString());

        result = parseMappedProperty("SupportStaticMethodLib.delimitPipe('POLYGON ((100.0 100, \", 100 100, 400 400))')");
        assertEquals("SupportStaticMethodLib", result.getClassName());
        assertEquals("delimitPipe", result.getMethodName());
        assertEquals("POLYGON ((100.0 100, \", 100 100, 400 400))", result.getArgString());

        result = parseMappedProperty("a.b.c.d.e('f.g.h,u.h')");
        assertEquals("a.b.c.d", result.getClassName());
        assertEquals("e", result.getMethodName());
        assertEquals("f.g.h,u.h", result.getArgString());

        result = parseMappedProperty("a.b.c.d.E(\"hfhf f f f \")");
        assertEquals("a.b.c.d", result.getClassName());
        assertEquals("E", result.getMethodName());
        assertEquals("hfhf f f f ", result.getArgString());

        result = parseMappedProperty("c.d.getEnumerationSource(\"kf\"kf'kf\")");
        assertEquals("c.d", result.getClassName());
        assertEquals("getEnumerationSource", result.getMethodName());
        assertEquals("kf\"kf'kf", result.getArgString());

        result = parseMappedProperty("c.d.getEnumerationSource('kf\"kf'kf\"')");
        assertEquals("c.d", result.getClassName());
        assertEquals("getEnumerationSource", result.getMethodName());
        assertEquals("kf\"kf'kf\"", result.getArgString());

        result = parseMappedProperty("f('a')");
        assertEquals(null, result.getClassName());
        assertEquals("f", result.getMethodName());
        assertEquals("a", result.getArgString());

        assertNull(parseMappedProperty("('a')"));
        assertNull(parseMappedProperty(""));
    }

    private String tryKey(String key) throws Exception {
        String propertyName = "m(\"" + key + "\")";
        log.debug(".tryKey propertyName=" + propertyName + " key=" + key);
        Property property = PropertyParser.parseAndWalk(propertyName, false);
        return ((MappedProperty) property).getKey();
    }

    private final static Logger log = LoggerFactory.getLogger(TestPropertyParserNoDep.class);
}