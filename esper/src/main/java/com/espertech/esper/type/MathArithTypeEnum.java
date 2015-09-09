/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.type;

import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.util.SimpleNumberBigDecimalCoercer;
import com.espertech.esper.util.SimpleNumberBigIntegerCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration for the type of arithmatic to use.
 */
public enum MathArithTypeEnum
{
    /**
     * Plus.
     */
    ADD ("+"),

    /**
     * Minus.
     */
    SUBTRACT ("-"),

    /**
     * Divide.
     */
    DIVIDE ("/"),

    /**
     * Multiply.
     */
    MULTIPLY ("*"),

    /**
     * Modulo.
     */
    MODULO ("%");

    private static Map<MultiKeyUntyped, Computer> computers;

    static
    {
        computers = new HashMap<MultiKeyUntyped, Computer>();
        computers.put(new MultiKeyUntyped(new Object[] {Double.class, ADD}), new AddDouble());
        computers.put(new MultiKeyUntyped(new Object[] {Float.class, ADD}), new AddFloat());
        computers.put(new MultiKeyUntyped(new Object[] {Long.class, ADD}), new AddLong());
        computers.put(new MultiKeyUntyped(new Object[] {Integer.class, ADD}), new AddInt());
        computers.put(new MultiKeyUntyped(new Object[] {BigDecimal.class, ADD}), new AddBigDec());
        computers.put(new MultiKeyUntyped(new Object[] {BigInteger.class, ADD}), new AddBigInt());
        computers.put(new MultiKeyUntyped(new Object[] {Double.class, SUBTRACT}), new SubtractDouble());
        computers.put(new MultiKeyUntyped(new Object[] {Float.class, SUBTRACT}), new SubtractFloat());
        computers.put(new MultiKeyUntyped(new Object[] {Long.class, SUBTRACT}), new SubtractLong());
        computers.put(new MultiKeyUntyped(new Object[] {Integer.class, SUBTRACT}), new SubtractInt());
        computers.put(new MultiKeyUntyped(new Object[] {BigDecimal.class, SUBTRACT}), new SubtractBigDec());
        computers.put(new MultiKeyUntyped(new Object[] {BigInteger.class, SUBTRACT}), new SubtractBigInt());
        computers.put(new MultiKeyUntyped(new Object[] {Double.class, MULTIPLY}), new MultiplyDouble());
        computers.put(new MultiKeyUntyped(new Object[] {Float.class, MULTIPLY}), new MultiplyFloat());
        computers.put(new MultiKeyUntyped(new Object[] {Long.class, MULTIPLY}), new MultiplyLong());
        computers.put(new MultiKeyUntyped(new Object[] {Integer.class, MULTIPLY}), new MultiplyInt());
        computers.put(new MultiKeyUntyped(new Object[] {BigDecimal.class, MULTIPLY}), new MultiplyBigDec());
        computers.put(new MultiKeyUntyped(new Object[] {BigInteger.class, MULTIPLY}), new MultiplyBigInt());
        computers.put(new MultiKeyUntyped(new Object[] {Double.class, MODULO}), new ModuloDouble());
        computers.put(new MultiKeyUntyped(new Object[] {Float.class, MODULO}), new ModuloFloat());
        computers.put(new MultiKeyUntyped(new Object[] {Long.class, MODULO}), new ModuloLong());
        computers.put(new MultiKeyUntyped(new Object[] {Integer.class, MODULO}), new ModuloInt());
        computers.put(new MultiKeyUntyped(new Object[] {BigDecimal.class, MODULO}), new ModuloDouble());
        computers.put(new MultiKeyUntyped(new Object[] {BigInteger.class, MODULO}), new ModuloLong());
    }

    /**
     * Interface for number cruncher.
     */
    public interface Computer
    {
        /**
         * Computes using the 2 numbers a result number.
         * @param d1 is the first number
         * @param d2 is the second number
         * @return result
         */
        public Number compute(Number d1, Number d2);
    }

    private String expressionText;

    private MathArithTypeEnum(String expressionText)
    {
        this.expressionText = expressionText;
    }

    /**
     * Returns number cruncher for the target coercion type.
     * @param coercedType - target type
     * @param typeOne - the LHS type
     * @param typeTwo - the RHS type
     * @param isIntegerDivision - false for division returns double, true for using Java-standard integer division
     * @param isDivisionByZeroReturnsNull - false for division-by-zero returns infinity, true for null
     * @return number cruncher
     */
    public Computer getComputer(Class coercedType, Class typeOne, Class typeTwo, boolean isIntegerDivision, boolean isDivisionByZeroReturnsNull, MathContext optionalMathContext)
    {
        if ( (coercedType != Double.class) &&
             (coercedType != Float.class) &&
             (coercedType != Long.class) &&
             (coercedType != Integer.class) &&
             (coercedType != BigDecimal.class) &&
             (coercedType != BigInteger.class) &&
             (coercedType != Short.class) &&
             (coercedType != Byte.class))
        {
            throw new IllegalArgumentException("Expected base numeric type for computation result but got type " + coercedType);
        }

        if (coercedType == BigDecimal.class)
        {
            return makeBigDecimalComputer(typeOne, typeTwo, isDivisionByZeroReturnsNull, optionalMathContext);
        }
        if (coercedType == BigInteger.class)
        {
            return makeBigIntegerComputer(typeOne, typeTwo);
        }

        if (this != DIVIDE)
        {
            MultiKeyUntyped key = new MultiKeyUntyped(new Object[] {coercedType, this});
            Computer computer = computers.get(key);
            if (computer == null)
            {
                throw new IllegalArgumentException("Could not determine process or type " + this + " type " + coercedType);
            }
            return computer;
        }

        if (!isIntegerDivision)
        {
            return new DivideDouble(isDivisionByZeroReturnsNull);
        }

        if (coercedType == Double.class) return new DivideDouble(isDivisionByZeroReturnsNull);
        if (coercedType == Float.class) return new DivideFloat();
        if (coercedType == Long.class) return new DivideLong();
        if (coercedType == Integer.class) return new DivideInt();
        if (coercedType == BigDecimal.class) {
            if (optionalMathContext != null) {
                return new DivideBigDecWMathContext(isDivisionByZeroReturnsNull, optionalMathContext);
            }
            return new DivideBigDec(isDivisionByZeroReturnsNull);
        }
        if (coercedType == BigInteger.class) return new DivideBigInt();

        throw new IllegalArgumentException("Could not determine process or type " + this + " type " + coercedType);
    }

    private Computer makeBigDecimalComputer(Class typeOne, Class typeTwo, boolean divisionByZeroReturnsNull, MathContext optionalMathContext)
    {
        if ((typeOne == BigDecimal.class) && (typeTwo == BigDecimal.class))
        {
            if (this == DIVIDE)
            {
                if (optionalMathContext != null) {
                    return new DivideBigDecWMathContext(divisionByZeroReturnsNull, optionalMathContext);
                }
                return new DivideBigDec(divisionByZeroReturnsNull);
            }
            return computers.get(new MultiKeyUntyped(new Object[] {BigDecimal.class, this}));
        }
        SimpleNumberBigDecimalCoercer convertorOne = SimpleNumberCoercerFactory.getCoercerBigDecimal(typeOne);
        SimpleNumberBigDecimalCoercer convertorTwo = SimpleNumberCoercerFactory.getCoercerBigDecimal(typeTwo);
        if (this == ADD)
        {
            return new AddBigDecConvComputer(convertorOne, convertorTwo);
        }
        if (this == SUBTRACT)
        {
            return new SubtractBigDecConvComputer(convertorOne, convertorTwo);
        }
        if (this == MULTIPLY)
        {
            return new MultiplyBigDecConvComputer(convertorOne, convertorTwo);
        }
        if (this == DIVIDE)
        {
            if (optionalMathContext == null) {
                return new DivideBigDecConvComputerNoMathCtx(convertorOne, convertorTwo, divisionByZeroReturnsNull);
            }
            return new DivideBigDecConvComputerWithMathCtx(convertorOne, convertorTwo, divisionByZeroReturnsNull, optionalMathContext);
        }
        return new ModuloDouble();
    }

    private Computer makeBigIntegerComputer(Class typeOne, Class typeTwo)
    {
        if ((typeOne == BigDecimal.class) && (typeTwo == BigDecimal.class))
        {
            return computers.get(new MultiKeyUntyped(new Object[] {BigDecimal.class, this}));
        }
        SimpleNumberBigIntegerCoercer convertorOne = SimpleNumberCoercerFactory.getCoercerBigInteger(typeOne);
        SimpleNumberBigIntegerCoercer convertorTwo = SimpleNumberCoercerFactory.getCoercerBigInteger(typeTwo);
        if (this == ADD)
        {
            return new AddBigIntConvComputer(convertorOne, convertorTwo);
        }
        if (this == SUBTRACT)
        {
            return new SubtractBigIntConvComputer(convertorOne, convertorTwo);
        }
        if (this == MULTIPLY)
        {
            return new MultiplyBigIntConvComputer(convertorOne, convertorTwo);
        }
        if (this == DIVIDE)
        {
            return new DivideBigIntConvComputer(convertorOne, convertorTwo);
        }
        return new ModuloLong();
    }

    /**
     * Computer for type-specific arith. operations.
     */
    public static class AddDouble implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.doubleValue() + d2.doubleValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class AddShort implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.shortValue() + d2.shortValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class AddByte implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.byteValue() + d2.byteValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class AddFloat implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.floatValue() + d2.floatValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class AddLong implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.longValue() + d2.longValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class AddInt implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.intValue() + d2.intValue();
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    public static class AddBigInt implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            BigInteger b1 = (BigInteger) d1;
            BigInteger b2 = (BigInteger) d2;
            return b1.add(b2);
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class AddBigDec implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            BigDecimal b1 = (BigDecimal) d1;
            BigDecimal b2 = (BigDecimal) d2;
            return b1.add(b2);
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    public static class SubtractDouble implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.doubleValue() - d2.doubleValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class SubtractFloat implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.floatValue() - d2.floatValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class SubtractLong implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.longValue() - d2.longValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class SubtractInt implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.intValue() - d2.intValue();
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    public static class SubtractBigInt implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            BigInteger b1 = (BigInteger) d1;
            BigInteger b2 = (BigInteger) d2;
            return b1.subtract(b2);
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class SubtractBigDec implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            BigDecimal b1 = (BigDecimal) d1;
            BigDecimal b2 = (BigDecimal) d2;
            return b1.subtract(b2);
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    public static class DivideDouble implements Computer
    {
        private final boolean divisionByZeroReturnsNull;

        /**
         * Ctor.
         * @param divisionByZeroReturnsNull false for division-by-zero returns infinity, true for null
         */
        public DivideDouble(boolean divisionByZeroReturnsNull)
        {
            this.divisionByZeroReturnsNull = divisionByZeroReturnsNull;
        }

        public Number compute(Number d1, Number d2)
        {
            double d2Double = d2.doubleValue();
            if ((divisionByZeroReturnsNull) && (d2Double == 0))
            {
                return null;
            }
            return d1.doubleValue() / d2Double;
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class DivideFloat implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            float d2Float = d2.floatValue();
            if (d2Float == 0)
            {
                return null;
            }
            return d1.floatValue() / d2Float;
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class DivideLong implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            long d2Long = d2.longValue();
            if (d2Long == 0)
            {
                return null;
            }
            return d1.longValue() / d2Long;
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class DivideInt implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            int d2Int = d2.intValue();
            if (d2Int == 0)
            {
                return null;
            }
            return d1.intValue() / d2Int;
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    public static class DivideBigInt implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            BigInteger b1 = (BigInteger) d1;
            BigInteger b2 = (BigInteger) d2;
            if (b2.doubleValue() == 0)
            {
                return null;
            }
            return b1.divide(b2);
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    public static class DivideBigDec implements Computer
    {
        private final boolean divisionByZeroReturnsNull;

        /**
         * Ctor.
         * @param divisionByZeroReturnsNull false for division-by-zero returns infinity, true for null
         */
        public DivideBigDec(boolean divisionByZeroReturnsNull)
        {
            this.divisionByZeroReturnsNull = divisionByZeroReturnsNull;
        }

        public Number compute(Number d1, Number d2)
        {
            BigDecimal b1 = (BigDecimal) d1;
            BigDecimal b2 = (BigDecimal) d2;
            if (b2.doubleValue() == 0)
            {
                if (divisionByZeroReturnsNull)
                {
                    return null;
                }
                else
                {
                    double result = b1.doubleValue() / 0;       // serves to create the right sign for infinity
                    return new BigDecimal(result);
                }
            }
            return b1.divide(b2);
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    public static class DivideBigDecWMathContext implements Computer
    {
        private final boolean divisionByZeroReturnsNull;
        private final MathContext mathContext;

        /**
         * Ctor.
         * @param divisionByZeroReturnsNull false for division-by-zero returns infinity, true for null
         * @param mathContext math context
         */
        public DivideBigDecWMathContext(boolean divisionByZeroReturnsNull, MathContext mathContext)
        {
            this.divisionByZeroReturnsNull = divisionByZeroReturnsNull;
            this.mathContext = mathContext;
        }

        public Number compute(Number d1, Number d2)
        {
            BigDecimal b1 = (BigDecimal) d1;
            BigDecimal b2 = (BigDecimal) d2;
            if (b2.doubleValue() == 0)
            {
                if (divisionByZeroReturnsNull)
                {
                    return null;
                }
                else
                {
                    double result = b1.doubleValue() / 0;       // serves to create the right sign for infinity
                    return new BigDecimal(result);
                }
            }
            return b1.divide(b2, mathContext);
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    public static class MultiplyDouble implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.doubleValue() * d2.doubleValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class MultiplyFloat implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.floatValue() * d2.floatValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class MultiplyLong implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.longValue() * d2.longValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class MultiplyInt implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.intValue() * d2.intValue();
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    public static class MultiplyBigInt implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            BigInteger b1 = (BigInteger) d1;
            BigInteger b2 = (BigInteger) d2;
            return b1.multiply(b2);
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class MultiplyBigDec implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            BigDecimal b1 = (BigDecimal) d1;
            BigDecimal b2 = (BigDecimal) d2;
            return b1.multiply(b2);
        }
    }

    /**
     * Computer for type-specific arith. operations.
     */
    public static class ModuloDouble implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.doubleValue() % d2.doubleValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class ModuloFloat implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.floatValue() % d2.floatValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class ModuloLong implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.longValue() % d2.longValue();
        }
    }
    /**
     * Computer for type-specific arith. operations.
     */
    public static class ModuloInt implements Computer
    {
        public Number compute(Number d1, Number d2)
        {
            return d1.intValue() % d2.intValue();
        }
    }

    /**
     * Computer for math op.
     */
    public static class AddBigDecConvComputer implements Computer
    {
        private final SimpleNumberBigDecimalCoercer convOne;
        private final SimpleNumberBigDecimalCoercer convTwo;

        /**
         * Ctor.
         * @param convOne conversion for LHS
         * @param convTwo conversion for RHS
         */
        public AddBigDecConvComputer(SimpleNumberBigDecimalCoercer convOne, SimpleNumberBigDecimalCoercer convTwo)
        {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public Number compute(Number d1, Number d2)
        {
            BigDecimal s1 = convOne.coerceBoxedBigDec(d1);
            BigDecimal s2 = convTwo.coerceBoxedBigDec(d2);
            return s1.add(s2);
        }
    }

    /**
     * Computer for math op.
     */
    public static class SubtractBigDecConvComputer implements Computer
    {
        private final SimpleNumberBigDecimalCoercer convOne;
        private final SimpleNumberBigDecimalCoercer convTwo;

        /**
         * Ctor.
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         */
        public SubtractBigDecConvComputer(SimpleNumberBigDecimalCoercer convOne, SimpleNumberBigDecimalCoercer convTwo)
        {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public Number compute(Number d1, Number d2)
        {
            BigDecimal s1 = convOne.coerceBoxedBigDec(d1);
            BigDecimal s2 = convTwo.coerceBoxedBigDec(d2);
            return s1.subtract(s2);
        }
    }

    /**
     * Computer for math op.
     */
    public static class MultiplyBigDecConvComputer implements Computer
    {
        private final SimpleNumberBigDecimalCoercer convOne;
        private final SimpleNumberBigDecimalCoercer convTwo;

        /**
         * Ctor.
         * @param convOne conversion for LHS
         * @param convTwo conversion for RHS
         */
        public MultiplyBigDecConvComputer(SimpleNumberBigDecimalCoercer convOne, SimpleNumberBigDecimalCoercer convTwo)
        {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public Number compute(Number d1, Number d2)
        {
            BigDecimal s1 = convOne.coerceBoxedBigDec(d1);
            BigDecimal s2 = convTwo.coerceBoxedBigDec(d2);
            return s1.multiply(s2);
        }
    }

    /**
     * Computer for math op.
     */
    public abstract static class DivideBigDecConvComputerBase implements Computer
    {
        private final SimpleNumberBigDecimalCoercer convOne;
        private final SimpleNumberBigDecimalCoercer convTwo;
        private final boolean divisionByZeroReturnsNull;

        public abstract Number doDivide(BigDecimal s1, BigDecimal s2);

        /**
         * Ctor.
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         * @param divisionByZeroReturnsNull false for division-by-zero returns infinity, true for null
         */
        public DivideBigDecConvComputerBase(SimpleNumberBigDecimalCoercer convOne, SimpleNumberBigDecimalCoercer convTwo, boolean divisionByZeroReturnsNull)
        {
            this.convOne = convOne;
            this.convTwo = convTwo;
            this.divisionByZeroReturnsNull = divisionByZeroReturnsNull;
        }

        public Number compute(Number d1, Number d2)
        {
            BigDecimal s1 = convOne.coerceBoxedBigDec(d1);
            BigDecimal s2 = convTwo.coerceBoxedBigDec(d2);
            if (s2.doubleValue() == 0)
            {
                if (divisionByZeroReturnsNull)
                {
                    return null;
                }
                else
                {
                    double result = s1.doubleValue() / 0;
                    return new BigDecimal(result);
                }
            }
            return doDivide(s1, s2);
        }
    }

    public static class DivideBigDecConvComputerNoMathCtx extends DivideBigDecConvComputerBase
    {
        public DivideBigDecConvComputerNoMathCtx(SimpleNumberBigDecimalCoercer convOne, SimpleNumberBigDecimalCoercer convTwo, boolean divisionByZeroReturnsNull) {
            super(convOne, convTwo, divisionByZeroReturnsNull);
        }

        public Number doDivide(BigDecimal s1, BigDecimal s2) {
            return s1.divide(s2);
        }
    }

    public static class DivideBigDecConvComputerWithMathCtx extends DivideBigDecConvComputerBase
    {
        private final MathContext mathContext;

        public DivideBigDecConvComputerWithMathCtx(SimpleNumberBigDecimalCoercer convOne, SimpleNumberBigDecimalCoercer convTwo, boolean divisionByZeroReturnsNull, MathContext mathContext) {
            super(convOne, convTwo, divisionByZeroReturnsNull);
            this.mathContext = mathContext;
        }

        public Number doDivide(BigDecimal s1, BigDecimal s2) {
            return s1.divide(s2, mathContext);
        }
    }

    /**
     * Computer for math op.
     */
    public static class AddBigIntConvComputer implements Computer
    {
        private final SimpleNumberBigIntegerCoercer convOne;
        private final SimpleNumberBigIntegerCoercer convTwo;

        /**
         * Ctor.
         * @param convOne conversion for LHS
         * @param convTwo conversion for RHS
         */
        public AddBigIntConvComputer(SimpleNumberBigIntegerCoercer convOne, SimpleNumberBigIntegerCoercer convTwo)
        {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public Number compute(Number d1, Number d2)
        {
            BigInteger s1 = convOne.coerceBoxedBigInt(d1);
            BigInteger s2 = convTwo.coerceBoxedBigInt(d2);
            return s1.add(s2);
        }
    }

    /**
     * Computer for math op.
     */
    public static class SubtractBigIntConvComputer implements Computer
    {
        private final SimpleNumberBigIntegerCoercer convOne;
        private final SimpleNumberBigIntegerCoercer convTwo;

        /**
         * Ctor.
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         */
        public SubtractBigIntConvComputer(SimpleNumberBigIntegerCoercer convOne, SimpleNumberBigIntegerCoercer convTwo)
        {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public Number compute(Number d1, Number d2)
        {
            BigInteger s1 = convOne.coerceBoxedBigInt(d1);
            BigInteger s2 = convTwo.coerceBoxedBigInt(d2);
            return s1.subtract(s2);
        }
    }

    /**
     * Computer for math op.
     */
    public static class MultiplyBigIntConvComputer implements Computer
    {
        private final SimpleNumberBigIntegerCoercer convOne;
        private final SimpleNumberBigIntegerCoercer convTwo;

        /**
         * Ctor.
         * @param convOne conversion for LHS
         * @param convTwo conversion for RHS
         */
        public MultiplyBigIntConvComputer(SimpleNumberBigIntegerCoercer convOne, SimpleNumberBigIntegerCoercer convTwo)
        {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public Number compute(Number d1, Number d2)
        {
            BigInteger s1 = convOne.coerceBoxedBigInt(d1);
            BigInteger s2 = convTwo.coerceBoxedBigInt(d2);
            return s1.multiply(s2);
        }
    }

    /**
     * Computer for math op.
     */
    public static class DivideBigIntConvComputer implements Computer
    {
        private final SimpleNumberBigIntegerCoercer convOne;
        private final SimpleNumberBigIntegerCoercer convTwo;

        /**
         * Ctor.
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         */
        public DivideBigIntConvComputer(SimpleNumberBigIntegerCoercer convOne, SimpleNumberBigIntegerCoercer convTwo)
        {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public Number compute(Number d1, Number d2)
        {
            BigInteger s1 = convOne.coerceBoxedBigInt(d1);
            BigInteger s2 = convTwo.coerceBoxedBigInt(d2);
            if (s2.doubleValue() == 0)
            {
                return null;
            }
            return s1.divide(s2);
        }
    }
    /**
     * Returns string representation of enum.
     * @return text for enum
     */
    public String getExpressionText()
    {
        return expressionText;
    }

    /**
     * Returns the math operator for the string.
     * @param operator to parse
     * @return math enum
     */
    public static MathArithTypeEnum parseOperator(String operator)
    {
        for (int i = 0; i < MathArithTypeEnum.values().length; i++)
        {
            MathArithTypeEnum val = MathArithTypeEnum.values()[i];
            if (val.getExpressionText().equals(operator))
            {
                return MathArithTypeEnum.values()[i];
            }
        }
        throw new IllegalArgumentException("Unknown operator '" + operator + "'");
    }
}
