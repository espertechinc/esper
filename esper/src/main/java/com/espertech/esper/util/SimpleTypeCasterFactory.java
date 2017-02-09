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
 * Factory for casters, which take an object and safely cast to a given type, performing coercion or dropping
 * precision if required.
 */
public class SimpleTypeCasterFactory {
    /**
     * Returns a caster that casts to a target type.
     *
     * @param fromType   can be null, if not known
     * @param targetType to cast to
     * @return caster for casting objects to the required type
     */
    public static SimpleTypeCaster getCaster(Class fromType, Class targetType) {
        if (fromType == targetType) {
            return new NullCaster();
        }

        targetType = JavaClassHelper.getBoxedType(targetType);
        if (targetType == Integer.class) {
            return new IntCaster();
        } else if (targetType == Long.class) {
            return new LongCaster();
        } else if (targetType == Double.class) {
            return new DoubleCaster();
        } else if (targetType == Float.class) {
            return new FloatCaster();
        } else if (targetType == Short.class) {
            return new ShortCaster();
        } else if (targetType == Byte.class) {
            return new ByteCaster();
        } else if ((targetType == Character.class) && (fromType == String.class)) {
            return new CharacterCaster();
        } else if (targetType == BigInteger.class) {
            return new BigIntCaster();
        } else if (targetType == BigDecimal.class) {
            if (JavaClassHelper.isFloatingPointClass(fromType)) {
                return new BigDecDoubleCaster();
            }
            return new BigDecLongCaster();
        } else {
            return new SimpleTypeCasterAnyType(targetType);
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class DoubleCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).doubleValue();
        }

        public boolean isNumericCast() {
            return true;
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class FloatCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).floatValue();
        }

        public boolean isNumericCast() {
            return true;
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class LongCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).longValue();
        }

        public boolean isNumericCast() {
            return true;
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class IntCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).intValue();
        }

        public boolean isNumericCast() {
            return true;
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class ShortCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).shortValue();
        }

        public boolean isNumericCast() {
            return true;
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class ByteCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return ((Number) object).byteValue();
        }

        public boolean isNumericCast() {
            return true;
        }
    }

    /**
     * Cast implementation for char values.
     */
    private static class CharacterCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            String value = object.toString();
            if ((value == null) || (value.length() == 0)) {
                return null;
            }
            return value.charAt(0);
        }

        public boolean isNumericCast() {
            return false;
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class BigIntCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            long value = ((Number) object).longValue();
            return BigInteger.valueOf(value);
        }

        public boolean isNumericCast() {
            return true;
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class BigDecLongCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            long value = ((Number) object).longValue();
            return new BigDecimal(value);
        }

        public boolean isNumericCast() {
            return true;
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class BigDecDoubleCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            double value = ((Number) object).doubleValue();
            return new BigDecimal(value);
        }

        public boolean isNumericCast() {
            return true;
        }
    }

    /**
     * Cast implementation for numeric values.
     */
    private static class NullCaster implements SimpleTypeCaster {
        public Object cast(Object object) {
            return object;
        }

        public boolean isNumericCast() {
            return false;
        }
    }
}
