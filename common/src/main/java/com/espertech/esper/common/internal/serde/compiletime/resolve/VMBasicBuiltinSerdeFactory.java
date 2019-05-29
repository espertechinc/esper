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
package com.espertech.esper.common.internal.serde.compiletime.resolve;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.serde.serdeset.builtin.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for serde implementations that provides a serde for a given Java built-in type.
 */
public class VMBasicBuiltinSerdeFactory {
    private static final Map<Class, DataInputOutputSerde> PRIMITIVES = new HashMap<>();
    private static final Map<Class, DataInputOutputSerde> BOXED = new HashMap<>();

    static {
        addPrimitive(char.class, DIOCharacterSerde.INSTANCE);
        addPrimitive(boolean.class, DIOBooleanSerde.INSTANCE);
        addPrimitive(byte.class, DIOByteSerde.INSTANCE);
        addPrimitive(short.class, DIOShortSerde.INSTANCE);
        addPrimitive(int.class, DIOIntegerSerde.INSTANCE);
        addPrimitive(long.class, DIOLongSerde.INSTANCE);
        addPrimitive(float.class, DIOFloatSerde.INSTANCE);
        addPrimitive(double.class, DIODoubleSerde.INSTANCE);
        addPrimitive(void.class, DIOSkipSerde.INSTANCE);

        addBoxed(String.class, DIOStringSerde.INSTANCE);
        addBoxed(CharSequence.class, DIOCharSequenceSerde.INSTANCE);
        addBoxed(Character.class, DIONullableCharacterSerde.INSTANCE);
        addBoxed(Boolean.class, DIONullableBooleanSerde.INSTANCE);
        addBoxed(Byte.class, DIONullableByteSerde.INSTANCE);
        addBoxed(Short.class, DIONullableShortSerde.INSTANCE);
        addBoxed(Integer.class, DIONullableIntegerSerde.INSTANCE);
        addBoxed(Long.class, DIONullableLongSerde.INSTANCE);
        addBoxed(Float.class, DIONullableFloatSerde.INSTANCE);
        addBoxed(Double.class, DIONullableDoubleSerde.INSTANCE);

        addBoxed(String[].class, DIOStringArrayNullableSerde.INSTANCE);
        addBoxed(CharSequence[].class, DIOStringArrayNullableSerde.INSTANCE);
        addBoxed(Character[].class, DIOBoxedCharacterArrayNullableSerde.INSTANCE);
        addBoxed(Boolean[].class, DIOBoxedBooleanArrayNullableSerde.INSTANCE);
        addBoxed(Byte[].class, DIOBoxedByteArrayNullableSerde.INSTANCE);
        addBoxed(Short[].class, DIOBoxedShortArrayNullableSerde.INSTANCE);
        addBoxed(Integer[].class, DIOBoxedIntegerArrayNullableSerde.INSTANCE);
        addBoxed(Long[].class, DIOBoxedLongArrayNullableSerde.INSTANCE);
        addBoxed(Float[].class, DIOBoxedFloatArrayNullableSerde.INSTANCE);
        addBoxed(Double[].class, DIOBoxedDoubleArrayNullableSerde.INSTANCE);

        addBoxed(char[].class, DIOPrimitiveCharArrayNullableSerde.INSTANCE);
        addBoxed(boolean[].class, DIOPrimitiveBooleanArrayNullableSerde.INSTANCE);
        addBoxed(byte[].class, DIOPrimitiveByteArrayNullableSerde.INSTANCE);
        addBoxed(short[].class, DIOPrimitiveShortArrayNullableSerde.INSTANCE);
        addBoxed(int[].class, DIOPrimitiveIntArrayNullableSerde.INSTANCE);
        addBoxed(long[].class, DIOPrimitiveLongArrayNullableSerde.INSTANCE);
        addBoxed(float[].class, DIOPrimitiveFloatArrayNullableSerde.INSTANCE);
        addBoxed(double[].class, DIOPrimitiveDoubleArrayNullableSerde.INSTANCE);

        addBoxed(char[][].class, DIOPrimitiveCharArray2DimNullableSerde.INSTANCE);
        addBoxed(boolean[][].class, DIOPrimitiveBooleanArray2DimNullableSerde.INSTANCE);
        addBoxed(byte[][].class, DIOPrimitiveByteArray2DimNullableSerde.INSTANCE);
        addBoxed(short[][].class, DIOPrimitiveShortArray2DimNullableSerde.INSTANCE);
        addBoxed(int[][].class, DIOPrimitiveIntArray2DimNullableSerde.INSTANCE);
        addBoxed(long[][].class, DIOPrimitiveLongArray2DimNullableSerde.INSTANCE);
        addBoxed(float[][].class, DIOPrimitiveFloatArray2DimNullableSerde.INSTANCE);
        addBoxed(double[][].class, DIOPrimitiveDoubleArray2DimNullableSerde.INSTANCE);

        addBoxed(String[][].class, DIOStringArray2DimNullableSerde.INSTANCE);
        addBoxed(CharSequence[][].class, DIOStringArray2DimNullableSerde.INSTANCE);
        addBoxed(Character[][].class, DIOBoxedCharacterArray2DimNullableSerde.INSTANCE);
        addBoxed(Boolean[][].class, DIOBoxedBooleanArray2DimNullableSerde.INSTANCE);
        addBoxed(Byte[][].class, DIOBoxedByteArray2DimNullableSerde.INSTANCE);
        addBoxed(Short[][].class, DIOBoxedShortArray2DimNullableSerde.INSTANCE);
        addBoxed(Integer[][].class, DIOBoxedIntegerArray2DimNullableSerde.INSTANCE);
        addBoxed(Long[][].class, DIOBoxedLongArray2DimNullableSerde.INSTANCE);
        addBoxed(Float[][].class, DIOBoxedFloatArray2DimNullableSerde.INSTANCE);
        addBoxed(Double[][].class, DIOBoxedDoubleArray2DimNullableSerde.INSTANCE);

        addBoxed(BigInteger[].class, DIOBigIntegerArrayNullableSerde.INSTANCE);
        addBoxed(BigDecimal[].class, DIOBigDecimalArrayNullableSerde.INSTANCE);
        addBoxed(BigInteger[][].class, DIOBigIntegerArray2DimNullableSerde.INSTANCE);
        addBoxed(BigDecimal[][].class, DIOBigDecimalArray2DimNullableSerde.INSTANCE);
    }

    private static void addPrimitive(Class cls, DataInputOutputSerde serde) {
        PRIMITIVES.put(cls, serde);
    }

    private static void addBoxed(Class cls, DataInputOutputSerde serde) {
        BOXED.put(cls, serde);
    }

    /**
     * Returns the serde for the given Java built-in type.
     *
     * @param cls is the Java type
     * @return serde for marshalling and unmarshalling that type
     */
    protected static DataInputOutputSerde getSerde(Class cls) {
        if (cls.isPrimitive()) {
            return PRIMITIVES.get(cls);
        }
        return BOXED.get(cls);
    }
}
