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
package com.espertech.esper.util;

import com.espertech.esper.type.*;

import java.util.Locale;

/**
 * A factory for creating an instance of a parser that parses a String and returns a target type.
 */
public class SimpleTypeParserFactory {
    /**
     * Returns a parsers for the String value using the given Java built-in class for parsing.
     *
     * @param clazz is the class to parse the value to
     * @return value matching the type passed in
     */
    public static SimpleTypeParser getParser(Class clazz) {
        Class classBoxed = JavaClassHelper.getBoxedType(clazz);

        if (classBoxed == String.class) {
            return new SimpleTypeParser() {
                public Object parse(String value) {
                    return value;
                }
            };
        }
        if (classBoxed == Character.class) {
            return new SimpleTypeParser() {
                public Object parse(String value) {
                    return value.charAt(0);
                }
            };
        }
        if (classBoxed == Boolean.class) {
            return new SimpleTypeParser() {
                public Object parse(String text) {
                    return BoolValue.parseString(text.toLowerCase(Locale.ENGLISH).trim());
                }
            };
        }
        if (classBoxed == Byte.class) {
            return new SimpleTypeParser() {
                public Object parse(String text) {
                    return ByteValue.parseString(text.trim());
                }
            };
        }
        if (classBoxed == Short.class) {
            return new SimpleTypeParser() {
                public Object parse(String text) {
                    return ShortValue.parseString(text.trim());
                }
            };
        }
        if (classBoxed == Long.class) {
            return new SimpleTypeParser() {
                public Object parse(String text) {
                    return LongValue.parseString(text.trim());
                }
            };
        }
        if (classBoxed == Float.class) {
            return new SimpleTypeParser() {
                public Object parse(String text) {
                    return FloatValue.parseString(text.trim());
                }
            };
        }
        if (classBoxed == Double.class) {
            return new SimpleTypeParser() {
                public Object parse(String text) {
                    return DoubleValue.parseString(text.trim());
                }
            };
        }
        if (classBoxed == Integer.class) {
            return new SimpleTypeParser() {
                public Object parse(String text) {
                    return IntValue.parseString(text.trim());
                }
            };
        }
        return null;
    }
}
