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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SerializerFactory {

    private static final List<Serializer> SERIALIZERS;
    private static final Serializer NULL_SERIALIZER = new Serializer() {
        public boolean accepts(Class c) {
            throw new UnsupportedOperationException("Not supported for null serializer");
        }

        public void serialize(Object object, DataOutputStream stream) throws IOException {
            // no-action
        }

        public Object deserialize(DataInputStream stream) throws IOException {
            return null;
        }
    };
    private static final Serializer OBJECT_SERIALIZER = new Serializer() {
        public boolean accepts(Class c) {
            throw new UnsupportedOperationException("Not supported for object serializer");
        }

        public void serialize(Object object, DataOutputStream stream) throws IOException {
            new ObjectOutputStream(stream).writeObject(object);
        }

        public Object deserialize(DataInputStream stream) throws IOException {
            try {
                return new ObjectInputStreamWithTCCL(stream).readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }
    };

    static {
        SERIALIZERS = new ArrayList<Serializer>();

        SERIALIZERS.add(new Serializer<Integer>() {
            public boolean accepts(Class c) {
                return Integer.class.equals(c);
            }

            public void serialize(Integer object, DataOutputStream stream) throws IOException {
                stream.writeInt(object);
            }

            public Integer deserialize(DataInputStream stream) throws IOException {
                return stream.readInt();
            }
        });
        SERIALIZERS.add(new Serializer<Long>() {
            public boolean accepts(Class c) {
                return Long.class.equals(c);
            }

            public void serialize(Long object, DataOutputStream stream) throws IOException {
                stream.writeLong(object);
            }

            public Long deserialize(DataInputStream stream) throws IOException {
                return stream.readLong();
            }
        });
        SERIALIZERS.add(new Serializer<Float>() {
            public boolean accepts(Class c) {
                return Float.class.equals(c);
            }

            public void serialize(Float object, DataOutputStream stream) throws IOException {
                stream.writeFloat(object);
            }

            public Float deserialize(DataInputStream stream) throws IOException {
                return stream.readFloat();
            }
        });
        SERIALIZERS.add(new Serializer<Double>() {
            public boolean accepts(Class c) {
                return Double.class.equals(c);
            }

            public void serialize(Double object, DataOutputStream stream) throws IOException {
                stream.writeDouble(object);
            }

            public Double deserialize(DataInputStream stream) throws IOException {
                return stream.readDouble();
            }
        });
        SERIALIZERS.add(new Serializer<Byte>() {
            public boolean accepts(Class c) {
                return Byte.class.equals(c);
            }

            public void serialize(Byte object, DataOutputStream stream) throws IOException {
                stream.writeByte(object);
            }

            public Byte deserialize(DataInputStream stream) throws IOException {
                return stream.readByte();
            }
        });
        SERIALIZERS.add(new Serializer<Short>() {
            public boolean accepts(Class c) {
                return Short.class.equals(c);
            }

            public void serialize(Short object, DataOutputStream stream) throws IOException {
                stream.writeShort(object);
            }

            public Short deserialize(DataInputStream stream) throws IOException {
                return stream.readShort();
            }
        });
        SERIALIZERS.add(new Serializer<String>() {
            public boolean accepts(Class c) {
                return String.class.equals(c);
            }

            public void serialize(String object, DataOutputStream stream) throws IOException {
                stream.writeUTF(object);
            }

            public String deserialize(DataInputStream stream) throws IOException {
                return stream.readUTF();
            }
        });
        SERIALIZERS.add(new Serializer<Boolean>() {
            public boolean accepts(Class c) {
                return Boolean.class.equals(c);
            }

            public void serialize(Boolean object, DataOutputStream stream) throws IOException {
                stream.writeBoolean(object);
            }

            public Boolean deserialize(DataInputStream stream) throws IOException {
                return stream.readBoolean();
            }
        });
    }

    public static Serializer[] getSerializers(Class[] classes) {
        Serializer[] serializers = new Serializer[classes.length];
        for (int i = 0; i < classes.length; i++) {
            serializers[i] = getSerializer(classes[i]);
        }
        return serializers;
    }

    public static Serializer getSerializer(Class clazz) {
        if (clazz == null) {
            return NULL_SERIALIZER;
        }
        for (Serializer serializer : SERIALIZERS) {
            if (serializer.accepts(JavaClassHelper.getBoxedType(clazz))) {
                return serializer;
            }
        }
        return OBJECT_SERIALIZER;
    }

    public static byte[] serialize(Serializer[] serializers, Object[] objects) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(buf);

        for (int i = 0; i < serializers.length; i++) {
            if (objects[i] != null) {
                serializers[i].serialize(objects[i], ds);
            }
        }

        return buf.toByteArray();
    }

    public static Object[] deserialize(int numObjects, byte[] bytes, Serializer[] serializers) throws IOException {
        ByteArrayInputStream buf = new ByteArrayInputStream(bytes);
        DataInputStream ds = new DataInputStream(buf);
        Object[] result = new Object[numObjects];

        for (int i = 0; i < serializers.length; i++) {
            result[i] = serializers[i].deserialize(ds);
        }

        return result;
    }
}
