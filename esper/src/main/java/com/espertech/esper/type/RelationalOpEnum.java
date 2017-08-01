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

import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRelational;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.util.SimpleNumberBigDecimalCoercer;
import com.espertech.esper.util.SimpleNumberBigIntegerCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Enum representing relational types of operation.
 */
public enum RelationalOpEnum {
    /**
     * Greater then.
     */
    GT(">"),

    /**
     * Greater equals.
     */
    GE(">="),

    /**
     * Lesser then.
     */
    LT("<"),

    /**
     * Lesser equals.
     */
    LE("<=");

    private static Map<MultiKeyUntyped, RelationalOpEnum.Computer> computers;

    private String expressionText;

    private RelationalOpEnum(String expressionText) {
        this.expressionText = expressionText;
    }

    public RelationalOpEnum reversed() {
        if (GT == this) {
            return LT;
        } else if (GE == this) {
            return LE;
        } else if (LE == this) {
            return GE;
        }
        return GT;
    }

    /**
     * Parses the operator and returns an enum for the operator.
     *
     * @param op to parse
     * @return enum representing relational operation
     */
    public static RelationalOpEnum parse(String op) {
        if (op.equals("<")) {
            return LT;
        } else if (op.equals(">")) {
            return GT;
        } else if ((op.equals(">=")) || op.equals("=>")) {
            return GE;
        } else if ((op.equals("<=")) || op.equals("=<")) {
            return LE;
        } else throw new IllegalArgumentException("Invalid relational operator '" + op + "'");
    }

    static {
        computers = new HashMap<>();
        computers.put(new MultiKeyUntyped(new Object[]{String.class, GT}), new GTStringComputer());
        computers.put(new MultiKeyUntyped(new Object[]{String.class, GE}), new GEStringComputer());
        computers.put(new MultiKeyUntyped(new Object[]{String.class, LT}), new LTStringComputer());
        computers.put(new MultiKeyUntyped(new Object[]{String.class, LE}), new LEStringComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Integer.class, GT}), new GTIntegerComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Integer.class, GE}), new GEIntegerComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Integer.class, LT}), new LTIntegerComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Integer.class, LE}), new LEIntegerComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Long.class, GT}), new GTLongComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Long.class, GE}), new GELongComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Long.class, LT}), new LTLongComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Long.class, LE}), new LELongComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Double.class, GT}), new GTDoubleComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Double.class, GE}), new GEDoubleComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Double.class, LT}), new LTDoubleComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Double.class, LE}), new LEDoubleComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Float.class, GT}), new GTFloatComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Float.class, GE}), new GEFloatComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Float.class, LT}), new LTFloatComputer());
        computers.put(new MultiKeyUntyped(new Object[]{Float.class, LE}), new LEFloatComputer());
        computers.put(new MultiKeyUntyped(new Object[]{BigDecimal.class, GT}), new GTBigDecComputer());
        computers.put(new MultiKeyUntyped(new Object[]{BigDecimal.class, GE}), new GEBigDecComputer());
        computers.put(new MultiKeyUntyped(new Object[]{BigDecimal.class, LT}), new LTBigDecComputer());
        computers.put(new MultiKeyUntyped(new Object[]{BigDecimal.class, LE}), new LEBigDecComputer());
        computers.put(new MultiKeyUntyped(new Object[]{BigInteger.class, GT}), new GTBigIntComputer());
        computers.put(new MultiKeyUntyped(new Object[]{BigInteger.class, GE}), new GEBigIntComputer());
        computers.put(new MultiKeyUntyped(new Object[]{BigInteger.class, LT}), new LTBigIntComputer());
        computers.put(new MultiKeyUntyped(new Object[]{BigInteger.class, LE}), new LEBigIntComputer());
    }

    /**
     * Returns the computer to use for the relational operation based on the coercion type.
     *
     * @param coercedType is the object type
     * @param typeOne     the compare-to type on the LHS
     * @param typeTwo     the compare-to type on the RHS
     * @return computer for performing the relational op
     */
    public RelationalOpEnum.Computer getComputer(Class coercedType, Class typeOne, Class typeTwo) {
        if ((coercedType != Double.class) &&
                (coercedType != Float.class) &&
                (coercedType != Integer.class) &&
                (coercedType != Long.class) &&
                (coercedType != String.class) &&
                (coercedType != BigDecimal.class) &&
                (coercedType != BigInteger.class)) {
            throw new IllegalArgumentException("Unsupported type for relational op compare, type " + coercedType);
        }

        if (coercedType == BigDecimal.class) {
            return makeBigDecimalComputer(typeOne, typeTwo);
        }
        if (coercedType == BigInteger.class) {
            return makeBigIntegerComputer(typeOne, typeTwo);
        }

        MultiKeyUntyped key = new MultiKeyUntyped(new Object[]{coercedType, this});
        return computers.get(key);
    }

    private Computer makeBigDecimalComputer(Class typeOne, Class typeTwo) {
        if ((typeOne == BigDecimal.class) && (typeTwo == BigDecimal.class)) {
            return computers.get(new MultiKeyUntyped(new Object[]{BigDecimal.class, this}));
        }
        SimpleNumberBigDecimalCoercer convertorOne = SimpleNumberCoercerFactory.getCoercerBigDecimal(typeOne);
        SimpleNumberBigDecimalCoercer convertorTwo = SimpleNumberCoercerFactory.getCoercerBigDecimal(typeTwo);
        if (this == GT) {
            return new GTBigDecConvComputer(convertorOne, convertorTwo);
        }
        if (this == LT) {
            return new LTBigDecConvComputer(convertorOne, convertorTwo);
        }
        if (this == GE) {
            return new GEBigDecConvComputer(convertorOne, convertorTwo);
        }
        return new LEBigDecConvComputer(convertorOne, convertorTwo);
    }

    private Computer makeBigIntegerComputer(Class typeOne, Class typeTwo) {
        if ((typeOne == BigInteger.class) && (typeTwo == BigInteger.class)) {
            return computers.get(new MultiKeyUntyped(new Object[]{BigInteger.class, this}));
        }
        SimpleNumberBigIntegerCoercer convertorOne = SimpleNumberCoercerFactory.getCoercerBigInteger(typeOne);
        SimpleNumberBigIntegerCoercer convertorTwo = SimpleNumberCoercerFactory.getCoercerBigInteger(typeTwo);
        if (this == GT) {
            return new GTBigIntConvComputer(convertorOne, convertorTwo);
        }
        if (this == LT) {
            return new LTBigIntConvComputer(convertorOne, convertorTwo);
        }
        if (this == GE) {
            return new GEBigIntConvComputer(convertorOne, convertorTwo);
        }
        return new LEBigIntConvComputer(convertorOne, convertorTwo);
    }

    /**
     * Computer for relational op.
     */
    public interface Computer {
        /**
         * Compares objects and returns boolean indicating larger (true) or smaller (false).
         *
         * @param objOne object to compare
         * @param objTwo object to compare
         * @return true if larger, false if smaller
         */
        public boolean compare(Object objOne, Object objTwo);

        CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType);
    }

    /**
     * Computer for relational op compare.
     */
    public static class GTStringComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            String s1 = (String) objOne;
            String s2 = (String) objTwo;
            int result = s1.compareTo(s2);
            return result > 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenStringCompare(lhs, lhsType, rhs, rhsType, CodegenExpressionRelational.CodegenRelational.GT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GEStringComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            String s1 = (String) objOne;
            String s2 = (String) objTwo;
            return s1.compareTo(s2) >= 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenStringCompare(lhs, lhsType, rhs, rhsType, CodegenExpressionRelational.CodegenRelational.GE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LEStringComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            String s1 = (String) objOne;
            String s2 = (String) objTwo;
            return s1.compareTo(s2) <= 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenStringCompare(lhs, lhsType, rhs, rhsType, CodegenExpressionRelational.CodegenRelational.LE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LTStringComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            String s1 = (String) objOne;
            String s2 = (String) objTwo;
            return s1.compareTo(s2) < 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenStringCompare(lhs, lhsType, rhs, rhsType, CodegenExpressionRelational.CodegenRelational.LT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GTLongComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.longValue() > s2.longValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenLong(lhs, lhsType, rhs, rhsType, GT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GELongComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.longValue() >= s2.longValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenLong(lhs, lhsType, rhs, rhsType, GE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LTLongComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.longValue() < s2.longValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenLong(lhs, lhsType, rhs, rhsType, LT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LELongComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.longValue() <= s2.longValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenLong(lhs, lhsType, rhs, rhsType, LE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GTIntegerComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.intValue() > s2.intValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenInt(lhs, lhsType, rhs, rhsType, GT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GEIntegerComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.intValue() >= s2.intValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenInt(lhs, lhsType, rhs, rhsType, GE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LTIntegerComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.intValue() < s2.intValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenInt(lhs, lhsType, rhs, rhsType, LT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LEIntegerComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.intValue() <= s2.intValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenInt(lhs, lhsType, rhs, rhsType, LE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GTDoubleComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.doubleValue() > s2.doubleValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenDouble(lhs, lhsType, rhs, rhsType, GT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GEDoubleComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.doubleValue() >= s2.doubleValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenDouble(lhs, lhsType, rhs, rhsType, GE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LTDoubleComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.doubleValue() < s2.doubleValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenDouble(lhs, lhsType, rhs, rhsType, LT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LEDoubleComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.doubleValue() <= s2.doubleValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenDouble(lhs, lhsType, rhs, rhsType, LE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GTFloatComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.floatValue() > s2.floatValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenFloat(lhs, lhsType, rhs, rhsType, GT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GEFloatComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.floatValue() >= s2.floatValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenFloat(lhs, lhsType, rhs, rhsType, GE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LTFloatComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.floatValue() < s2.floatValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenFloat(lhs, lhsType, rhs, rhsType, LT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LEFloatComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            Number s1 = (Number) objOne;
            Number s2 = (Number) objTwo;
            return s1.floatValue() <= s2.floatValue();
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenFloat(lhs, lhsType, rhs, rhsType, LE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GTBigDecComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            BigDecimal s1 = (BigDecimal) objOne;
            BigDecimal s2 = (BigDecimal) objTwo;
            int result = s1.compareTo(s2);
            return result > 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntBigDecPlain(lhs, rhs, CodegenExpressionRelational.CodegenRelational.GT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GEBigDecComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            BigDecimal s1 = (BigDecimal) objOne;
            BigDecimal s2 = (BigDecimal) objTwo;
            return s1.compareTo(s2) >= 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntBigDecPlain(lhs, rhs, CodegenExpressionRelational.CodegenRelational.GE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LEBigDecComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            BigDecimal s1 = (BigDecimal) objOne;
            BigDecimal s2 = (BigDecimal) objTwo;
            return s1.compareTo(s2) <= 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntBigDecPlain(lhs, rhs, CodegenExpressionRelational.CodegenRelational.LE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LTBigDecComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            BigDecimal s1 = (BigDecimal) objOne;
            BigDecimal s2 = (BigDecimal) objTwo;
            return s1.compareTo(s2) < 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntBigDecPlain(lhs, rhs, CodegenExpressionRelational.CodegenRelational.LT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GTBigIntComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            BigInteger s1 = (BigInteger) objOne;
            BigInteger s2 = (BigInteger) objTwo;
            int result = s1.compareTo(s2);
            return result > 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntBigDecPlain(lhs, rhs, CodegenExpressionRelational.CodegenRelational.GT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GEBigIntComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            BigInteger s1 = (BigInteger) objOne;
            BigInteger s2 = (BigInteger) objTwo;
            return s1.compareTo(s2) >= 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntBigDecPlain(lhs, rhs, CodegenExpressionRelational.CodegenRelational.GE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LEBigIntComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            BigInteger s1 = (BigInteger) objOne;
            BigInteger s2 = (BigInteger) objTwo;
            return s1.compareTo(s2) <= 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntBigDecPlain(lhs, rhs, CodegenExpressionRelational.CodegenRelational.LE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LTBigIntComputer implements Computer {
        public boolean compare(Object objOne, Object objTwo) {
            BigInteger s1 = (BigInteger) objOne;
            BigInteger s2 = (BigInteger) objTwo;
            return s1.compareTo(s2) < 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntBigDecPlain(lhs, rhs, CodegenExpressionRelational.CodegenRelational.LT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GTBigIntConvComputer implements Computer {
        private final SimpleNumberBigIntegerCoercer convOne;
        private final SimpleNumberBigIntegerCoercer convTwo;

        /**
         * Ctor.
         *
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         */
        public GTBigIntConvComputer(SimpleNumberBigIntegerCoercer convOne, SimpleNumberBigIntegerCoercer convTwo) {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public boolean compare(Object objOne, Object objTwo) {
            BigInteger s1 = convOne.coerceBoxedBigInt((Number) objOne);
            BigInteger s2 = convTwo.coerceBoxedBigInt((Number) objTwo);
            int result = s1.compareTo(s2);
            return result > 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntConv(lhs, lhsType, rhs, rhsType, convOne, convTwo, CodegenExpressionRelational.CodegenRelational.GT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GEBigIntConvComputer implements Computer {
        private final SimpleNumberBigIntegerCoercer convOne;
        private final SimpleNumberBigIntegerCoercer convTwo;

        /**
         * Ctor.
         *
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         */
        public GEBigIntConvComputer(SimpleNumberBigIntegerCoercer convOne, SimpleNumberBigIntegerCoercer convTwo) {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public boolean compare(Object objOne, Object objTwo) {
            BigInteger s1 = convOne.coerceBoxedBigInt((Number) objOne);
            BigInteger s2 = convTwo.coerceBoxedBigInt((Number) objTwo);
            return s1.compareTo(s2) >= 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntConv(lhs, lhsType, rhs, rhsType, convOne, convTwo, CodegenExpressionRelational.CodegenRelational.GE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LEBigIntConvComputer implements Computer {
        private final SimpleNumberBigIntegerCoercer convOne;
        private final SimpleNumberBigIntegerCoercer convTwo;

        /**
         * Ctor.
         *
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         */
        public LEBigIntConvComputer(SimpleNumberBigIntegerCoercer convOne, SimpleNumberBigIntegerCoercer convTwo) {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public boolean compare(Object objOne, Object objTwo) {
            BigInteger s1 = convOne.coerceBoxedBigInt((Number) objOne);
            BigInteger s2 = convTwo.coerceBoxedBigInt((Number) objTwo);
            return s1.compareTo(s2) <= 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntConv(lhs, lhsType, rhs, rhsType, convOne, convTwo, CodegenExpressionRelational.CodegenRelational.LE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LTBigIntConvComputer implements Computer {
        private final SimpleNumberBigIntegerCoercer convOne;
        private final SimpleNumberBigIntegerCoercer convTwo;

        /**
         * Ctor.
         *
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         */
        public LTBigIntConvComputer(SimpleNumberBigIntegerCoercer convOne, SimpleNumberBigIntegerCoercer convTwo) {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public boolean compare(Object objOne, Object objTwo) {
            BigInteger s1 = convOne.coerceBoxedBigInt((Number) objOne);
            BigInteger s2 = convTwo.coerceBoxedBigInt((Number) objTwo);
            return s1.compareTo(s2) < 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigIntConv(lhs, lhsType, rhs, rhsType, convOne, convTwo, CodegenExpressionRelational.CodegenRelational.LT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GTBigDecConvComputer implements Computer {
        private final SimpleNumberBigDecimalCoercer convOne;
        private final SimpleNumberBigDecimalCoercer convTwo;

        /**
         * Ctor.
         *
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         */
        public GTBigDecConvComputer(SimpleNumberBigDecimalCoercer convOne, SimpleNumberBigDecimalCoercer convTwo) {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public boolean compare(Object objOne, Object objTwo) {
            BigDecimal s1 = convOne.coerceBoxedBigDec((Number) objOne);
            BigDecimal s2 = convTwo.coerceBoxedBigDec((Number) objTwo);
            int result = s1.compareTo(s2);
            return result > 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigDecConv(lhs, lhsType, rhs, rhsType, convOne, convTwo, CodegenExpressionRelational.CodegenRelational.GT);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class GEBigDecConvComputer implements Computer {
        private final SimpleNumberBigDecimalCoercer convOne;
        private final SimpleNumberBigDecimalCoercer convTwo;

        /**
         * Ctor.
         *
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         */
        public GEBigDecConvComputer(SimpleNumberBigDecimalCoercer convOne, SimpleNumberBigDecimalCoercer convTwo) {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public boolean compare(Object objOne, Object objTwo) {
            BigDecimal s1 = convOne.coerceBoxedBigDec((Number) objOne);
            BigDecimal s2 = convTwo.coerceBoxedBigDec((Number) objTwo);
            return s1.compareTo(s2) >= 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigDecConv(lhs, lhsType, rhs, rhsType, convOne, convTwo, CodegenExpressionRelational.CodegenRelational.GE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LEBigDecConvComputer implements Computer {
        private final SimpleNumberBigDecimalCoercer convOne;
        private final SimpleNumberBigDecimalCoercer convTwo;

        /**
         * Ctor.
         *
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         */
        public LEBigDecConvComputer(SimpleNumberBigDecimalCoercer convOne, SimpleNumberBigDecimalCoercer convTwo) {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public boolean compare(Object objOne, Object objTwo) {
            BigDecimal s1 = convOne.coerceBoxedBigDec((Number) objOne);
            BigDecimal s2 = convTwo.coerceBoxedBigDec((Number) objTwo);
            return s1.compareTo(s2) <= 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigDecConv(lhs, lhsType, rhs, rhsType, convOne, convTwo, CodegenExpressionRelational.CodegenRelational.LE);
        }
    }

    /**
     * Computer for relational op compare.
     */
    public static class LTBigDecConvComputer implements Computer {
        private final SimpleNumberBigDecimalCoercer convOne;
        private final SimpleNumberBigDecimalCoercer convTwo;

        /**
         * Ctor.
         *
         * @param convOne convertor for LHS
         * @param convTwo convertor for RHS
         */
        public LTBigDecConvComputer(SimpleNumberBigDecimalCoercer convOne, SimpleNumberBigDecimalCoercer convTwo) {
            this.convOne = convOne;
            this.convTwo = convTwo;
        }

        public boolean compare(Object objOne, Object objTwo) {
            BigDecimal s1 = convOne.coerceBoxedBigDec((Number) objOne);
            BigDecimal s2 = convTwo.coerceBoxedBigDec((Number) objTwo);
            return s1.compareTo(s2) < 0;
        }

        public CodegenExpression codegen(CodegenExpressionRef lhs, Class lhsType, CodegenExpression rhs, Class rhsType) {
            return codegenBigDecConv(lhs, lhsType, rhs, rhsType, convOne, convTwo, CodegenExpressionRelational.CodegenRelational.LT);
        }
    }

    /**
     * Returns string rendering of enum.
     *
     * @return relational op string
     */
    public String getExpressionText() {
        return expressionText;
    }

    private static CodegenExpression codegenLong(CodegenExpression lhs, Class lhsType, CodegenExpression rhs, Class rhsType, RelationalOpEnum op) {
        return op(MathArithTypeEnum.codegenAsLong(lhs, lhsType), op.getExpressionText(), MathArithTypeEnum.codegenAsLong(rhs, rhsType));
    }

    private static CodegenExpression codegenDouble(CodegenExpression lhs, Class lhsType, CodegenExpression rhs, Class rhsType, RelationalOpEnum op) {
        return op(MathArithTypeEnum.codegenAsDouble(lhs, lhsType), op.getExpressionText(), MathArithTypeEnum.codegenAsDouble(rhs, rhsType));
    }

    private static CodegenExpression codegenFloat(CodegenExpression lhs, Class lhsType, CodegenExpression rhs, Class rhsType, RelationalOpEnum op) {
        return op(MathArithTypeEnum.codegenAsFloat(lhs, lhsType), op.getExpressionText(), MathArithTypeEnum.codegenAsFloat(rhs, rhsType));
    }

    private static CodegenExpression codegenInt(CodegenExpression lhs, Class lhsType, CodegenExpression rhs, Class rhsType, RelationalOpEnum op) {
        return op(MathArithTypeEnum.codegenAsInt(lhs, lhsType), op.getExpressionText(), MathArithTypeEnum.codegenAsInt(rhs, rhsType));
    }

    private static CodegenExpression codegenStringCompare(CodegenExpression lhs, Class lhsType, CodegenExpression rhs, Class rhsType, CodegenExpressionRelational.CodegenRelational rel) {
        return relational(exprDotMethod(codegenAsString(lhs, lhsType), "compareTo", codegenAsString(rhs, rhsType)), rel, constant(0));
    }

    private static CodegenExpression codegenAsString(CodegenExpression ref, Class type) {
        if (type == String.class) {
            return ref;
        }
        return cast(String.class, ref);
    }

    private static CodegenExpression codegenBigIntBigDecPlain(CodegenExpression lhs, CodegenExpression rhs, CodegenExpressionRelational.CodegenRelational rel) {
        return relational(exprDotMethod(lhs, "compareTo", rhs), rel, constant(0));
    }

    private static CodegenExpression codegenBigDecConv(CodegenExpression lhs, Class lhsType, CodegenExpression rhs, Class rhsType, SimpleNumberBigDecimalCoercer convLeft, SimpleNumberBigDecimalCoercer convRight, CodegenExpressionRelational.CodegenRelational rel) {
        CodegenExpression leftConv = convLeft.coerceBoxedBigDecCodegen(lhs, lhsType);
        CodegenExpression rightConv = convRight.coerceBoxedBigDecCodegen(rhs, rhsType);
        return relational(exprDotMethod(leftConv, "compareTo", rightConv), rel, constant(0));
    }

    private static CodegenExpression codegenBigIntConv(CodegenExpression lhs, Class lhsType, CodegenExpression rhs, Class rhsType, SimpleNumberBigIntegerCoercer convLeft, SimpleNumberBigIntegerCoercer convRight, CodegenExpressionRelational.CodegenRelational rel) {
        CodegenExpression leftConv = convLeft.coerceBoxedBigIntCodegen(lhs, lhsType);
        CodegenExpression rightConv = convRight.coerceBoxedBigIntCodegen(rhs, rhsType);
        return relational(exprDotMethod(leftConv, "compareTo", rightConv), rel, constant(0));
    }
}
