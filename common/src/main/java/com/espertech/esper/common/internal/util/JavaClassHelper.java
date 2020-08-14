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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.client.type.*;
import com.espertech.esper.common.client.util.ClassForNameProvider;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiConsumer;

import static com.espertech.esper.common.client.type.EPTypePremade.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Helper for questions about EPType and Classes.
 */
public class JavaClassHelper {

    public final static String APACHE_AVRO_GENERIC_RECORD_CLASSNAME = "org.apache.avro.generic.GenericData$Record";

    public static boolean isImplementsCharSequence(EPType type) {
        if (isNullTypeSafe(type)) {
            return false;
        }
        EPTypeClass typeClass = (EPTypeClass) type;
        if (typeClass.getType() == String.class || typeClass.getType() == CharSequence.class) {
            return true;
        }
        return isSubclassOrImplementsInterface(typeClass, EPTypePremade.CHARSEQUENCE.getEPType());
    }

    public static boolean isArrayTypeCompatible(Class<?> target, Class<?> provided) {
        if (target == provided || target == Object.class) {
            return true;
        }
        Class<?> targetBoxed = getBoxedType(target);
        Class<?> providedBoxed = getBoxedType(provided);
        return targetBoxed == providedBoxed || isSubclassOrImplementsInterface(providedBoxed, targetBoxed);
    }

    public static boolean isCollectionMapOrArray(EPType returnType) {
        if (returnType == null || returnType == EPTypeNull.INSTANCE) {
            return false;
        }
        EPTypeClass rt = (EPTypeClass) returnType;
        return isImplementsInterface(rt, Collection.class) || isImplementsInterface(rt, Map.class) || rt.getType().isArray();
    }

    public static EPType getBoxedType(EPType type) {
        if (type == EPTypeNull.INSTANCE) {
            return type;
        }
        return getBoxedType((EPTypeClass) type);
    }

    public static EPTypeClass getBoxedType(EPTypeClass clazz) {
        if (clazz == null) {
            return null;
        }
        if (!clazz.getType().isPrimitive()) {
            return clazz;
        }
        if (clazz.getType() == boolean.class) {
            return BOOLEANBOXED.getEPType();
        }
        if (clazz.getType() == char.class) {
            return CHARBOXED.getEPType();
        }
        if (clazz.getType() == double.class) {
            return DOUBLEBOXED.getEPType();
        }
        if (clazz.getType() == float.class) {
            return FLOATBOXED.getEPType();
        }
        if (clazz.getType() == byte.class) {
            return BYTEBOXED.getEPType();
        }
        if (clazz.getType() == short.class) {
            return SHORTBOXED.getEPType();
        }
        if (clazz.getType() == int.class) {
            return INTEGERBOXED.getEPType();
        }
        if (clazz.getType() == long.class) {
            return LONGBOXED.getEPType();
        }
        return clazz;
    }

    /**
     * Returns the boxed class for the given class, or the class itself if already boxed or not a primitive type.
     * For primitive unboxed types returns the boxed types, e.g. returns java.lang.Integer for passing int.class.
     * For any other class, returns the class passed.
     *
     * @param clazz is the class to return the boxed class for
     * @return boxed variant of the same class
     */
    public static Class<?> getBoxedType(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        if (!clazz.isPrimitive()) {
            return clazz;
        }
        if (clazz == boolean.class) {
            return Boolean.class;
        }
        if (clazz == char.class) {
            return Character.class;
        }
        if (clazz == double.class) {
            return Double.class;
        }
        if (clazz == float.class) {
            return Float.class;
        }
        if (clazz == byte.class) {
            return Byte.class;
        }
        if (clazz == short.class) {
            return Short.class;
        }
        if (clazz == int.class) {
            return Integer.class;
        }
        if (clazz == long.class) {
            return Long.class;
        }
        if (clazz == void.class) {
            return Void.class;
        }
        return clazz;
    }

    /**
     * Returns the un-boxed class for the given class, or the class itself if already un-boxed or not a primitive type.
     * For primitive boxed types returns the unboxed primitive type, e.g. returns int.class for passing Integer.class.
     * For any other class, returns the class passed.
     *
     * @param clazz is the class to return the unboxed (or primitive) class for
     * @return primitive variant of the same class
     */
    public static EPTypeClass getPrimitiveType(Class<?> clazz) {
        if (clazz == Boolean.class) {
            return BOOLEANPRIMITIVE.getEPType();
        }
        if (clazz == Character.class) {
            return CHARPRIMITIVE.getEPType();
        }
        if (clazz == Double.class) {
            return DOUBLEPRIMITIVE.getEPType();
        }
        if (clazz == Float.class) {
            return FLOATPRIMITIVE.getEPType();
        }
        if (clazz == Byte.class) {
            return BYTEPRIMITIVE.getEPType();
        }
        if (clazz == Short.class) {
            return SHORTPRIMITIVE.getEPType();
        }
        if (clazz == Integer.class) {
            return INTEGERPRIMITIVE.getEPType();
        }
        if (clazz == Long.class) {
            return LONGPRIMITIVE.getEPType();
        }
        return EPTypePremade.getOrCreate(clazz);
    }

    /**
     * Returns the un-boxed class for the given class, or the class itself if already un-boxed or not a primitive type.
     * For primitive boxed types returns the unboxed primitive type, e.g. returns int.class for passing Integer.class.
     * For any other class, returns the class passed.
     *
     * @param clazz is the class to return the unboxed (or primitive) class for
     * @return primitive variant of the same class
     */
    public static EPTypeClass getPrimitiveType(EPTypeClass clazz) {
        if (clazz.getType() == Boolean.class) {
            return EPTypePremade.BOOLEANPRIMITIVE.getEPType();
        }
        if (clazz.getType() == Character.class) {
            return EPTypePremade.CHARPRIMITIVE.getEPType();
        }
        if (clazz.getType() == Double.class) {
            return EPTypePremade.DOUBLEPRIMITIVE.getEPType();
        }
        if (clazz.getType() == Float.class) {
            return EPTypePremade.FLOATPRIMITIVE.getEPType();
        }
        if (clazz.getType() == Byte.class) {
            return EPTypePremade.BYTEPRIMITIVE.getEPType();
        }
        if (clazz.getType() == Short.class) {
            return EPTypePremade.SHORTPRIMITIVE.getEPType();
        }
        if (clazz.getType() == Integer.class) {
            return EPTypePremade.INTEGERPRIMITIVE.getEPType();
        }
        if (clazz.getType() == Long.class) {
            return EPTypePremade.LONGPRIMITIVE.getEPType();
        }
        return clazz;
    }

    public static boolean isNumeric(EPType clazz) {
        if (clazz == null || clazz == EPTypeNull.INSTANCE) {
            return false;
        }
        return isNumeric(((EPTypeClass) clazz).getType());
    }

    /**
     * Determines if the class passed in is one of the numeric classes.
     *
     * @param clazz to check
     * @return true if numeric, false if not
     */
    public static boolean isNumeric(Class<?> clazz) {
        return (clazz == Double.class) ||
            (clazz == double.class) ||
            (clazz == BigDecimal.class) ||
            (clazz == BigInteger.class) ||
            (clazz == Float.class) ||
            (clazz == float.class) ||
            (clazz == Short.class) ||
            (clazz == short.class) ||
            (clazz == Integer.class) ||
            (clazz == int.class) ||
            (clazz == Long.class) ||
            (clazz == long.class) ||
            (clazz == Byte.class) ||
            (clazz == byte.class);
    }

    /**
     * Determines if the type passed in is one of the numeric classes and not a floating point.
     *
     * @param type to check
     * @return true if numeric and not a floating point, false if not
     */
    public static boolean isNumericNonFP(EPType type) {
        if (isNullTypeSafe(type)) {
            return false;
        }
        return isNumericNonFP(((EPTypeClass) type).getType());
    }

    /**
     * Determines if the class passed in is one of the numeric classes and not a floating point.
     *
     * @param clazz to check
     * @return true if numeric and not a floating point, false if not
     */
    public static boolean isNumericNonFP(Class<?> clazz) {
        return (clazz == Short.class) ||
            (clazz == short.class) ||
            (clazz == Integer.class) ||
            (clazz == int.class) ||
            (clazz == Long.class) ||
            (clazz == long.class) ||
            (clazz == Byte.class) ||
            (clazz == byte.class);
    }

    public static boolean isAssignmentCompatible(EPType invocationType, Class<?> declarationType) {
        if (invocationType == null || invocationType == EPTypeNull.INSTANCE) {
            return declarationType == null || !declarationType.isPrimitive();
        }
        return isAssignmentCompatible(((EPTypeClass) invocationType).getType(), declarationType);
    }

    /**
     * Returns true if 2 classes are assignment compatible.
     *
     * @param invocationType  type to assign from
     * @param declarationType type to assign to
     * @return true if assignment compatible, false if not
     */
    public static boolean isAssignmentCompatible(Class<?> invocationType, Class<?> declarationType) {
        if (invocationType == null) {
            return declarationType == null || !declarationType.isPrimitive();
        }
        if (declarationType.isAssignableFrom(invocationType)) {
            return true;
        }

        if (declarationType.isPrimitive()) {
            Class<?> parameterWrapperClazz = getBoxedType(declarationType);
            if (parameterWrapperClazz != null) {
                if (parameterWrapperClazz.equals(invocationType)) {
                    return true;
                }
            }
        }

        if (getBoxedType(invocationType) == declarationType) {
            return true;
        }

        Set<Class> widenings = MethodResolver.getWideningConversions().get(declarationType);
        if (widenings != null) {
            return widenings.contains(invocationType);
        }

        if (declarationType.isInterface()) {
            if (isImplementsInterface(invocationType, declarationType)) {
                return true;
            }
        }

        return recursiveIsSuperClass(invocationType, declarationType);
    }

    /**
     * Determines if the class passed in is a boolean boxed or unboxed type.
     *
     * @param type to check
     * @return true if boolean, false if not
     */
    public static boolean isTypeBoolean(EPType type) {
        if (isNullTypeSafe(type)) {
            return false;
        }
        return isTypeBoolean(((EPTypeClass) type).getType());
    }

    /**
     * Determines if the class passed in is a boolean boxed or unboxed type.
     *
     * @param type to check
     * @return true if boolean, false if not
     */
    public static boolean isTypeInteger(EPType type) {
        if (isNullTypeSafe(type)) {
            return false;
        }
        return isTypeInteger(((EPTypeClass) type).getType());
    }

    /**
     * Determines if the class passed in is a boolean boxed or unboxed type.
     *
     * @param type to check
     * @return true if boolean, false if not
     */
    public static boolean isTypeLong(EPType type) {
        if (isNullTypeSafe(type)) {
            return false;
        }
        return isTypeLong(((EPTypeClass) type).getType());
    }

    /**
     * Determines if the class passed in is a boolean boxed or unboxed type.
     *
     * @param type to check
     * @return true if boolean, false if not
     */
    public static boolean isTypeDouble(EPType type) {
        if (isNullTypeSafe(type)) {
            return false;
        }
        return isTypeDouble(((EPTypeClass) type).getType());
    }

    /**
     * Determines if the class passed in is a boolean boxed or unboxed type.
     *
     * @param type to check
     * @return true if boolean, false if not
     */
    public static boolean isTypeString(EPType type) {
        return isType(type, String.class);
    }

    public static boolean isTypePrimitive(EPType type) {
        if (isNullTypeSafe(type)) {
            return false;
        }
        return ((EPTypeClass) type).getType().isPrimitive();
    }

    public static boolean isType(EPType type, Class<?> clazz) {
        if (isNullTypeSafe(type)) {
            return false;
        }
        return ((EPTypeClass) type).getType() == clazz;
    }

    /**
     * Determines if the class passed in is a boolean boxed or unboxed type.
     *
     * @param clazz to check
     * @return true if boolean, false if not
     */
    public static boolean isTypeBoolean(Class<?> clazz) {
        return (clazz == Boolean.class) ||
            (clazz == boolean.class);
    }

    /**
     * Determines if the class passed in is a boolean boxed or unboxed type.
     *
     * @param clazz to check
     * @return true if boolean, false if not
     */
    public static boolean isTypeInteger(Class<?> clazz) {
        return (clazz == Integer.class) ||
            (clazz == int.class);
    }

    /**
     * Determines if the class passed in is a boolean boxed or unboxed type.
     *
     * @param clazz to check
     * @return true if boolean, false if not
     */
    public static boolean isTypeLong(Class<?> clazz) {
        return (clazz == Long.class) ||
            (clazz == long.class);
    }

    /**
     * Determines if the class passed in is a boolean boxed or unboxed type.
     *
     * @param clazz to check
     * @return true if boolean, false if not
     */
    public static boolean isTypeDouble(Class<?> clazz) {
        return (clazz == Double.class) ||
            (clazz == double.class);
    }

    /**
     * Returns the coercion type for the 2 numeric types for use in arithmatic.
     * Note: byte and short types always result in integer.
     *
     * @param typeOne is the first type
     * @param typeTwo is the second type
     * @return coerced type
     * @throws CoercionException if types don't allow coercion
     */
    public static EPTypeClass getArithmaticCoercionType(EPTypeClass typeOne, EPTypeClass typeTwo)
        throws CoercionException {
        EPTypeClass boxedOneType = getBoxedType(typeOne);
        EPTypeClass boxedTwoType = getBoxedType(typeTwo);
        Class<?> boxedOne = boxedOneType.getType();
        Class<?> boxedTwo = boxedTwoType.getType();

        if (!isNumeric(boxedOneType) || !isNumeric(boxedTwoType)) {
            throw new CoercionException("Cannot coerce types " + typeOne.getTypeName() + " and " + typeTwo.getTypeName());
        }
        if (boxedOne == boxedTwo) {
            return boxedOneType;
        }
        if ((boxedOne == BigDecimal.class) || (boxedTwo == BigDecimal.class)) {
            return BIGDECIMAL.getEPType();
        }
        if (((boxedOne == BigInteger.class) && JavaClassHelper.isFloatingPointClass(boxedTwo)) ||
            ((boxedTwo == BigInteger.class) && JavaClassHelper.isFloatingPointClass(boxedOne))) {
            return BIGDECIMAL.getEPType();
        }
        if ((boxedOne == BigInteger.class) || (boxedTwo == BigInteger.class)) {
            return BIGINTEGER.getEPType();
        }
        if ((boxedOne == Double.class) || (boxedTwo == Double.class)) {
            return DOUBLEBOXED.getEPType();
        }
        if ((boxedOne == Float.class) && (!isFloatingPointClass(typeTwo))) {
            return DOUBLEBOXED.getEPType();
        }
        if ((boxedTwo == Float.class) && (!isFloatingPointClass(typeOne))) {
            return DOUBLEBOXED.getEPType();
        }
        if ((boxedOne == Long.class) || (boxedTwo == Long.class)) {
            return LONGBOXED.getEPType();
        }
        return INTEGERBOXED.getEPType();
    }

    /**
     * Returns the coercion type for the 2 numeric types for use in arithmatic.
     * Note: byte and short types always result in integer.
     *
     * @param typeOne is the first type
     * @param typeTwo is the second type
     * @return coerced type
     * @throws CoercionException if types don't allow coercion
     */
    public static Class<?> getArithmaticCoercionType(Class<?> typeOne, Class<?> typeTwo)
        throws CoercionException {
        Class<?> boxedOne = getBoxedType(typeOne);
        Class<?> boxedTwo = getBoxedType(typeTwo);

        if (!isNumeric(boxedOne) || !isNumeric(boxedTwo)) {
            throw new CoercionException("Cannot coerce types " + typeOne.getName() + " and " + typeTwo.getName());
        }
        if (boxedOne == boxedTwo) {
            return boxedOne;
        }
        if ((boxedOne == BigDecimal.class) || (boxedTwo == BigDecimal.class)) {
            return BigDecimal.class;
        }
        if (((boxedOne == BigInteger.class) && JavaClassHelper.isFloatingPointClass(boxedTwo)) ||
            ((boxedTwo == BigInteger.class) && JavaClassHelper.isFloatingPointClass(boxedOne))) {
            return BigDecimal.class;
        }
        if ((boxedOne == BigInteger.class) || (boxedTwo == BigInteger.class)) {
            return BigInteger.class;
        }
        if ((boxedOne == Double.class) || (boxedTwo == Double.class)) {
            return Double.class;
        }
        if ((boxedOne == Float.class) && (!isFloatingPointClass(typeTwo))) {
            return Double.class;
        }
        if ((boxedTwo == Float.class) && (!isFloatingPointClass(typeOne))) {
            return Double.class;
        }
        if ((boxedOne == Long.class) || (boxedTwo == Long.class)) {
            return Long.class;
        }
        return Integer.class;
    }

    /**
     * Coerce the given number to the given type, assuming the type is a Boxed type. Allows coerce to lower resultion number.
     * Does't coerce to primitive types.
     * <p>
     * Meant for statement compile-time use, not for runtime use.
     *
     * @param numToCoerce     is the number to coerce to the given type
     * @param resultBoxedType is the boxed result type to return
     * @return the numToCoerce as a value in the given result type
     */
    public static Number coerceBoxed(Number numToCoerce, Class<?> resultBoxedType) {
        if (numToCoerce.getClass() == resultBoxedType) {
            return numToCoerce;
        }
        if (resultBoxedType == Double.class) {
            return numToCoerce.doubleValue();
        }
        if (resultBoxedType == Long.class) {
            return numToCoerce.longValue();
        }
        if (resultBoxedType == BigInteger.class) {
            return BigInteger.valueOf(numToCoerce.longValue());
        }
        if (resultBoxedType == BigDecimal.class) {
            if (JavaClassHelper.isFloatingPointNumber(numToCoerce)) {
                return new BigDecimal(numToCoerce.doubleValue());
            }
            return new BigDecimal(numToCoerce.longValue());
        }
        if (resultBoxedType == Float.class) {
            return numToCoerce.floatValue();
        }
        if (resultBoxedType == Integer.class) {
            return numToCoerce.intValue();
        }
        if (resultBoxedType == Short.class) {
            return numToCoerce.shortValue();
        }
        if (resultBoxedType == Byte.class) {
            return numToCoerce.byteValue();
        }
        throw new IllegalArgumentException("Cannot coerce to number subtype " + resultBoxedType.getName());
    }

    public static CodegenExpression coerceNumberBoxedToBoxedCodegen(CodegenExpression exprReturningBoxed, EPTypeClass fromTypeBoxed, EPTypeClass targetTypeBoxed) {
        if (fromTypeBoxed.equals(targetTypeBoxed)) {
            return exprReturningBoxed;
        }
        if (targetTypeBoxed.getType() == Double.class) {
            return exprDotMethod(exprReturningBoxed, "doubleValue");
        }
        if (targetTypeBoxed.getType() == Long.class) {
            return exprDotMethod(exprReturningBoxed, "longValue");
        }
        if (targetTypeBoxed.getType() == BigInteger.class) {
            return staticMethod(BigInteger.class, "valueOf", exprDotMethod(exprReturningBoxed, "longValue"));
        }
        if (targetTypeBoxed.getType() == BigDecimal.class) {
            if (JavaClassHelper.isFloatingPointClass(fromTypeBoxed)) {
                return newInstance(BIGDECIMAL.getEPType(), exprDotMethod(exprReturningBoxed, "doubleValue"));
            }
            return newInstance(BIGDECIMAL.getEPType(), exprDotMethod(exprReturningBoxed, "longValue"));
        }
        if (targetTypeBoxed.getType() == Float.class) {
            return exprDotMethod(exprReturningBoxed, "floatValue");
        }
        if (targetTypeBoxed.getType() == Integer.class) {
            return exprDotMethod(exprReturningBoxed, "intValue");
        }
        if (targetTypeBoxed.getType() == Short.class) {
            return exprDotMethod(exprReturningBoxed, "shortValue");
        }
        if (targetTypeBoxed.getType() == Byte.class) {
            return exprDotMethod(exprReturningBoxed, "byteValue");
        }
        throw new IllegalArgumentException("Cannot coerce to number subtype " + fromTypeBoxed.getType().getName());
    }

    public static CodegenExpression coerceNumberToBoxedCodegen(CodegenExpression expr, EPTypeClass fromType, EPTypeClass targetTypeBoxed) {
        if (!fromType.getType().isPrimitive()) {
            return coerceNumberBoxedToBoxedCodegen(expr, fromType, targetTypeBoxed);
        }
        if (targetTypeBoxed.getType() == Double.class) {
            return coerceAnyToBoxedCodegenValueOf(expr, fromType, DOUBLEBOXED.getEPType(), DOUBLEPRIMITIVE.getEPType());
        }
        if (targetTypeBoxed.getType() == Long.class) {
            return coerceAnyToBoxedCodegenValueOf(expr, fromType, LONGBOXED.getEPType(), LONGPRIMITIVE.getEPType());
        }
        if (targetTypeBoxed.getType() == BigInteger.class) {
            return staticMethod(BigInteger.class, "valueOf", coerceAnyToBoxedCodegenValueOf(expr, fromType, LONGBOXED.getEPType(), LONGPRIMITIVE.getEPType()));
        }
        if (targetTypeBoxed.getType() == BigDecimal.class) {
            if (JavaClassHelper.isFloatingPointClass(fromType)) {
                return newInstance(BIGDECIMAL.getEPType(), exprDotMethod(coerceAnyToBoxedCodegenValueOf(expr, fromType, DOUBLEBOXED.getEPType(), DOUBLEPRIMITIVE.getEPType()), "doubleValue"));
            }
            return newInstance(BIGDECIMAL.getEPType(), exprDotMethod(coerceAnyToBoxedCodegenValueOf(expr, fromType, LONGBOXED.getEPType(), LONGPRIMITIVE.getEPType()), "longValue"));
        }
        if (targetTypeBoxed.getType() == Float.class) {
            return coerceAnyToBoxedCodegenValueOf(expr, fromType, BYTEBOXED.getEPType(), BYTEPRIMITIVE.getEPType());
        }
        if (targetTypeBoxed.getType() == Integer.class) {
            return coerceAnyToBoxedCodegenValueOf(expr, fromType, INTEGERBOXED.getEPType(), INTEGERPRIMITIVE.getEPType());
        }
        if (targetTypeBoxed.getType() == Short.class) {
            return coerceAnyToBoxedCodegenValueOf(expr, fromType, SHORTBOXED.getEPType(), SHORTPRIMITIVE.getEPType());
        }
        if (targetTypeBoxed.getType() == Byte.class) {
            return coerceAnyToBoxedCodegenValueOf(expr, fromType, BYTEBOXED.getEPType(), BYTEPRIMITIVE.getEPType());
        }
        throw new IllegalArgumentException("Cannot coerce to number subtype " + targetTypeBoxed);
    }

    private static CodegenExpression coerceAnyToBoxedCodegenValueOf(CodegenExpression expr, EPTypeClass from, EPTypeClass boxed, EPTypeClass cast) {
        if (from == cast) {
            return staticMethod(boxed.getType(), "valueOf", expr);
        }
        return staticMethod(boxed.getType(), "valueOf", cast(cast, expr));
    }

    /**
     * Returns true if the Number instance is a floating point number.
     *
     * @param number to check
     * @return true if number is Float or Double type
     */
    public static boolean isFloatingPointNumber(Number number) {
        return (number instanceof Float) ||
            (number instanceof Double);
    }

    /**
     * Returns true if the supplied type is a floating point number.
     *
     * @param type to check
     * @return true if primitive or boxed float or double
     */
    public static boolean isFloatingPointClass(EPType type) {
        if (isNullTypeSafe(type)) {
            return false;
        }
        return isFloatingPointClass(((EPTypeClass) type).getType());
    }

    /**
     * Returns true if the supplied type is a floating point number.
     *
     * @param clazz to check
     * @return true if primitive or boxed float or double
     */
    public static boolean isFloatingPointClass(Class<?> clazz) {
        return (clazz == Float.class) ||
            (clazz == Double.class) ||
            (clazz == float.class) ||
            (clazz == double.class);
    }

    /**
     * Returns for 2 classes to be compared via relational operator the Class type of
     * common comparison. The output is always Long.class, Double.class, String.class or Boolean.class
     * depending on whether the passed types are numeric and floating-point.
     * Accepts primitive as well as boxed types.
     *
     * @param typeOne is the first type
     * @param typeTwo is the second type
     * @return One of Long.class, Double.class or String.class
     * @throws CoercionException if the types cannot be compared
     */
    public static EPTypeClass getCompareToCoercionType(EPType typeOne, EPType typeTwo) throws CoercionException {
        if (typeOne == null || typeOne == EPTypeNull.INSTANCE) {
            if (typeTwo == null || typeTwo == EPTypeNull.INSTANCE) {
                return null;
            }
            return (EPTypeClass) typeTwo;
        }
        if (typeTwo == null || typeTwo == EPTypeNull.INSTANCE) {
            return (EPTypeClass) typeOne;
        }
        EPTypeClass typeClassOne = (EPTypeClass) typeOne;
        EPTypeClass typeClassTwo = (EPTypeClass) typeTwo;
        Class<?> classOne = typeClassOne.getType();
        Class<?> classTwo = typeClassTwo.getType();
        if ((classOne == String.class) && (classTwo == String.class)) {
            return STRING.getEPType();
        }
        if (((classOne == boolean.class) || ((classOne == Boolean.class))) &&
            ((classTwo == boolean.class) || ((classTwo == Boolean.class)))) {
            return BOOLEANBOXED.getEPType();
        }
        if (!isJavaBuiltinDataType(classOne) && (!isJavaBuiltinDataType(classTwo))) {
            if (classOne != classTwo) {
                return OBJECT.getEPType();
            }
            return typeClassOne;
        }
        if (classOne == null) {
            return typeClassTwo;
        }
        if (classTwo == null) {
            return typeClassOne;
        }
        if (!isNumeric(classOne) || !isNumeric(classTwo)) {
            String classOneName = classOne.getName();
            String classTwoName = classTwo.getName();
            throw new CoercionException("Types cannot be compared: " + classOneName + " and " + classTwoName);
        }
        return getArithmaticCoercionType(typeClassOne, typeClassTwo);
    }

    /**
     * Returns true if the type is one of the big number types, i.e. BigDecimal or BigInteger
     *
     * @param clazz to check
     * @return true for big number
     */
    static boolean isBigNumberType(Class<?> clazz) {
        return (clazz == BigInteger.class) || (clazz == BigDecimal.class);
    }

    /**
     * Determines if a number can be coerced upwards to another number class without loss.
     * <p>
     * Clients must pass in two classes that are numeric types.
     * <p>
     * Any number class can be coerced to double, while only double cannot be coerced to float.
     * Any non-floating point number can be coerced to long.
     * Integer can be coerced to Byte and Short even though loss is possible, for convenience.
     *
     * @param numberClassToBeCoerced the number class to be coerced
     * @param numberClassToCoerceTo  the number class to coerce to
     * @return true if numbers can be coerced without loss, false if not
     */
    public static boolean canCoerce(Class<?> numberClassToBeCoerced, Class<?> numberClassToCoerceTo) {
        Class<?> boxedFrom = getBoxedType(numberClassToBeCoerced);
        Class<?> boxedTo = getBoxedType(numberClassToCoerceTo);

        if (!isNumeric(numberClassToBeCoerced)) {
            throw new IllegalArgumentException("Class '" + numberClassToBeCoerced + "' is not a numeric type'");
        }

        if (boxedTo == Float.class) {
            return (boxedFrom == Byte.class) ||
                (boxedFrom == Short.class) ||
                (boxedFrom == Integer.class) ||
                (boxedFrom == Long.class) ||
                (boxedFrom == Float.class);
        } else if (boxedTo == Double.class) {
            return (boxedFrom == Byte.class) ||
                (boxedFrom == Short.class) ||
                (boxedFrom == Integer.class) ||
                (boxedFrom == Long.class) ||
                (boxedFrom == Float.class) ||
                (boxedFrom == Double.class);
        } else if (boxedTo == BigDecimal.class) {
            return (boxedFrom == Byte.class) ||
                (boxedFrom == Short.class) ||
                (boxedFrom == Integer.class) ||
                (boxedFrom == Long.class) ||
                (boxedFrom == Float.class) ||
                (boxedFrom == Double.class) ||
                (boxedFrom == BigInteger.class) ||
                (boxedFrom == BigDecimal.class);
        } else if (boxedTo == BigInteger.class) {
            return (boxedFrom == Byte.class) ||
                (boxedFrom == Short.class) ||
                (boxedFrom == Integer.class) ||
                (boxedFrom == Long.class) ||
                (boxedFrom == BigInteger.class);
        } else if (boxedTo == Long.class) {
            return (boxedFrom == Byte.class) ||
                (boxedFrom == Short.class) ||
                (boxedFrom == Integer.class) ||
                (boxedFrom == Long.class);
        } else if ((boxedTo == Integer.class) ||
            (boxedTo == Short.class) ||
            (boxedTo == Byte.class)) {
            return (boxedFrom == Byte.class) ||
                (boxedFrom == Short.class) ||
                (boxedFrom == Integer.class);
        } else {
            throw new IllegalArgumentException("Class '" + numberClassToCoerceTo + "' is not a numeric type'");
        }
    }

    /**
     * Returns for the class name given the class name of the boxed (wrapped) type if
     * the class name is one of the Java primitive types.
     *
     * @param className is a class name, a Java primitive type or other class
     * @return boxed class name if Java primitive type, or just same class name passed in if not a primitive type
     */
    public static String getBoxedClassName(String className) {
        if (className.equals(char.class.getName())) {
            return Character.class.getName();
        }
        if (className.equals(byte.class.getName())) {
            return Byte.class.getName();
        }
        if (className.equals(short.class.getName())) {
            return Short.class.getName();
        }
        if (className.equals(int.class.getName())) {
            return Integer.class.getName();
        }
        if (className.equals(long.class.getName())) {
            return Long.class.getName();
        }
        if (className.equals(float.class.getName())) {
            return Float.class.getName();
        }
        if (className.equals(double.class.getName())) {
            return Double.class.getName();
        }
        if (className.equals(boolean.class.getName())) {
            return Boolean.class.getName();
        }
        return className;
    }

    /**
     * Returns true if the class passed in is a Java built-in data type (primitive or wrapper) including String and 'null'.
     *
     * @param type to check
     * @return true if built-in data type, or false if not
     */
    public static boolean isJavaBuiltinDataType(EPType type) {
        if (type == null || type == EPTypeNull.INSTANCE) {
            return true;
        }
        return isJavaBuiltinDataType(((EPTypeClass) type).getType());
    }

    /**
     * Returns true if the class passed in is a Java built-in data type (primitive or wrapper) including String and 'null'.
     *
     * @param clazz to check
     * @return true if built-in data type, or false if not
     */
    public static boolean isJavaBuiltinDataType(Class<?> clazz) {
        if (clazz == null) {
            return true;
        }
        if (clazz.isArray()) {
            return isJavaBuiltinDataType(clazz.getComponentType());
        }
        Class<?> clazzBoxed = getBoxedType(clazz);
        if (isNumeric(clazzBoxed)) {
            return true;
        }
        if (isTypeBoolean(clazzBoxed)) {
            return true;
        }
        if (clazzBoxed.equals(String.class)) {
            return true;
        }
        if (clazzBoxed.equals(CharSequence.class)) {
            return true;
        }
        if ((clazzBoxed.equals(char.class)) ||
            (clazzBoxed.equals(Character.class))) {
            return true;
        }
        return clazzBoxed.equals(void.class);
    }

    /**
     * Determines a common denominator type to which one or more types can be casted or coerced.
     * For use in determining the result type in certain expressions (coalesce, case).
     * <p>
     * Null values are allowed as part of the input and indicate a 'null' constant value
     * in an expression tree. Such as value doesn't have any type and can be ignored in
     * determining a result type.
     * <p>
     * For numeric types, determines a coercion type that all types can be converted to
     * via the method getArithmaticCoercionType.
     * <p>
     * Indicates that there is no common denominator type by throwing {@link CoercionException}.
     *
     * @param types is an array of one or more types, which can be Java built-in (primitive or wrapper)
     *              or user types
     * @return common denominator type if any can be found, for use in comparison
     * @throws CoercionException when no coercion type could be determined
     */
    public static EPType getCommonCoercionType(EPType[] types)
        throws CoercionException {
        if (types.length < 1) {
            throw new IllegalArgumentException("Unexpected zero length array");
        }
        if (types.length == 1) {
            return getBoxedType(types[0]);
        }

        // Reduce to non-null types
        List<EPTypeClass> nonNullTypes = new ArrayList<>();
        for (EPType type : types) {
            if (type != null && type != EPTypeNull.INSTANCE) {
                nonNullTypes.add((EPTypeClass) type);
            }
        }
        EPTypeClass[] classes = nonNullTypes.toArray(new EPTypeClass[0]);

        if (classes.length == 0) {
            return EPTypeNull.INSTANCE;    // only null types, result is null
        }
        if (classes.length == 1) {
            return getBoxedType(classes[0]);
        }

        // Check if all String
        if (classes[0].getType() == String.class) {
            for (EPTypeClass clazz : classes) {
                if (clazz.getType() != String.class) {
                    throw new CoercionException("Cannot coerce to String type " + clazz.getTypeName());
                }
            }
            return EPTypePremade.STRING.getEPType();
        }

        // Convert to boxed classes
        for (int i = 0; i < classes.length; i++) {
            classes[i] = getBoxedType(classes[i]);
        }

        // Check if all boolean
        if (classes[0].getType() == Boolean.class) {
            for (EPTypeClass clazz : classes) {
                if (clazz.getType() != Boolean.class) {
                    throw new CoercionException("Cannot coerce to Boolean type " + clazz.getTypeName());
                }
            }
            return EPTypePremade.BOOLEANBOXED.getEPType();
        }

        // Check if all char
        if (classes[0].getType() == Character.class) {
            for (EPTypeClass type : classes) {
                if (type.getType() != Character.class) {
                    throw new CoercionException("Cannot coerce to Boolean type " + type.getTypeName());
                }
            }
            return EPTypePremade.CHARBOXED.getEPType();
        }

        // handle arrays
        if (classes[0].getType().isArray()) {
            EPTypeClass componentType = getArrayComponentType(classes[0]);
            boolean sameComponentType = true;
            for (int i = 1; i < classes.length; i++) {
                if (!classes[i].getType().isArray()) {
                    throw getCoercionException(classes[0], classes[i]);
                }
                EPTypeClass otherComponentType = getArrayComponentType(classes[i]);
                if (!(componentType.equals(otherComponentType))) {
                    if (componentType.getType().isPrimitive() || otherComponentType.getType().isPrimitive()) {
                        throw getCoercionException(classes[0], classes[i]);
                    }
                    sameComponentType = false;
                }
            }
            if (sameComponentType) {
                return classes[0];
            }
            return EPTypePremade.OBJECTARRAY.getEPType();
        }

        // Check if all the same non-Java builtin type, i.e. Java beans etc.
        boolean isAllBuiltinTypes = true;
        for (EPTypeClass type : classes) {
            if (!isNumeric(type) && (!isJavaBuiltinDataType(type.getType()))) {
                isAllBuiltinTypes = false;
            }
        }

        if (isAllBuiltinTypes) {
            // Use arithmatic coercion type as the final authority, considering all classes
            EPTypeClass result = getArithmaticCoercionType(classes[0], classes[1]);
            int count = 2;
            while (count < classes.length) {
                result = getArithmaticCoercionType(result, classes[count]);
                count++;
            }
            return result;
        }

        // handle all non-built-in types

        // first we check if all types are the same or are built-in
        boolean allSame = true;
        for (EPTypeClass type : classes) {
            if (classes[0].equals(type)) {
                continue;
            }
            if (isJavaBuiltinDataType(type.getType())) {
                throw getCoercionException(classes[0], type);
            }
            allSame = false;
        }
        if (allSame) {
            return classes[0];
        }

        // next we check if the unparameterized types are the same
        Class<?> unparameterized = classes[0].getType();
        boolean unparameterizedSame = true;
        for (EPTypeClass type : classes) {
            if (type.getType() == unparameterized) {
                continue;
            }
            unparameterizedSame = false;
        }
        if (unparameterizedSame) {
            return EPTypePremade.getOrCreate(unparameterized);
        }

        return EPTypePremade.OBJECT.getEPType();
    }

    /**
     * Returns the class given a fully-qualified class name.
     *
     * @param className            is the fully-qualified class name, java primitive types included.
     * @param classForNameProvider lookup of class for class name
     * @return class for name
     * @throws ClassNotFoundException if the class cannot be found
     */
    public static Class<?> getClassForName(String className, ClassForNameProvider classForNameProvider) throws ClassNotFoundException {
        if (className.equals(boolean.class.getName())) {
            return boolean.class;
        }
        if (className.equals(char.class.getName())) {
            return char.class;
        }
        if (className.equals(double.class.getName())) {
            return double.class;
        }
        if (className.equals(float.class.getName())) {
            return float.class;
        }
        if (className.equals(byte.class.getName())) {
            return byte.class;
        }
        if (className.equals(short.class.getName())) {
            return short.class;
        }
        if (className.equals(int.class.getName())) {
            return int.class;
        }
        if (className.equals(long.class.getName())) {
            return long.class;
        }
        return classForNameProvider.classForName(className);
    }

    /**
     * Returns the boxed class for the given classname, recognizing all primitive and abbreviations,
     * uppercase and lowercase.
     * <p>
     * Recognizes "int" as Integer.class and "strIng" as String.class, and "Integer" as Integer.class, and so on.
     *
     * @param className            is the name to recognize
     * @param classForNameProvider lookup of class for class name
     * @return class
     */
    public static Class<?> getClassForSimpleName(String className, ClassForNameProvider classForNameProvider) {
        if (("string".equals(className.toLowerCase(Locale.ENGLISH).trim())) ||
            ("varchar".equals(className.toLowerCase(Locale.ENGLISH).trim())) ||
            ("varchar2".equals(className.toLowerCase(Locale.ENGLISH).trim()))) {
            return String.class;
        }

        if (("integer".equals(className.toLowerCase(Locale.ENGLISH).trim())) ||
            ("int".equals(className.toLowerCase(Locale.ENGLISH).trim()))) {
            return Integer.class;
        }

        if ("bool".equals(className.toLowerCase(Locale.ENGLISH).trim())) {
            return Boolean.class;
        }

        if ("character".equals(className.toLowerCase(Locale.ENGLISH).trim())) {
            return Character.class;
        }

        // use the boxed type for primitives
        String boxedClassName = JavaClassHelper.getBoxedClassName(className.trim());

        try {
            return classForNameProvider.classForName(boxedClassName);
        } catch (ClassNotFoundException ex) {
            // expected
        }

        boxedClassName = JavaClassHelper.getBoxedClassName(className.toLowerCase(Locale.ENGLISH).trim());
        try {
            return classForNameProvider.classForName(boxedClassName);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    public static String getSimpleNameForClass(EPTypeClass clazz) {
        if (clazz == null) {
            return "(null)";
        }
        return getSimpleNameForClass(clazz.getType());
    }

    public static String getSimpleNameForClass(Class<?> clazz) {
        if (clazz == null) {
            return "(null)";
        }
        if (JavaClassHelper.isImplementsInterface(clazz, CharSequence.class)) {
            return "string";
        }
        Class<?> boxed = JavaClassHelper.getBoxedType(clazz);
        if (boxed == Integer.class) {
            return "int";
        }
        if (boxed == Boolean.class) {
            return "boolean";
        }
        if (boxed == Character.class) {
            return "character";
        }
        if (boxed == Double.class) {
            return "double";
        }
        if (boxed == Float.class) {
            return "float";
        }
        if (boxed == Byte.class) {
            return "byte";
        }
        if (boxed == Short.class) {
            return "short";
        }
        if (boxed == Long.class) {
            return "long";
        }
        return clazz.getSimpleName();
    }

    /**
     * Returns the class for a Java primitive type name, ignoring case, and considering String as a primitive.
     *
     * @param typeName is a potential primitive Java type, or some other type name
     * @return class for primitive type name, or null if not a primitive type.
     */
    public static Class<?> getPrimitiveClassForName(String typeName) {
        typeName = typeName.toLowerCase(Locale.ENGLISH);
        if (typeName.equals("boolean")) {
            return boolean.class;
        }
        if (typeName.equals("char")) {
            return char.class;
        }
        if (typeName.equals("double")) {
            return double.class;
        }
        if (typeName.equals("float")) {
            return float.class;
        }
        if (typeName.equals("byte")) {
            return byte.class;
        }
        if (typeName.equals("short")) {
            return short.class;
        }
        if (typeName.equals("int")) {
            return int.class;
        }
        if (typeName.equals("long")) {
            return long.class;
        }
        if (typeName.equals("string")) {
            return String.class;
        }
        return null;
    }

    /**
     * Parse the String using the given Java built-in class for parsing.
     *
     * @param clazz is the class to parse the value to
     * @param text  is the text to parse
     * @return value matching the type passed in
     */
    public static Object parse(Class<?> clazz, String text) {
        Class<?> classBoxed = JavaClassHelper.getBoxedType(clazz);

        if (classBoxed == String.class) {
            return text;
        }
        if (classBoxed == Character.class) {
            return text.charAt(0);
        }
        if (classBoxed == Boolean.class) {
            return BoolValue.parseString(text.toLowerCase(Locale.ENGLISH).trim());
        }
        if (classBoxed == Byte.class) {
            return Byte.decode(text.trim());
        }
        if (classBoxed == Short.class) {
            return Short.parseShort(text.trim());
        }
        if (classBoxed == Long.class) {
            return LongValue.parseString(text.trim());
        }
        if (classBoxed == Float.class) {
            return Float.parseFloat(text.trim());
        }
        if (classBoxed == Double.class) {
            return Double.parseDouble(text.trim());
        }
        if (classBoxed == Integer.class) {
            return Integer.parseInt(text.trim());
        }
        return null;
    }

    /**
     * Method to check if a given class, and its superclasses and interfaces (deep), implement a given interface.
     *
     * @param type           to check, including all its superclasses and their interfaces and extends
     * @param interfaceClass is the interface class to look for
     * @return true if such interface is implemented by any of the clazz or its superclasses or
     * extends by any interface and superclasses (deep check)
     */
    public static boolean isImplementsInterface(EPType type, Class<?> interfaceClass) {
        if (type == null || type == EPTypeNull.INSTANCE) {
            return false;
        }
        return isImplementsInterface(((EPTypeClass) type).getType(), interfaceClass);
    }

    /**
     * Method to check if a given class, and its superclasses and interfaces (deep), implement a given interface.
     *
     * @param clazz          to check, including all its superclasses and their interfaces and extends
     * @param interfaceClass is the interface class to look for
     * @return true if such interface is implemented by any of the clazz or its superclasses or
     * extends by any interface and superclasses (deep check)
     */
    public static boolean isImplementsInterface(Class<?> clazz, Class<?> interfaceClass) {
        if (!(interfaceClass.isInterface())) {
            throw new IllegalArgumentException("Interface class passed in is not an interface");
        }
        boolean resultThisClass = recursiveIsImplementsInterface(clazz, interfaceClass);
        if (resultThisClass) {
            return true;
        }
        return recursiveSuperclassImplementsInterface(clazz, interfaceClass);
    }

    public static boolean isSubclassOrImplementsInterface(EPType extendorOrImplementor, EPTypeClass extendedOrImplemented) {
        if (isNullTypeSafe(extendorOrImplementor)) {
            return false;
        }
        return isSubclassOrImplementsInterface(((EPTypeClass) extendorOrImplementor).getType(), extendedOrImplemented.getType());
    }

    public static boolean isSubclassOrImplementsInterface(EPType extendorOrImplementor, Class<?> extendedOrImplemented) {
        if (isNullTypeSafe(extendorOrImplementor)) {
            return false;
        }
        return isSubclassOrImplementsInterface(((EPTypeClass) extendorOrImplementor).getType(), extendedOrImplemented);
    }

    /**
     * Method to check if a given class, and its superclasses and interfaces (deep), implement a given interface or extend a given class.
     *
     * @param extendorOrImplementor is the class to inspect its extends and implements clauses
     * @param extendedOrImplemented is the potential interface, or superclass, to check
     * @return true if such interface is implemented by any of the clazz or its superclasses or
     * extends by any interface and superclasses (deep check)
     */
    public static boolean isSubclassOrImplementsInterface(Class<?> extendorOrImplementor, Class<?> extendedOrImplemented) {
        if (extendorOrImplementor == null) {
            return false;
        }
        if (extendorOrImplementor.equals(extendedOrImplemented)) {
            return true;
        }
        if (extendedOrImplemented.isArray()) {
            if (!extendorOrImplementor.isArray()) {
                return false;
            }
            if (extendorOrImplementor.getComponentType().isPrimitive()) {
                return false;
            }
            if (extendedOrImplemented == Object[].class) {
                return true;
            }

            Class<?> extendorComponentType = getArrayComponentTypeInnermost(extendorOrImplementor);
            Class<?> extendedComponentType = getArrayComponentTypeInnermost(extendedOrImplemented);
            boolean subclass = isSubclassOrImplementsInterface(extendorComponentType, extendedComponentType);
            if (!subclass) {
                return false;
            }
            int dimFrom = getArrayDimensions(extendorOrImplementor);
            int dimTo = getArrayDimensions(extendedOrImplemented);
            if (dimFrom == dimTo) {
                return true;
            }
            if (dimFrom < dimTo) {
                return false;
            }
            return getArrayComponentTypeInnermost(extendedOrImplemented) == Object.class;
        }
        if (extendedOrImplemented.isInterface()) {
            return recursiveIsImplementsInterface(extendorOrImplementor, extendedOrImplemented) ||
                recursiveSuperclassImplementsInterface(extendorOrImplementor, extendedOrImplemented);
        }
        return recursiveIsSuperClass(extendorOrImplementor, extendedOrImplemented);
    }

    private static boolean recursiveIsSuperClass(Class<?> clazz, Class<?> superClass) {
        if (clazz == null) {
            return false;
        }
        if (clazz.isPrimitive()) {
            return false;
        }
        if (superClass == java.lang.Object.class) {
            return true;
        }
        Class<?> mySuperClass = clazz.getSuperclass();
        if (mySuperClass == superClass) {
            return true;
        }
        if (mySuperClass == Object.class) {
            return false;
        }
        return recursiveIsSuperClass(mySuperClass, superClass);
    }

    private static boolean recursiveSuperclassImplementsInterface(Class<?> clazz, Class<?> interfaceClass) {
        Class<?> superClass = clazz.getSuperclass();
        if ((superClass == null) || (superClass == Object.class)) {
            return false;
        }
        boolean result = recursiveIsImplementsInterface(superClass, interfaceClass);
        if (result) {
            return result;
        }
        return recursiveSuperclassImplementsInterface(superClass, interfaceClass);
    }

    private static boolean recursiveIsImplementsInterface(Class<?> clazz, Class<?> interfaceClass) {
        if (clazz == interfaceClass) {
            return true;
        }
        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces == null) {
            return false;
        }
        for (Class<?> implementedInterface : interfaces) {
            if (implementedInterface == interfaceClass) {
                return true;
            }
            boolean result = recursiveIsImplementsInterface(implementedInterface, interfaceClass);
            if (result) {
                return result;
            }
        }
        return false;
    }

    /**
     * Looks up the given class and checks that it implements or extends the required interface,
     * and instantiates an object.
     *
     * @param implementedOrExtendedClass is the class that the looked-up class should extend or implement
     * @param className                  of the class to load, check type and instantiate
     * @param classForNameProvider       lookup of class for class name
     * @return instance of given class, via newInstance
     * @throws ClassInstantiationException if the type does not match or the class cannot be loaded or an object instantiated
     */
    public static Object instantiate(Class<?> implementedOrExtendedClass, String className, ClassForNameProvider classForNameProvider) throws ClassInstantiationException {
        Class<?> clazz;
        try {
            clazz = classForNameProvider.classForName(className);
        } catch (ClassNotFoundException ex) {
            throw new ClassInstantiationException("Unable to load class '" + className + "', class not found", ex);
        }

        return instantiate(implementedOrExtendedClass, clazz);
    }

    /**
     * Checks that the given class implements or extends the required interface (first parameter),
     * and instantiates an object.
     *
     * @param implementedOrExtendedClass is the class that the looked-up class should extend or implement
     * @param clazz                      to check type and instantiate
     * @return instance of given class, via newInstance
     * @throws ClassInstantiationException if the type does not match or the class cannot be loaded or an object instantiated
     */
    public static Object instantiate(Class<?> implementedOrExtendedClass, Class<?> clazz) throws ClassInstantiationException {
        if (!JavaClassHelper.isSubclassOrImplementsInterface(clazz, implementedOrExtendedClass)) {
            if (implementedOrExtendedClass.isInterface()) {
                throw new ClassInstantiationException("Class '" + clazz.getName() + "' does not implement interface '" + implementedOrExtendedClass.getName() + "'");
            }
            throw new ClassInstantiationException("Class '" + clazz.getName() + "' does not extend '" + implementedOrExtendedClass.getName() + "'");
        }

        Object obj;
        try {
            obj = clazz.newInstance();
        } catch (InstantiationException ex) {
            throw new ClassInstantiationException("Unable to instantiate from class '" + clazz.getName() + "' via default constructor", ex);
        } catch (IllegalAccessException ex) {
            throw new ClassInstantiationException("Illegal access when instantiating class '" + clazz.getName() + "' via default constructor", ex);
        }

        return obj;
    }

    /**
     * Populates all interface and superclasses for the given class, recursivly.
     *
     * @param clazz  to reflect upon
     * @param result set of classes to populate
     */
    public static void getSuper(Class<?> clazz, Set<Class> result) {
        getSuperInterfaces(clazz, result);
        getSuperClasses(clazz, result);
    }

    /**
     * Returns true if the simple class name is the class name of the fully qualified classname.
     * <p>This method does not verify validity of class and package names, it uses simple string compare
     * inspecting the trailing part of the fully qualified class name.
     *
     * @param simpleClassName         simple class name
     * @param fullyQualifiedClassname fully qualified class name contains package name and simple class name
     * @return true if simple class name of the fully qualified class name, false if not
     */
    public static boolean isSimpleNameFullyQualfied(String simpleClassName, String fullyQualifiedClassname) {
        return (fullyQualifiedClassname.endsWith("." + simpleClassName)) || (fullyQualifiedClassname.equals(simpleClassName));
    }

    /**
     * Returns true if the Class is a fragmentable type, i.e. not a primitive or boxed type or
     * any of the common built-in types or does not implement Map.
     *
     * @param type type to check
     * @return true if fragmentable
     */
    public static boolean isFragmentableType(EPType type) {
        if (type == null || type == EPTypeNull.INSTANCE) {
            return false;
        }
        return isFragmentableType(((EPTypeClass) type).getType());
    }

    /**
     * Returns true if the Class is a fragmentable type, i.e. not a primitive or boxed type or
     * any of the common built-in types or does not implement Map.
     *
     * @param propertyType type to check
     * @return true if fragmentable
     */
    public static boolean isFragmentableType(Class<?> propertyType) {
        if (propertyType == null) {
            return false;
        }
        if (propertyType.isArray()) {
            return isFragmentableType(propertyType.getComponentType());
        }
        if (JavaClassHelper.isJavaBuiltinDataType(propertyType)) {
            return false;
        }
        if (propertyType.isEnum()) {
            return false;
        }
        if (JavaClassHelper.isImplementsInterface(propertyType, Map.class)) {
            return false;
        }
        if (JavaClassHelper.isImplementsInterface(propertyType, Collection.class)) {
            return false;
        }
        if (propertyType == Number.class ||
            propertyType == Node.class ||
            propertyType == NodeList.class ||
            propertyType == Object.class ||
            propertyType == Calendar.class ||
            propertyType == Date.class ||
            propertyType == LocalDateTime.class ||
            propertyType == ZonedDateTime.class ||
            propertyType == LocalDate.class ||
            propertyType == LocalTime.class ||
            propertyType == java.sql.Date.class ||
            propertyType == java.sql.Time.class ||
            propertyType == Optional.class) {
            return false;
        }
        if (propertyType.getName().equals(APACHE_AVRO_GENERIC_RECORD_CLASSNAME)) {
            return false;
        }
        return true;
    }

    public static Class[] getSuperInterfaces(Class<?> clazz) {
        Set<Class> interfaces = new HashSet<>();
        Class[] declaredInterfaces = clazz.getInterfaces();

        for (int i = 0; i < declaredInterfaces.length; i++) {
            interfaces.add(declaredInterfaces[i]);
            getSuperInterfaces(declaredInterfaces[i], interfaces);
        }

        Set<Class> superClasses = new HashSet<>();
        getSuperClasses(clazz, superClasses);
        for (Class<?> superClass : superClasses) {
            declaredInterfaces = superClass.getInterfaces();

            for (int i = 0; i < declaredInterfaces.length; i++) {
                interfaces.add(declaredInterfaces[i]);
                getSuperInterfaces(declaredInterfaces[i], interfaces);
            }
        }

        return interfaces.toArray(new Class[declaredInterfaces.length]);
    }

    public static void getSuperInterfaces(Class<?> clazz, Set<Class> result) {
        Class[] interfaces = clazz.getInterfaces();

        for (int i = 0; i < interfaces.length; i++) {
            result.add(interfaces[i]);
            getSuperInterfaces(interfaces[i], result);
        }
    }

    private static void getSuperClasses(Class<?> clazz, Set<Class> result) {
        Class<?> superClass = clazz.getSuperclass();
        if (superClass == null) {
            return;
        }

        result.add(superClass);
        getSuper(superClass, result);
    }

    /**
     * Returns the generic type parameter of a return value by a field.
     *
     * @param field       field or null if method
     * @param isAllowNull whether null is allowed as a return value or expected Object.class
     * @return generic type parameter
     */
    public static Class<?> getGenericFieldType(Field field, boolean isAllowNull) {
        Type t = field.getGenericType();
        Class<?> result = getGenericType(t, 0);
        if (!isAllowNull && result == null) {
            return Object.class;
        }
        return result;
    }

    /**
     * Returns the generic type parameter of a return value by a field or method.
     *
     * @param field       field or null if method
     * @param isAllowNull whether null is allowed as a return value or expected Object.class
     * @return generic type parameter
     */
    public static Class<?> getGenericFieldTypeMap(Field field, boolean isAllowNull) {
        Type t = field.getGenericType();
        Class<?> result = getGenericType(t, 1);
        if (!isAllowNull && result == null) {
            return Object.class;
        }
        return result;
    }

    public static Class<?> getGenericType(Type t, int index) {
        if (t == null) {
            return null;
        }
        if (!(t instanceof ParameterizedType)) {
            return null;
        }
        ParameterizedType ptype = (ParameterizedType) t;
        if ((ptype.getActualTypeArguments() == null) || (ptype.getActualTypeArguments().length < (index + 1))) {
            return Object.class;
        }
        Type typeParam = ptype.getActualTypeArguments()[index];
        if (typeParam instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) typeParam;
            if (genericArrayType.getGenericComponentType() instanceof Class) {
                return JavaClassHelper.getArrayType((Class<?>) genericArrayType.getGenericComponentType());
            }
        }
        if (!(typeParam instanceof Class)) {
            return Object.class;
        }
        return (Class<?>) typeParam;
    }

    public static Class<?> getArrayType(Class<?> resultType) {
        if (resultType == null) {
            return null;
        }
        return Array.newInstance(resultType, 0).getClass();
    }

    public static int getArrayDimensions(Class<?> clazz) {
        return clazz.isArray() ? getArrayDimensions(clazz.getComponentType()) + 1 : 0;
    }

    public static Class<?> getArrayComponentTypeInnermost(Class<?> clazz) {
        return clazz.isArray() ? getArrayComponentTypeInnermost(clazz.getComponentType()) : clazz;
    }

    public static Class<?> getArrayType(Class<?> resultType, int numberOfDimensions) {
        if (resultType == null) {
            return null;
        }
        if (numberOfDimensions == 0) {
            return resultType;
        }
        Class<?> inner = getArrayType(resultType);
        if (numberOfDimensions == 1) {
            return inner;
        }
        return getArrayType(inner, numberOfDimensions - 1);
    }

    public static Method getMethodByName(Class<?> clazz, String methodName) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        throw new IllegalStateException("Expected '" + methodName + "' method not found on interface '" + clazz.getName());
    }

    public static String printInstance(Object instance, boolean fullyQualified) {
        if (instance == null) {
            return "(null)";
        }
        StringWriter writer = new StringWriter();
        writeInstance(writer, instance, fullyQualified);
        return writer.toString();
    }

    public static void writeInstance(StringWriter writer, Object instance, boolean fullyQualified) {
        if (instance == null) {
            writer.write("(null)");
            return;
        }

        String className;
        if (fullyQualified) {
            className = instance.getClass().getName();
        } else {
            className = instance.getClass().getSimpleName();
            if (className.isEmpty()) {
                className = instance.getClass().getName();
            }
        }
        writeInstance(writer, className, instance);
    }

    public static void writeInstance(StringWriter writer, String title, Object instance) {
        writer.write(title);
        writer.write("@");
        if (instance == null) {
            writer.write("(null)");
        } else {
            writer.write(Integer.toHexString(System.identityHashCode(instance)));
        }
    }

    public static String getMessageInvocationTarget(String optionalStatementName, String methodName, Class[] methodParameters, String classOrPropertyName, Object[] args, Throwable targetException) {

        String statementName = optionalStatementName == null ? "(unnamed)" : optionalStatementName;

        String parameters = args == null ? "null" : Arrays.toString(args);
        if (args != null) {
            for (int i = 0; i < methodParameters.length; i++) {
                if (methodParameters[i].isPrimitive() && args[i] == null) {
                    return "NullPointerException invoking method '" + methodName +
                        "' of class '" + classOrPropertyName +
                        "' in parameter " + i +
                        " passing parameters " + parameters +
                        " for statement '" + statementName + "': The method expects a primitive " + methodParameters[i].getSimpleName() +
                        " value but received a null value";
                }
            }
        }

        return "Invocation exception when invoking method '" + methodName +
            "' of class '" + classOrPropertyName +
            "' passing parameters " + parameters +
            " for statement '" + statementName + "': " + targetException.getClass().getSimpleName() + " : " + targetException.getMessage();
    }

    public static boolean isDatetimeClass(EPType inputType) {
        if (inputType == null || inputType == EPTypeNull.INSTANCE) {
            return false;
        }
        return isDatetimeClass(((EPTypeClass) inputType).getType());
    }

    public static boolean isDatetimeClass(Class<?> inputType) {
        if (inputType == null) {
            return false;
        }
        if ((!JavaClassHelper.isSubclassOrImplementsInterface(inputType, Calendar.class)) &&
            (!JavaClassHelper.isSubclassOrImplementsInterface(inputType, Date.class)) &&
            (!JavaClassHelper.isSubclassOrImplementsInterface(inputType, LocalDateTime.class)) &&
            (!JavaClassHelper.isSubclassOrImplementsInterface(inputType, ZonedDateTime.class)) &&
            (JavaClassHelper.getBoxedType(inputType) != Long.class)) {
            return false;
        }
        return true;
    }

    public static Map<String, Object> getClassObjectFromPropertyTypeNames(Properties properties, ClassForNameProvider classForNameProvider) throws ClassNotFoundException {
        Map<String, Object> propertyTypes = new LinkedHashMap<String, Object>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String className = (String) entry.getValue();

            if ("string".equals(className)) {
                className = String.class.getName();
            }

            // use the boxed type for primitives
            String boxedClassName = JavaClassHelper.getBoxedClassName(className);

            Class<?> clazz = classForNameProvider.classForName(boxedClassName);

            propertyTypes.put((String) entry.getKey(), clazz);
        }
        return propertyTypes;
    }

    public static Class<?> getClassInClasspath(String classname, ClassForNameProvider classForNameProvider) {
        try {
            return classForNameProvider.classForName(classname);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    public static boolean isSignatureCompatible(Class<?>[] one, Class<?>[] two) {
        if (Arrays.equals(one, two)) {
            return true;
        }
        if (one.length != two.length) {
            return false;
        }
        for (int i = 0; i < one.length; i++) {
            Class<?> oneClass = one[i];
            Class<?> twoClass = two[i];
            if (!JavaClassHelper.isAssignmentCompatible(oneClass, twoClass)) {
                return false;
            }
        }
        return true;
    }

    public static Method findRequiredMethod(Class<?> clazz, String methodName) {
        Method found = null;
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName)) {
                found = m;
                break;
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("Not found method '" + methodName + "'");
        }
        return found;
    }

    public static List<Annotation> getAnnotations(Class<? extends Annotation> annotationClass, Annotation[] annotations) {
        List<Annotation> result = null;
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == annotationClass) {
                if (result == null) {
                    result = new ArrayList<Annotation>();
                }
                result.add(annotation);
            }
        }
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    public static boolean isAnnotationListed(Class<? extends Annotation> annotationClass, Annotation[] annotations) {
        return !getAnnotations(annotationClass, annotations).isEmpty();
    }

    public static Set<Field> findAnnotatedFields(Class<?> targetClass, Class<? extends Annotation> annotation) {
        Set<Field> fields = new LinkedHashSet<Field>();
        findFieldInternal(targetClass, annotation, fields);

        // superclass fields
        Class<?> clazz = targetClass;
        while (true) {
            clazz = clazz.getSuperclass();
            if (clazz == Object.class || clazz == null) {
                break;
            }
            findFieldInternal(clazz, annotation, fields);
        }
        return fields;
    }

    private static void findFieldInternal(Class<?> currentClass, Class<? extends Annotation> annotation, Set<Field> fields) {
        for (Field field : currentClass.getDeclaredFields()) {
            if (isAnnotationListed(annotation, field.getDeclaredAnnotations())) {
                fields.add(field);
            }
        }
    }

    public static Set<Method> findAnnotatedMethods(Class<?> targetClass, Class<? extends Annotation> annotation) {
        Set<Method> methods = new LinkedHashSet<Method>();
        findAnnotatedMethodsInternal(targetClass, annotation, methods);

        // superclass fields
        Class<?> clazz = targetClass;
        while (true) {
            clazz = clazz.getSuperclass();
            if (clazz == Object.class || clazz == null) {
                break;
            }
            findAnnotatedMethodsInternal(clazz, annotation, methods);
        }
        return methods;
    }

    private static void findAnnotatedMethodsInternal(Class<?> currentClass, Class<? extends Annotation> annotation, Set<Method> methods) {
        for (Method method : currentClass.getDeclaredMethods()) {
            Annotation[] methodAnnotations = method.getDeclaredAnnotations();
            if (isAnnotationListed(annotation, methodAnnotations)) {
                methods.add(method);
            }
        }
    }

    public static void setFieldForAnnotation(Object target, Class<? extends Annotation> annotation, Object value) {
        boolean found = setFieldForAnnotation(target, annotation, value, target.getClass());
        if (!found) {

            Class<?> superClass = target.getClass().getSuperclass();
            while (!found) {
                found = setFieldForAnnotation(target, annotation, value, superClass);
                if (!found) {
                    superClass = superClass.getSuperclass();
                }
                if (superClass == Object.class || superClass == null) {
                    break;
                }
            }
        }
    }

    private static boolean setFieldForAnnotation(Object target, Class<? extends Annotation> annotation, Object value, Class<?> currentClass) {
        boolean found = false;
        for (Field field : currentClass.getDeclaredFields()) {
            if (isAnnotationListed(annotation, field.getDeclaredAnnotations())) {
                field.setAccessible(true);
                try {
                    field.set(target, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to set field " + field + " on class " + currentClass.getName() + ": " + e.getMessage(), e);
                }
                return true;
            }
        }
        return false;
    }

    static Class<?>[] takeFirstN(Class<?>[] classes, int numToTake) {
        Class<?>[] shrunk = new Class[numToTake];
        System.arraycopy(classes, 0, shrunk, 0, shrunk.length);
        return shrunk;
    }

    public static Type[] takeFirstN(Type[] types, int numToTake) {
        Type[] shrunk = new Type[numToTake];
        System.arraycopy(types, 0, shrunk, 0, shrunk.length);
        return shrunk;
    }

    private static CoercionException getCoercionException(EPTypeClass type, EPTypeClass other) {
        throw new CoercionException("Cannot coerce to " + type + " type " + other);
    }

    public static void getObjectValuePretty(Object value, StringWriter writer) {
        if (value == null) {
            writer.append("(null)");
            return;
        }
        if (!value.getClass().isArray()) {
            writer.append(value.toString());
            return;
        }
        String delimiter = "";
        writer.append("[");
        for (int i = 0; i < Array.getLength(value); i++) {
            writer.append(delimiter);
            delimiter = ", ";
            Object val = Array.get(value, i);
            getObjectValuePretty(val, writer);
        }
        writer.append("]");
    }

    public static boolean isTypeVoid(EPType type) {
        return isTypeEither(type, void.class, Void.class);
    }

    public static boolean isTypeVoid(Class<?> clazz) {
        return clazz == void.class || clazz == Void.class;
    }

    public static boolean isTypeOrNull(EPType type, Class<?> expected) {
        if (isNullTypeSafe(type)) {
            return true;
        }
        EPTypeClass typeClass = (EPTypeClass) type;
        if (typeClass.getType() == expected) {
            return true;
        }
        return JavaClassHelper.getBoxedType(typeClass.getType()) == expected;
    }

    public static <T> void traverseAnnotations(List<Class> classes, Class<T> annotationClass, BiConsumer<Class, T> consumer) {
        walkAnnotations(classes, (annotation, clazz) -> {
            if (annotation.annotationType() == annotationClass) {
                consumer.accept(clazz, (T) annotation);
            }
        });
    }

    private static boolean isNullTypeSafe(EPType type) {
        return type == null || type == EPTypeNull.INSTANCE;
    }

    private static void walkAnnotations(List<Class> classes, AnnotationConsumer consumer) {
        if (classes == null) {
            return;
        }
        for (Class<?> clazz : classes) {
            for (Annotation annotation : clazz.getDeclaredAnnotations()) {
                consumer.accept(annotation, clazz);
            }
        }
    }

    private static boolean isTypeEither(EPType type, Class<?> one, Class<?> two) {
        if (isNullTypeSafe(type)) {
            return false;
        }
        EPTypeClass typeClass = (EPTypeClass) type;
        return typeClass.getType() == one || typeClass.getType() == two;
    }

    public static EPTypeClass getArrayComponentType(EPTypeClass type) {
        if (!type.getType().isArray()) {
            throw new IllegalArgumentException("Not an array");
        }
        if (type instanceof EPTypeClassParameterized) {
            EPTypeClassParameterized parameterized = (EPTypeClassParameterized) type;
            return new EPTypeClassParameterized(parameterized.getType().getComponentType(), parameterized.getParameters());
        }
        return getOrCreate(type.getType().getComponentType());
    }

    public static EPTypeClass getArrayComponentTypeInnermost(EPTypeClass type) {
        if (!type.getType().isArray()) {
            throw new IllegalArgumentException("Not an array");
        }
        EPTypeClass component = getArrayComponentType(type);
        if (!component.getType().isArray()) {
            return component;
        }
        return getArrayComponentTypeInnermost(component);
    }

    public static EPTypeClass getSingleParameterTypeOrObject(EPTypeClass returnType) {
        if (!(returnType instanceof EPTypeClassParameterized)) {
            return OBJECT.getEPType();
        }
        EPTypeClassParameterized parameterized = (EPTypeClassParameterized) returnType;
        if (parameterized.getParameters().length != 1) {
            return OBJECT.getEPType();
        }
        return parameterized.getParameters()[0];
    }

    public static EPTypeClass getSecondParameterTypeOrObject(EPTypeClass returnType) {
        if (!(returnType instanceof EPTypeClassParameterized)) {
            return OBJECT.getEPType();
        }
        EPTypeClassParameterized parameterized = (EPTypeClassParameterized) returnType;
        if (parameterized.getParameters().length != 2) {
            return OBJECT.getEPType();
        }
        return parameterized.getParameters()[1];
    }

    public static EPTypeClass getTypeClassOrObjectType(EPType evaluationType) {
        if (evaluationType == null || evaluationType == EPTypeNull.INSTANCE) {
            return OBJECT.getEPType();
        }
        return (EPTypeClass) evaluationType;
    }

    public static EPTypeClass getArrayType(EPTypeClass type) {
        return getArrayType(type, 1);
    }

    public static EPTypeClass getArrayType(EPTypeClass type, int numDimensions) {
        if (numDimensions == 0) {
            return type;
        }
        Class inner = getArrayType(type.getType(), numDimensions);
        if (type instanceof EPTypeClassParameterized) {
            EPTypeClassParameterized parameterized = (EPTypeClassParameterized) type;
            return new EPTypeClassParameterized(inner, parameterized.getParameters());
        }
        return EPTypePremade.getOrCreate(inner);
    }

    public interface AnnotationConsumer {
        void accept(Annotation annotation, Class<?> clazz);
    }
}
