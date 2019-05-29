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
package com.espertech.esper.common.internal.event.json.core;

import com.espertech.esper.common.client.json.minimaljson.JsonWriter;
import com.espertech.esper.common.client.json.minimaljson.WriterConfig;
import com.espertech.esper.common.client.json.minimaljson.WritingBuffer;
import com.espertech.esper.common.client.json.util.JsonEventObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public abstract class JsonEventObjectBase implements JsonEventObject {
    private static final Logger log = LoggerFactory.getLogger(JsonEventObjectBase.class);

    /**
     * Add a dynamic property value that the json parser encounters.
     * Dynamic property values are not predefined and are catch-all in nature.
     *
     * @param name  name
     * @param value value
     */
    public abstract void addJsonValue(String name, Object value);

    /**
     * Returns the dynamic property values (the non-predefined values)
     *
     * @return map
     */
    public abstract Map<String, Object> getJsonValues();

    /**
     * Returns the total number of pre-declared properties available including properties of the parent event type if any
     *
     * @return size
     */
    public abstract int getNativeSize();

    /**
     * Returns the name-value pre-declared property including properties of the parent event type if any
     *
     * @param num index number of the property
     * @return entry
     * @throws java.util.NoSuchElementException for invalid index
     */
    public abstract Map.Entry<String, Object> getNativeEntry(int num);

    /**
     * Returns the pre-declared property name including properties names of the parent event type if any
     *
     * @param num index number of the property
     * @return name
     * @throws java.util.NoSuchElementException for invalid index
     */
    public abstract String getNativeKey(int num);

    /**
     * Returns the value of a pre-declared property including property values of the parent event type if any
     *
     * @param num index number of the property
     * @return value
     * @throws java.util.NoSuchElementException for invalid index
     */
    public abstract Object getNativeValue(int num);

    /**
     * Returns the index number of a a pre-declared property of the same name including property names of the parent event type if any
     *
     * @param name property name
     * @return index starting at zero, ending at native-size minus 1; Returns -1 for non-existing property name
     */
    public abstract int getNativeNum(String name);

    /**
     * Returns the index number of a a pre-declared property of the same name including property names of the parent event type if any
     *
     * @param num   index number of the property
     * @param value to set
     * @throws java.util.NoSuchElementException for invalid index
     */
    public abstract void setNativeValue(int num, Object value);

    /**
     * Returns the flag whether the key exists as a pre-declared property of the same name including property names of the parent event type if any
     *
     * @param key property name
     * @return flag
     */
    public abstract boolean nativeContainsKey(Object key);

    /**
     * Write the pre-declared properties to the writer
     *
     * @param writer writer
     * @throws IOException for IO exceptions
     */
    public abstract void nativeWrite(JsonWriter writer) throws IOException;

    public void writeTo(Writer writer, WriterConfig config) throws IOException {
        WritingBuffer buffer = new WritingBuffer(writer, 128);
        write(config.createWriter(buffer));
        buffer.flush();
    }

    public void write(JsonWriter writer) throws IOException {
        writer.writeObjectOpen();
        nativeWrite(writer);
        boolean first = getNativeSize() == 0;
        for (Map.Entry<String, Object> entry : getJsonValues().entrySet()) {
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

    public int size() {
        return getJsonValues().size() + getNativeSize();
    }

    public Set<Entry<String, Object>> entrySet() {
        return new JsonEventUnderlyingEntrySet(this);
    }

    public boolean isEmpty() {
        return getNativeSize() == 0 && getJsonValues().isEmpty();
    }

    public Set<String> keySet() {
        return new JsonEventUnderlyingKeySet(this);
    }

    public boolean containsKey(Object key) {
        return nativeContainsKey(key) || getJsonValues().containsKey(key);
    }

    public Object get(Object key) {
        if (key == null || !(key instanceof String)) {
            return getJsonValues().get(key);
        }
        int num = getNativeNum((String) key);
        if (num == -1) {
            return getJsonValues().get(key);
        }
        return getNativeValue(num);
    }

    public boolean containsValue(Object value) {
        if (value == null) {
            for (int i = 0; i < getNativeSize(); i++) {
                if (getNativeValue(i) == null) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < getNativeSize(); i++) {
                if (value.equals(getNativeValue(i))) {
                    return true;
                }
            }
        }
        return getJsonValues().containsValue(value);
    }

    public Collection<Object> values() {
        return new JsonEventUnderlyingValueCollection(this, getJsonValues().values());
    }

    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {
        throw new UnsupportedOperationException();
    }

    public Object putIfAbsent(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    public boolean replace(String key, Object oldValue, Object newValue) {
        throw new UnsupportedOperationException();
    }

    public Object replace(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    public String toString(WriterConfig config) {
        StringWriter writer = new StringWriter();
        try {
            writeTo(writer, config);
        } catch (IOException exception) {
            // StringWriter does not throw IOExceptions
            throw new RuntimeException(exception);
        }
        return writer.toString();
    }

    public String toString() {
        return toString(WriterConfig.MINIMAL);
    }

    protected void writeNullableString(JsonWriter writer, String value) throws IOException {
        if (value == null) {
            writer.writeLiteral("null");
        } else {
            writer.writeString(value);
        }
    }

    protected void writeNullableStringToString(JsonWriter writer, Object value) throws IOException {
        if (value == null) {
            writer.writeLiteral("null");
        } else {
            writer.writeString(value.toString());
        }
    }

    protected void writeNullableBoolean(JsonWriter writer, Boolean value) throws IOException {
        if (value == null) {
            writer.writeLiteral("null");
        } else {
            writer.writeLiteral(value ? "true" : "false");
        }
    }

    protected void writeNullableNumber(JsonWriter writer, Object value) throws IOException {
        if (value == null) {
            writer.writeLiteral("null");
        } else {
            writer.writeNumber(value.toString());
        }
    }

    protected void writeArray2DimString(JsonWriter writer, String[][] array) throws IOException {
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

    protected void writeArray2DimCharacter(JsonWriter writer, Character[][] array) throws IOException {
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

    protected void writeArray2DimLong(JsonWriter writer, Long[][] array) throws IOException {
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

    protected void writeArray2DimInteger(JsonWriter writer, Integer[][] array) throws IOException {
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

    protected void writeArray2DimShort(JsonWriter writer, Short[][] array) throws IOException {
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

    protected void writeArray2DimDouble(JsonWriter writer, Double[][] array) throws IOException {
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

    protected void writeArray2DimFloat(JsonWriter writer, Float[][] array) throws IOException {
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

    protected void writeArray2DimByte(JsonWriter writer, Byte[][] array) throws IOException {
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

    protected void writeArray2DimBoolean(JsonWriter writer, Boolean[][] array) throws IOException {
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

    protected void writeArray2DimBigInteger(JsonWriter writer, BigInteger[][] array) throws IOException {
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

    protected void writeArray2DimBigDecimal(JsonWriter writer, BigDecimal[][] array) throws IOException {
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

    protected void writeArray2DimBooleanPrimitive(JsonWriter writer, boolean[][] array) throws IOException {
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

    protected void writeArray2DimBytePrimitive(JsonWriter writer, byte[][] array) throws IOException {
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

    protected void writeArray2DimShortPrimitive(JsonWriter writer, short[][] array) throws IOException {
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

    protected void writeArray2DimIntPrimitive(JsonWriter writer, int[][] array) throws IOException {
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

    protected void writeArray2DimLongPrimitive(JsonWriter writer, long[][] array) throws IOException {
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

    protected void writeArray2DimFloatPrimitive(JsonWriter writer, float[][] array) throws IOException {
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

    protected void writeArray2DimDoublePrimitive(JsonWriter writer, double[][] array) throws IOException {
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

    protected void writeArray2DimCharPrimitive(JsonWriter writer, char[][] array) throws IOException {
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

    protected void writeArrayString(JsonWriter writer, String[] array) throws IOException {
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

    protected void writeArrayCharacter(JsonWriter writer, Character[] array) throws IOException {
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

    protected void writeArrayLong(JsonWriter writer, Long[] array) throws IOException {
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

    protected void writeArrayInteger(JsonWriter writer, Integer[] array) throws IOException {
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

    protected void writeArrayShort(JsonWriter writer, Short[] array) throws IOException {
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

    protected void writeArrayDouble(JsonWriter writer, Double[] array) throws IOException {
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

    protected void writeArrayFloat(JsonWriter writer, Float[] array) throws IOException {
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

    protected void writeArrayByte(JsonWriter writer, Byte[] array) throws IOException {
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

    protected void writeArrayBoolean(JsonWriter writer, Boolean[] array) throws IOException {
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

    protected void writeArrayBigInteger(JsonWriter writer, BigInteger[] array) throws IOException {
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

    protected void writeArrayBigDecimal(JsonWriter writer, BigDecimal[] array) throws IOException {
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

    protected void writeArrayBooleanPrimitive(JsonWriter writer, boolean[] array) throws IOException {
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

    protected void writeArrayBytePrimitive(JsonWriter writer, byte[] array) throws IOException {
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

    protected void writeArrayShortPrimitive(JsonWriter writer, short[] array) throws IOException {
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

    protected void writeArrayIntPrimitive(JsonWriter writer, int[] array) throws IOException {
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

    protected void writeArrayLongPrimitive(JsonWriter writer, long[] array) throws IOException {
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

    protected void writeArrayFloatPrimitive(JsonWriter writer, float[] array) throws IOException {
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

    protected void writeArrayDoublePrimitive(JsonWriter writer, double[] array) throws IOException {
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

    protected void writeArrayCharPrimitive(JsonWriter writer, char[] array) throws IOException {
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

    protected void writeEnumArray(JsonWriter writer, Object[] array) throws IOException {
        writeObjectArrayWToString(writer, array);
    }

    protected void writeEnumArray2Dim(JsonWriter writer, Object[][] array) throws IOException {
        writeObjectArray2DimWToString(writer, array);
    }

    protected void writeJsonValue(JsonWriter writer, String name, Object jsonValue) throws IOException {
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

    protected void writeJsonMap(JsonWriter writer, Map<String, Object> map) throws IOException {
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

    protected void writeJsonArray(JsonWriter writer, String name, Object[] array) throws IOException {
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

    protected static void writeNested(JsonWriter writer, JsonEventObjectBase nested) throws IOException {
        if (nested == null) {
            writer.writeLiteral("null");
            return;
        }
        nested.write(writer);
    }

    protected static void writeNestedArray(JsonWriter writer, JsonEventObjectBase[] nesteds) throws IOException {
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

    private void writeObjectArrayWToString(JsonWriter writer, Object[] array) throws IOException {
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

    private void writeObjectArray2DimWToString(JsonWriter writer, Object[][] array) throws IOException {
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

