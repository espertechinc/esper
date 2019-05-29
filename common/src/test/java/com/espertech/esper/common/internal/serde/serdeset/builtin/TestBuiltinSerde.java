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
package com.espertech.esper.common.internal.serde.serdeset.builtin;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.collection.MultiKeyArrayWrap;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.support.SupportBean;
import junit.framework.TestCase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

public class TestBuiltinSerde extends TestCase {
    public void testSerde() throws IOException {
        assertSerde(DIOBooleanSerde.INSTANCE, true);
        assertSerde(DIOByteSerde.INSTANCE, Byte.decode("0x0F"));
        assertSerde(DIOCharacterSerde.INSTANCE, 'x');
        assertSerde(DIOCharSequenceSerde.INSTANCE, "abc");
        assertSerde(DIODoubleSerde.INSTANCE, 10d);
        assertSerde(DIOFloatSerde.INSTANCE, 11f);
        assertSerde(DIOIntegerSerde.INSTANCE, 12);
        assertSerde(DIOShortSerde.INSTANCE, (short) 13);
        assertSerde(DIOLongSerde.INSTANCE, 14L);
        assertSerde(DIOStringSerde.INSTANCE, "def");

        assertSerde(DIOPrimitiveByteArraySerde.INSTANCE, new byte[] {1, 2});
        assertSerdeWNull(DIOPrimitiveBooleanArrayNullableSerde.INSTANCE, new boolean[] {true, false});
        assertSerdeWNull(DIOPrimitiveByteArrayNullableSerde.INSTANCE, new byte[] {1, 2});
        assertSerdeWNull(DIOPrimitiveCharArrayNullableSerde.INSTANCE, new char[] {'a', 'b'});
        assertSerdeWNull(DIOPrimitiveDoubleArrayNullableSerde.INSTANCE, new double[] {1d, 2d});
        assertSerdeWNull(DIOPrimitiveFloatArrayNullableSerde.INSTANCE, new float[] {1f, 2f});
        assertSerdeWNull(DIOPrimitiveIntArrayNullableSerde.INSTANCE, new int[] {1, 2});
        assertSerdeWNull(DIOPrimitiveLongArrayNullableSerde.INSTANCE, new long[] {1, 2});
        assertSerdeWNull(DIOPrimitiveShortArrayNullableSerde.INSTANCE, new short[] {1, 2});

        assertSerdeWNull(DIOPrimitiveCharArray2DimNullableSerde.INSTANCE, new char[][] {{'a', 'b'}, {'c'}});
        assertSerdeWNull(DIOPrimitiveDoubleArray2DimNullableSerde.INSTANCE, new double[][] {{1, 2d}, {3d}});
        assertSerdeWNull(DIOPrimitiveFloatArray2DimNullableSerde.INSTANCE, new float[][] {{1f, 2f}, {3f}});
        assertSerdeWNull(DIOPrimitiveIntArray2DimNullableSerde.INSTANCE, new int[][] {{1, 2}, {3}});
        assertSerdeWNull(DIOPrimitiveLongArray2DimNullableSerde.INSTANCE, new long[][] {{1, 2}, {3}});
        assertSerdeWNull(DIOPrimitiveShortArray2DimNullableSerde.INSTANCE, new short[][] {{1, 2}, {3}});
        assertSerdeWNull(DIOPrimitiveBooleanArray2DimNullableSerde.INSTANCE, new boolean[][] {{true, false}, {true}});
        assertSerdeWNull(DIOPrimitiveByteArray2DimNullableSerde.INSTANCE, new byte[][] {{1, 2}, {3}});

        assertSerdeWNull(DIONullableBooleanSerde.INSTANCE, true);
        assertSerdeWNull(DIONullableByteSerde.INSTANCE, Byte.decode("0xf"));
        assertSerdeWNull(DIONullableCharacterSerde.INSTANCE, 'x');
        assertSerdeWNull(DIONullableDoubleSerde.INSTANCE, 10d);
        assertSerdeWNull(DIONullableFloatSerde.INSTANCE, 11f);
        assertSerdeWNull(DIONullableIntegerSerde.INSTANCE, 12);
        assertSerdeWNull(DIONullableLongSerde.INSTANCE, 13L);
        assertSerdeWNull(DIONullableShortSerde.INSTANCE, (short) 14);
        assertSerdeWNull(DIOStringSerde.INSTANCE, "abc");

        assertSerdeWNull(DIOBigDecimalSerde.INSTANCE, BigDecimal.TEN);
        assertSerdeWNull(DIOBigIntegerSerde.INSTANCE, BigInteger.TEN);
        assertSerdeWNull(DIODateSerde.INSTANCE, new Date());
        assertSerdeWNull(DIOCalendarSerde.INSTANCE, Calendar.getInstance());
        assertSerdeWNull(DIOSqlDateSerde.INSTANCE, new java.sql.Date(1));
        assertSerdeWNull(DIOSqlDateArrayNullableSerde.INSTANCE, new java.sql.Date[] {new java.sql.Date(1), null});

        assertSerdeWNull(DIOBigDecimalArrayNullableSerde.INSTANCE, new BigDecimal[]{BigDecimal.ONE, BigDecimal.valueOf(10)});
        assertSerdeWNull(DIOBigIntegerArrayNullableSerde.INSTANCE, new BigInteger[] {BigInteger.ONE, BigInteger.valueOf(10)});
        assertSerdeWNull(DIOBigDecimalArray2DimNullableSerde.INSTANCE, new BigDecimal[][] {{BigDecimal.ONE, BigDecimal.valueOf(10)}, null, {null}});
        assertSerdeWNull(DIOBigIntegerArray2DimNullableSerde.INSTANCE, new BigInteger[][] {{BigInteger.ONE, BigInteger.valueOf(10)}, null, {null}});

        assertSerdeWNull(DIOBoxedBooleanArrayNullableSerde.INSTANCE, new Boolean[] {true, null, false});
        assertSerdeWNull(DIOBoxedByteArrayNullableSerde.INSTANCE, new Byte[] {1, null, 0x2});
        assertSerdeWNull(DIOBoxedCharacterArrayNullableSerde.INSTANCE, new Character[] {1, null, 2});
        assertSerdeWNull(DIOBoxedDoubleArrayNullableSerde.INSTANCE, new Double[] {1d, null, 2d});
        assertSerdeWNull(DIOBoxedFloatArrayNullableSerde.INSTANCE, new Float[] {1f, null, 2f});
        assertSerdeWNull(DIOBoxedIntegerArrayNullableSerde.INSTANCE, new Integer[] {1, null, 2});
        assertSerdeWNull(DIOBoxedLongArrayNullableSerde.INSTANCE, new Long[] {1L, null, 2L});
        assertSerdeWNull(DIOBoxedShortArrayNullableSerde.INSTANCE, new Short[] {1, null, 2});
        assertSerdeWNull(DIOStringArrayNullableSerde.INSTANCE, new String[] {"A", null, "B"});

        assertSerdeWNull(DIOBoxedBooleanArray2DimNullableSerde.INSTANCE, new Boolean[][] {{true, null, false}, null, {true}});
        assertSerdeWNull(DIOBoxedByteArray2DimNullableSerde.INSTANCE, new Byte[][] {{1, null, 0x2}, null, {0x3}});
        assertSerdeWNull(DIOBoxedCharacterArray2DimNullableSerde.INSTANCE, new Character[][] {{1, null, 2}, null, {1}});
        assertSerdeWNull(DIOBoxedDoubleArray2DimNullableSerde.INSTANCE, new Double[][] {{1d, null, 2d}, null, {3d}});
        assertSerdeWNull(DIOBoxedFloatArray2DimNullableSerde.INSTANCE, new Float[][] {{1f, null, 2f}, null, {3f}});
        assertSerdeWNull(DIOBoxedIntegerArray2DimNullableSerde.INSTANCE, new Integer[][] {{1, null, 2}, null, {3}});
        assertSerdeWNull(DIOBoxedLongArray2DimNullableSerde.INSTANCE, new Long[][] {{1L, null, 2L}, null, {3L}});
        assertSerdeWNull(DIOBoxedShortArray2DimNullableSerde.INSTANCE, new Short[][] {{1, null, 2}, null, {0x3}});
        assertSerdeWNull(DIOStringArray2DimNullableSerde.INSTANCE, new String[][] {{"A", null, "B"}, null, {}, {"a"}});

        assertSerdeWNull(DIOCalendarArrayNullableSerde.INSTANCE, new Calendar[] {Calendar.getInstance(), null});
        assertSerdeWNull(DIODateArrayNullableSerde.INSTANCE, new Date[] {new Date(), null});

        assertSerde(DIOSkipSerde.INSTANCE, null);
        assertSerdeWNull(DIOSerializableObjectSerde.INSTANCE, new SupportBean());

        assertSerdeWNull(new DIONullableObjectArraySerde(SupportBean.class, DIOSerializableObjectSerde.INSTANCE), new SupportBean[] {new SupportBean(), null});
        assertSerde(new DIOSetSerde(DIOIntegerSerde.INSTANCE), new HashSet<>(Arrays.asList(1, 2)));

    }

    public static <T> void assertSerdeWNull(DataInputOutputSerde<T> serde, T serialized) throws IOException  {
        assertSerde(serde, serialized);
        assertSerde(serde, null);
    }

    public static <T> void assertSerde(DataInputOutputSerde<T> serde, T serialized) throws IOException  {
        FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(fos);
        serde.write(serialized, dos, null, null);
        dos.close();
        fos.close();
        byte[] bytes = fos.getByteArrayFast();

        FastByteArrayInputStream fis = new FastByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(fis);
        Object deserialized = serde.read(dis, null);
        dis.close();
        fis.close();

        if (serialized == null) {
            assertNull(deserialized);
            return;
        }
        if (serialized.getClass().isArray()) {
            MultiKeyArrayWrap wrapExpected = getWrapped(serialized);
            MultiKeyArrayWrap wrapValue = getWrapped(deserialized);
            assertEquals(wrapValue, wrapExpected);
            return;
        }
        assertEquals(deserialized, serialized);
    }

    private static MultiKeyArrayWrap getWrapped(Object array) {
        Class mkclzz = MultiKeyPlanner.getMKClassForComponentType(array.getClass().getComponentType());
        Constructor[] ctors = mkclzz.getConstructors();
        try {
            return (MultiKeyArrayWrap) ctors[0].newInstance(array);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
