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
package com.espertech.esper.type;

import com.espertech.esper.collection.MultiKeyUntyped;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing relational types of operation.
 */
public enum BitWiseOpEnum {
    /**
     * Bitwise and.
     */
    BAND("&"),
    /**
     * Bitwise or.
     */
    BOR("|"),
    /**
     * Bitwise xor.
     */
    BXOR("^");

    private static Map<MultiKeyUntyped, BitWiseOpEnum.Computer> computers;

    private String expressionText;

    private BitWiseOpEnum(String expressionText) {
        this.expressionText = expressionText;
    }

    /**
     * Returns the operator as an expression text.
     *
     * @return text of operator
     */
    public String getExpressionText() {
        return expressionText;
    }

    static {
        computers = new HashMap<MultiKeyUntyped, BitWiseOpEnum.Computer>();
        computers.put(new MultiKeyUntyped(new Object[]{Byte.class, BAND}), new BAndByte());
        computers.put(new MultiKeyUntyped(new Object[]{Short.class, BAND}), new BAndShort());
        computers.put(new MultiKeyUntyped(new Object[]{Integer.class, BAND}), new BAndInt());
        computers.put(new MultiKeyUntyped(new Object[]{Long.class, BAND}), new BAndLong());
        computers.put(new MultiKeyUntyped(new Object[]{Boolean.class, BAND}), new BAndBoolean());
        computers.put(new MultiKeyUntyped(new Object[]{Byte.class, BOR}), new BOrByte());
        computers.put(new MultiKeyUntyped(new Object[]{Short.class, BOR}), new BOrShort());
        computers.put(new MultiKeyUntyped(new Object[]{Integer.class, BOR}), new BOrInt());
        computers.put(new MultiKeyUntyped(new Object[]{Long.class, BOR}), new BOrLong());
        computers.put(new MultiKeyUntyped(new Object[]{Boolean.class, BOR}), new BOrBoolean());
        computers.put(new MultiKeyUntyped(new Object[]{Byte.class, BXOR}), new BXorByte());
        computers.put(new MultiKeyUntyped(new Object[]{Short.class, BXOR}), new BXorShort());
        computers.put(new MultiKeyUntyped(new Object[]{Integer.class, BXOR}), new BXorInt());
        computers.put(new MultiKeyUntyped(new Object[]{Long.class, BXOR}), new BXorLong());
        computers.put(new MultiKeyUntyped(new Object[]{Boolean.class, BXOR}), new BXorBoolean());
    }

    /**
     * Returns number or boolean computation for the target coercion type.
     *
     * @param coercedType - target type
     * @return number cruncher
     */
    public Computer getComputer(Class coercedType) {
        if ((coercedType != Byte.class) &&
                (coercedType != Short.class) &&
                (coercedType != Integer.class) &&
                (coercedType != Long.class) &&
                (coercedType != Boolean.class)) {
            throw new IllegalArgumentException("Expected base numeric or boolean type for computation result but got type " + coercedType);
        }
        MultiKeyUntyped key = new MultiKeyUntyped(new Object[]{coercedType, this});
        return computers.get(key);
    }

    /**
     * Computer for relational op.
     */
    public interface Computer {
        /**
         * Computes using the 2 numbers or boolean a result object.
         *
         * @param objOne is the first number or boolean
         * @param objTwo is the second number or boolean
         * @return result
         */

        public Object compute(Object objOne, Object objTwo);
    }

    /**
     * Computer for type-specific arith. operations.
     */
    /**
     * Bit Wise And.
     */
    public static class BAndByte implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Byte n1 = (Byte) objOne;
            Byte n2 = (Byte) objTwo;
            return (byte) (n1.byteValue() & n2.byteValue());
        }
    }

    /**
     * Bit Wise Or.
     */
    public static class BOrByte implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Byte n1 = (Byte) objOne;
            Byte n2 = (Byte) objTwo;
            return (byte) (n1.byteValue() | n2.byteValue());
        }
    }

    /**
     * Bit Wise Xor.
     */
    public static class BXorByte implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Byte n1 = (Byte) objOne;
            Byte n2 = (Byte) objTwo;
            return (byte) (n1.byteValue() ^ n2.byteValue());
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    /**
     * Bit Wise And.
     */
    public static class BAndShort implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Short n1 = (Short) objOne;
            Short n2 = (Short) objTwo;
            return (short) (n1.shortValue() & n2.shortValue());
        }
    }

    /**
     * Bit Wise Or.
     */
    public static class BOrShort implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Short n1 = (Short) objOne;
            Short n2 = (Short) objTwo;
            return (short) (n1.shortValue() | n2.shortValue());
        }
    }

    /**
     * Bit Wise Xor.
     */
    public static class BXorShort implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Short n1 = (Short) objOne;
            Short n2 = (Short) objTwo;
            return (short) (n1.shortValue() ^ n2.shortValue());
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    /**
     * Bit Wise And.
     */
    public static class BAndInt implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Integer n1 = (Integer) objOne;
            Integer n2 = (Integer) objTwo;
            return n1.intValue() & n2.intValue();
        }
    }

    /**
     * Bit Wise Or.
     */
    public static class BOrInt implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Integer n1 = (Integer) objOne;
            Integer n2 = (Integer) objTwo;
            return n1.intValue() | n2.intValue();
        }
    }

    /**
     * Bit Wise Xor.
     */
    public static class BXorInt implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Integer n1 = (Integer) objOne;
            Integer n2 = (Integer) objTwo;
            return n1.intValue() ^ n2.intValue();
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    /**
     * Bit Wise And.
     */
    public static class BAndLong implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Long n1 = (Long) objOne;
            Long n2 = (Long) objTwo;
            return n1.longValue() & n2.longValue();
        }
    }

    /**
     * Bit Wise Or.
     */
    public static class BOrLong implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Long n1 = (Long) objOne;
            Long n2 = (Long) objTwo;
            return n1.longValue() | n2.longValue();
        }
    }

    /**
     * Bit Wise Xor.
     */
    public static class BXorLong implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Long n1 = (Long) objOne;
            Long n2 = (Long) objTwo;
            return n1.longValue() ^ n2.longValue();
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    /**
     * Bit Wise And.
     */
    public static class BAndBoolean implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Boolean b1 = (Boolean) objOne;
            Boolean b2 = (Boolean) objTwo;
            return b1.booleanValue() & b2.booleanValue();
        }
    }

    /**
     * Bit Wise Or.
     */
    public static class BOrBoolean implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Boolean b1 = (Boolean) objOne;
            Boolean b2 = (Boolean) objTwo;
            return b1.booleanValue() | b2.booleanValue();
        }
    }

    /**
     * Bit Wise Xor.
     */
    public static class BXorBoolean implements Computer {
        public Object compute(Object objOne, Object objTwo) {
            Boolean b1 = (Boolean) objOne;
            Boolean b2 = (Boolean) objTwo;
            return b1.booleanValue() ^ b2.booleanValue();
        }
    }

    /**
     * Returns string rendering of enum.
     *
     * @return bitwise operator string
     */
    public String getComputeDescription() {
        return expressionText;
    }
}
