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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static com.espertech.esper.common.internal.event.json.write.JsonWriteUtil.writeJsonValue;

public abstract class JsonEventObjectBase implements JsonEventObject {
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
}

