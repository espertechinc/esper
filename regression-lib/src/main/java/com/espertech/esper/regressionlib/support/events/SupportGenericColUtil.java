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
package com.espertech.esper.regressionlib.support.events;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.internal.support.SupportEventPropDesc;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;

import java.util.*;

import static com.espertech.esper.common.client.type.EPTypeClassParameterized.from;
import static org.junit.Assert.assertEquals;

public class SupportGenericColUtil {
    public final static PairOfNameAndType[] NAMESANDTYPES = new PairOfNameAndType[]{
        fromPair("listOfString", "java.util.List<String>", from(List.class, String.class)),
        fromPair("listOfOptionalInteger", "java.util.List<Optional<Integer>>", from(List.class, from(Optional.class, Integer.class))),
        fromPair("mapOfStringAndInteger", "java.util.Map<String, Integer>", from(Map.class, String.class, Integer.class)),
        fromPair("listArrayOfString", "java.util.List<String>[]", from(List[].class, String.class)),
        fromPair("listOfStringArray", "java.util.List<String[]>", from(List.class, String[].class)),
        fromPair("listArray2DimOfString", "java.util.List<String>[][]", from(List[][].class, String.class)),
        fromPair("listOfStringArray2Dim", "java.util.List<String[][]>", from(List.class, String[][].class)),
        fromPair("listOfT", "java.util.List<Object>", from(List.class, Object.class))
    };

    public static String allNames() {
        StringBuilder names = new StringBuilder();
        String delimiter = "";
        for (PairOfNameAndType pair : NAMESANDTYPES) {
            names.append(delimiter).append(pair.name);
            delimiter = ",";
        }
        return names.toString();
    }

    public static String allNamesAndTypes() {
        StringBuilder names = new StringBuilder();
        String delimiter = "";
        for (PairOfNameAndType pair : NAMESANDTYPES) {
            names.append(delimiter).append(pair.name).append(" ").append(pair.getType());
            delimiter = ",";
        }
        return names.toString();
    }

    public static void assertPropertyEPTypes(EventType type) {
        SupportEventPropUtil.assertPropsEquals(type.getPropertyDescriptors(),
            new SupportEventPropDesc("listOfString", EPTypeClassParameterized.from(List.class, String.class)),
            new SupportEventPropDesc("listOfOptionalInteger", from(List.class, EPTypeClassParameterized.from(Optional.class, Integer.class))),
            new SupportEventPropDesc("mapOfStringAndInteger", EPTypeClassParameterized.from(Map.class, String.class, Integer.class)),
            new SupportEventPropDesc("listArrayOfString", EPTypeClassParameterized.from(List[].class, String.class)),
            new SupportEventPropDesc("listOfStringArray", EPTypeClassParameterized.from(List.class, String[].class)),
            new SupportEventPropDesc("listArray2DimOfString", EPTypeClassParameterized.from(List[][].class, String.class)),
            new SupportEventPropDesc("listOfStringArray2Dim", EPTypeClassParameterized.from(List.class, String[][].class)),
            new SupportEventPropDesc("listOfT", EPTypeClassParameterized.from(List.class, Object.class))
        );
    }

    public static Map<String, Object> getSampleEvent() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("listOfString", makeListOfString());
        fields.put("listOfOptionalInteger", makeListOfOptionalInteger());
        fields.put("mapOfStringAndInteger", makeMapOfStringAndInteger());
        fields.put("listArrayOfString", makeListArrayOfString());
        fields.put("listOfStringArray", makeListOfStringArray());
        fields.put("listArray2DimOfString", makeListArray2DimOfString());
        fields.put("listOfStringArray2Dim", makeListOfStringArray2Dim());
        fields.put("listOfT", makeListOfT());
        return fields;
    }

    public static void compare(EventBean event) {
        assertEquals(makeListOfString(), event.get("listOfString"));
        assertEquals(makeListOfOptionalInteger(), event.get("listOfOptionalInteger"));
        assertEquals(makeMapOfStringAndInteger(), event.get("mapOfStringAndInteger"));
        assertEquals(Arrays.toString(makeListArrayOfString()), Arrays.toString((List[]) event.get("listArrayOfString")));
        EPAssertionUtil.assertEqualsExactOrder(makeListOfStringArray().toArray(), ((List) event.get("listOfStringArray")).toArray());
        assertEquals(Arrays.toString(makeListArray2DimOfString()[0]), Arrays.toString(((List[][]) event.get("listArray2DimOfString"))[0]));
        EPAssertionUtil.assertEqualsExactOrder(makeListOfStringArray2Dim().toArray(), ((List) event.get("listOfStringArray2Dim")).toArray());
        EPAssertionUtil.assertEqualsExactOrder(makeListOfT().toArray(), ((List) event.get("listOfT")).toArray());
    }

    private static List<String> makeListOfString() {
        return Arrays.asList("a");
    }

    private static List<Optional<Integer>> makeListOfOptionalInteger() {
        return Arrays.asList(Optional.of(10));
    }

    private static Map<String, Integer> makeMapOfStringAndInteger() {
        return Collections.singletonMap("k", 20);
    }

    private static List<String>[] makeListArrayOfString() {
        return new List[]{Arrays.asList("b")};
    }

    private static List<String[]> makeListOfStringArray() {
        List<String[]> list = new ArrayList<>();
        list.add(new String[]{"c"});
        return list;
    }

    private static List<String>[][] makeListArray2DimOfString() {
        return new List[][]{{Arrays.asList("b")}};
    }

    private static List<String[][]> makeListOfStringArray2Dim() {
        List<String[][]> list = new ArrayList<>();
        list.add(new String[][]{{"c"}});
        return list;
    }

    private static List<Object> makeListOfT() {
        return Arrays.asList("x");
    }

    private static PairOfNameAndType fromPair(String name, String type, EPTypeClass typeClass) {
        return new PairOfNameAndType(name, type, typeClass);
    }

    public static class PairOfNameAndType {
        private final String name;
        private final String type;
        private final EPTypeClass typeClass;

        public PairOfNameAndType(String name, String type, EPTypeClass typeClass) {
            this.name = name;
            this.type = type;
            this.typeClass = typeClass;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public EPTypeClass getTypeClass() {
            return typeClass;
        }
    }
}
