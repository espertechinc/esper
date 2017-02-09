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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Factory for conversion/coercion and widening implementations for numbers.
 */
public class SimpleNumberCoercerFactory {
    private static SimpleNumberCoercerNull nullCoerce = new SimpleNumberCoercerNull();
    private static SimpleNumberCoercerDouble doubleCoerce = new SimpleNumberCoercerDouble();
    private static SimpleNumberCoercerLong longCoerce = new SimpleNumberCoercerLong();
    private static SimpleNumberCoercerFloat floatCoerce = new SimpleNumberCoercerFloat();
    private static SimpleNumberCoercerInt intCoerce = new SimpleNumberCoercerInt();
    private static SimpleNumberCoercerShort shortCoerce = new SimpleNumberCoercerShort();
    private static SimpleNumberCoercerByte byteCoerce = new SimpleNumberCoercerByte();
    private static SimpleNumberCoercerBigInt bigIntCoerce = new SimpleNumberCoercerBigInt();
    private static SimpleNumberCoercerBigIntNull bigIntCoerceNull = new SimpleNumberCoercerBigIntNull();
    private static SimpleNumberCoercerBigDecLong bigDecCoerceLong = new SimpleNumberCoercerBigDecLong();
    private static SimpleNumberCoercerBigDecDouble bigDecCoerceDouble = new SimpleNumberCoercerBigDecDouble();
    private static SimpleNumberCoercerBigDecNull bigDecCoerceNull = new SimpleNumberCoercerBigDecNull();

    /**
     * Returns a coercer/widener to BigDecimal for a given type.
     *
     * @param fromType to widen
     * @return widener
     */
    public static SimpleNumberBigDecimalCoercer getCoercerBigDecimal(Class fromType) {
        if (fromType == BigDecimal.class) {
            return bigDecCoerceNull;
        }
        if (JavaClassHelper.isFloatingPointClass(fromType)) {
            return bigDecCoerceDouble;
        }
        return bigDecCoerceLong;
    }

    /**
     * Returns a coercer/widener to BigInteger for a given type.
     *
     * @param fromType to widen
     * @return widener
     */
    public static SimpleNumberBigIntegerCoercer getCoercerBigInteger(Class fromType) {
        if (fromType == BigInteger.class) {
            return bigIntCoerceNull;
        }
        return bigIntCoerce;
    }

    /**
     * Returns a coercer/widener/narrower to a result number type from a given type.
     *
     * @param fromType        to widen/narrow, can be null to indicate that no shortcut-coercer is used
     * @param resultBoxedType type to widen/narrow to
     * @return widener/narrower
     */
    public static SimpleNumberCoercer getCoercer(Class fromType, Class resultBoxedType) {
        if (fromType == resultBoxedType) {
            return nullCoerce;
        }
        if (resultBoxedType == Double.class) {
            return doubleCoerce;
        }
        if (resultBoxedType == Long.class) {
            return longCoerce;
        }
        if (resultBoxedType == Float.class) {
            return floatCoerce;
        }
        if (resultBoxedType == Integer.class) {
            return intCoerce;
        }
        if (resultBoxedType == Short.class) {
            return shortCoerce;
        }
        if (resultBoxedType == Byte.class) {
            return byteCoerce;
        }
        if (resultBoxedType == BigInteger.class) {
            return bigIntCoerce;
        }
        if (resultBoxedType == BigDecimal.class) {
            if (JavaClassHelper.isFloatingPointClass(fromType)) {
                return bigDecCoerceDouble;
            }
            return bigDecCoerceLong;
        }
        throw new IllegalArgumentException("Cannot coerce to number subtype " + resultBoxedType.getName());
    }

    private static class SimpleNumberCoercerNull implements SimpleNumberCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce;
        }

        public Class getReturnType() {
            return Number.class;
        }
    }

    private static class SimpleNumberCoercerDouble implements SimpleNumberCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.doubleValue();
        }

        public Class getReturnType() {
            return Double.class;
        }
    }

    private static class SimpleNumberCoercerLong implements SimpleNumberCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.longValue();
        }

        public Class getReturnType() {
            return Long.class;
        }
    }

    private static class SimpleNumberCoercerInt implements SimpleNumberCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.intValue();
        }

        public Class getReturnType() {
            return Integer.class;
        }
    }

    private static class SimpleNumberCoercerFloat implements SimpleNumberCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.floatValue();
        }

        public Class getReturnType() {
            return Float.class;
        }
    }

    private static class SimpleNumberCoercerShort implements SimpleNumberCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.shortValue();
        }

        public Class getReturnType() {
            return Short.class;
        }
    }

    private static class SimpleNumberCoercerByte implements SimpleNumberCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce.byteValue();
        }

        public Class getReturnType() {
            return Byte.class;
        }
    }

    private static class SimpleNumberCoercerBigInt implements SimpleNumberCoercer, SimpleNumberBigIntegerCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return BigInteger.valueOf(numToCoerce.longValue());
        }

        public BigInteger coerceBoxedBigInt(Number numToCoerce) {
            return BigInteger.valueOf(numToCoerce.longValue());
        }

        public Class getReturnType() {
            return Long.class;
        }
    }

    private static class SimpleNumberCoercerBigDecLong implements SimpleNumberCoercer, SimpleNumberBigDecimalCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return new BigDecimal(numToCoerce.longValue());
        }

        public BigDecimal coerceBoxedBigDec(Number numToCoerce) {
            return new BigDecimal(numToCoerce.longValue());
        }

        public Class getReturnType() {
            return Long.class;
        }
    }

    private static class SimpleNumberCoercerBigDecDouble implements SimpleNumberCoercer, SimpleNumberBigDecimalCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return new BigDecimal(numToCoerce.doubleValue());
        }

        public BigDecimal coerceBoxedBigDec(Number numToCoerce) {
            return new BigDecimal(numToCoerce.doubleValue());
        }

        public Class getReturnType() {
            return Double.class;
        }
    }

    private static class SimpleNumberCoercerBigIntNull implements SimpleNumberCoercer, SimpleNumberBigIntegerCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce;
        }

        public BigInteger coerceBoxedBigInt(Number numToCoerce) {
            return (BigInteger) numToCoerce;
        }

        public Class getReturnType() {
            return Number.class;
        }
    }

    private static class SimpleNumberCoercerBigDecNull implements SimpleNumberCoercer, SimpleNumberBigDecimalCoercer {
        public Number coerceBoxed(Number numToCoerce) {
            return numToCoerce;
        }

        public BigDecimal coerceBoxedBigDec(Number numToCoerce) {
            return (BigDecimal) numToCoerce;
        }

        public Class getReturnType() {
            return Number.class;
        }
    }
}
