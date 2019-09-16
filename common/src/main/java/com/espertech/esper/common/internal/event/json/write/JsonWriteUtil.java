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
package com.espertech.esper.common.internal.event.json.write;

import com.espertech.esper.common.client.json.minimaljson.JsonWriter;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

public class JsonWriteUtil {
    private static final Logger log = LoggerFactory.getLogger(JsonEventObjectBase.class);

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param value  value
     * @throws IOException io error
     */
    public static void writeNullableString(JsonWriter writer, String value) throws IOException {
        if (value == null) {
            writer.writeLiteral("null");
        } else {
            writer.writeString(value);
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param value  value
     * @throws IOException io error
     */
    public static void writeNullableStringToString(JsonWriter writer, Object value) throws IOException {
        if (value == null) {
            writer.writeLiteral("null");
        } else {
            writer.writeString(value.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param value  value
     * @throws IOException io error
     */
    public static void writeNullableBoolean(JsonWriter writer, Boolean value) throws IOException {
        if (value == null) {
            writer.writeLiteral("null");
        } else {
            writer.writeLiteral(value ? "true" : "false");
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param value  value
     * @throws IOException io error
     */
    public static void writeNullableNumber(JsonWriter writer, Object value) throws IOException {
        if (value == null) {
            writer.writeLiteral("null");
        } else {
            writer.writeNumber(value.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimString(JsonWriter writer, String[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (String[] strings : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayString(writer, strings);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimCharacter(JsonWriter writer, Character[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Character[] characters : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayCharacter(writer, characters);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimLong(JsonWriter writer, Long[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Long[] longs : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayLong(writer, longs);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimInteger(JsonWriter writer, Integer[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Integer[] ints : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayInteger(writer, ints);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer  writer
     * @param array   value
     * @param factory write class
     * @throws IOException io error
     */
    public static void writeArray2DimAppClass(JsonWriter writer, Object[][] array, JsonDelegateFactory factory) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Object[] values : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayAppClass(writer, values, factory);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimShort(JsonWriter writer, Short[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Short[] shorts : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayShort(writer, shorts);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimDouble(JsonWriter writer, Double[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Double[] doubles : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayDouble(writer, doubles);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimFloat(JsonWriter writer, Float[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Float[] floats : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayFloat(writer, floats);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimByte(JsonWriter writer, Byte[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Byte[] b : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayByte(writer, b);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimBoolean(JsonWriter writer, Boolean[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Boolean[] bools : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayBoolean(writer, bools);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimBigInteger(JsonWriter writer, BigInteger[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (BigInteger[] b : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayBigInteger(writer, b);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimObjectToString(JsonWriter writer, Object[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Object[] b : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayObjectToString(writer, b);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimBigDecimal(JsonWriter writer, BigDecimal[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (BigDecimal[] b : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayBigDecimal(writer, b);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimBooleanPrimitive(JsonWriter writer, boolean[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (boolean[] b : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayBooleanPrimitive(writer, b);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimBytePrimitive(JsonWriter writer, byte[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (byte[] bytes : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayBytePrimitive(writer, bytes);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimShortPrimitive(JsonWriter writer, short[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (short[] shorts : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayShortPrimitive(writer, shorts);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimIntPrimitive(JsonWriter writer, int[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (int[] ints : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayIntPrimitive(writer, ints);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimLongPrimitive(JsonWriter writer, long[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (long[] longs : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayLongPrimitive(writer, longs);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimFloatPrimitive(JsonWriter writer, float[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (float[] floats : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayFloatPrimitive(writer, floats);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimDoublePrimitive(JsonWriter writer, double[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (double[] doubles : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayDoublePrimitive(writer, doubles);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArray2DimCharPrimitive(JsonWriter writer, char[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (char[] chars : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeArrayCharPrimitive(writer, chars);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayString(JsonWriter writer, String[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (String string : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableString(writer, string);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param values value
     * @throws IOException io error
     */
    public static void writeCollectionString(JsonWriter writer, Collection<String> values) throws IOException {
        if (values == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (String string : values) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableString(writer, string);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer  writer
     * @param values  value
     * @param factory delegate factory
     * @throws IOException io error
     */
    public static void writeCollectionAppClass(JsonWriter writer, Collection values, JsonDelegateFactory factory) throws IOException {
        if (values == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Object value : values) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            if (value == null) {
                writer.writeLiteral("null");
            } else {
                factory.write(writer, value);
            }
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer  writer
     * @param values  value
     * @param factory delegate factory
     * @throws IOException io error
     */
    public static void writeArrayAppClass(JsonWriter writer, Object[] values, JsonDelegateFactory factory) throws IOException {
        if (values == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Object value : values) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            if (value == null) {
                writer.writeLiteral("null");
            } else {
                factory.write(writer, value);
            }
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayCharacter(JsonWriter writer, Character[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Character character : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableStringToString(writer, character);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayLong(JsonWriter writer, Long[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Long l : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableNumber(writer, l);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayInteger(JsonWriter writer, Integer[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Integer i : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableNumber(writer, i);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param values value
     * @throws IOException io error
     */
    public static void writeCollectionNumber(JsonWriter writer, Collection<Number> values) throws IOException {
        if (values == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Number i : values) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableNumber(writer, i);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayShort(JsonWriter writer, Short[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Short s : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableNumber(writer, s);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayDouble(JsonWriter writer, Double[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Double d : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableNumber(writer, d);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayFloat(JsonWriter writer, Float[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Float f : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableNumber(writer, f);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayByte(JsonWriter writer, Byte[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Byte b : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableNumber(writer, b);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayBoolean(JsonWriter writer, Boolean[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Boolean b : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableBoolean(writer, b);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param values value
     * @throws IOException io error
     */
    public static void writeCollectionBoolean(JsonWriter writer, Collection<Boolean> values) throws IOException {
        if (values == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Boolean b : values) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableBoolean(writer, b);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayBigInteger(JsonWriter writer, BigInteger[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (BigInteger b : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableNumber(writer, b);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayBigDecimal(JsonWriter writer, BigDecimal[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (BigDecimal b : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeNullableNumber(writer, b);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayBooleanPrimitive(JsonWriter writer, boolean[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (boolean b : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writer.writeLiteral(b ? "true" : "false");
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayBytePrimitive(JsonWriter writer, byte[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (byte b : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writer.writeNumber(Byte.toString(b));
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayShortPrimitive(JsonWriter writer, short[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (short s : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writer.writeNumber(Short.toString(s));
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayIntPrimitive(JsonWriter writer, int[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (int i : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writer.writeNumber(Integer.toString(i));
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayLongPrimitive(JsonWriter writer, long[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (long l : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writer.writeNumber(Long.toString(l));
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayFloatPrimitive(JsonWriter writer, float[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (float f : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writer.writeNumber(Float.toString(f));
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayDoublePrimitive(JsonWriter writer, double[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (double d : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writer.writeNumber(Double.toString(d));
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayCharPrimitive(JsonWriter writer, char[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (char c : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writer.writeString(Character.toString(c));
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeArrayObjectToString(JsonWriter writer, Object[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Object value : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            if (value == null) {
                writer.writeLiteral("null");
            } else {
                writer.writeString(value.toString());
            }
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeEnumArray(JsonWriter writer, Object[] array) throws IOException {
        writeObjectArrayWToString(writer, array);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param values value
     * @throws IOException io error
     */
    public static void writeEnumCollection(JsonWriter writer, Collection values) throws IOException {
        writeCollectionWToString(writer, values);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeEnumArray2Dim(JsonWriter writer, Object[][] array) throws IOException {
        writeObjectArray2DimWToString(writer, array);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param name      name
     * @param writer    writer
     * @param jsonValue value
     * @throws IOException io error
     */
    public static void writeJsonValue(JsonWriter writer, String name, Object jsonValue) throws IOException {
        if (jsonValue == null) {
            writer.writeLiteral("null");
        } else if (jsonValue instanceof Boolean) {
            writer.writeLiteral((boolean) jsonValue ? "true" : "false");
        } else if (jsonValue instanceof String) {
            writer.writeString((String) jsonValue);
        } else if (jsonValue instanceof Number) {
            writer.writeNumber(jsonValue.toString());
        } else if (jsonValue instanceof Object[]) {
            writeJsonArray(writer, name, (Object[]) jsonValue);
        } else if (jsonValue instanceof Map) {
            writeJsonMap(writer, (Map<String, Object>) jsonValue);
        } else if (jsonValue instanceof JsonEventObjectBase) {
            writer.writeObjectOpen();
            JsonEventObjectBase und = (JsonEventObjectBase) jsonValue;
            und.write(writer);
            writer.writeObjectClose();
        } else {
            log.warn("Unknown json value of type " + jsonValue.getClass() + " encountered, skipping member '" + name + "'");
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param map    value
     * @throws IOException io error
     */
    public static void writeJsonMap(JsonWriter writer, Map<String, Object> map) throws IOException {
        if (map == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeObjectOpen();
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writer.writeMemberName(entry.getKey());
            writer.writeMemberSeparator();
            writeJsonValue(writer, entry.getKey(), entry.getValue());
        }
        writer.writeObjectClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param name   name
     * @param writer writer
     * @param array  value
     * @throws IOException io error
     */
    public static void writeJsonArray(JsonWriter writer, String name, Object[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Object item : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeJsonValue(writer, name, item);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param nested value
     * @param nestedFactory  writer for nested object
     * @throws IOException io error
     */
    public static void writeNested(JsonWriter writer, Object nested, JsonDelegateFactory nestedFactory) throws IOException {
        if (nested == null) {
            writer.writeLiteral("null");
            return;
        }
        nestedFactory.write(writer, nested);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param nested value
     * @throws IOException io error
     */
    public static void writeNested(JsonWriter writer, JsonEventObjectBase nested) throws IOException {
        if (nested == null) {
            writer.writeLiteral("null");
            return;
        }
        nested.write(writer);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer  writer
     * @param nesteds value
     * @param nestedFactory writer for nested object
     * @throws IOException io error
     */
    public static void writeNestedArray(JsonWriter writer, Object[] nesteds, JsonDelegateFactory nestedFactory) throws IOException {
        if (nesteds == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Object nested : nesteds) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            nestedFactory.write(writer, nested);
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer  writer
     * @param nesteds value
     * @throws IOException io error
     */
    public static void writeNestedArray(JsonWriter writer, JsonEventObjectBase[] nesteds) throws IOException {
        if (nesteds == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (JsonEventObjectBase nested : nesteds) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            nested.write(writer);
        }
        writer.writeArrayClose();
    }

    private static void writeObjectArrayWToString(JsonWriter writer, Object[] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Object object : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            if (object == null) {
                writer.writeLiteral("null");
            } else {
                writer.writeString(object.toString());
            }
        }
        writer.writeArrayClose();
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param writer writer
     * @param values collection
     * @throws IOException io error
     */
    public static void writeCollectionWToString(JsonWriter writer, Collection values) throws IOException {
        if (values == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Object object : values) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            if (object == null) {
                writer.writeLiteral("null");
            } else {
                writer.writeString(object.toString());
            }
        }
        writer.writeArrayClose();
    }

    private static void writeObjectArray2DimWToString(JsonWriter writer, Object[][] array) throws IOException {
        if (array == null) {
            writer.writeLiteral("null");
            return;
        }
        writer.writeArrayOpen();
        boolean first = true;
        for (Object[] objects : array) {
            if (!first) {
                writer.writeObjectSeparator();
            }
            first = false;
            writeObjectArrayWToString(writer, objects);
        }
        writer.writeArrayClose();
    }
}
