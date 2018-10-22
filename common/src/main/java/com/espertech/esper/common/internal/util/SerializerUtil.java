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

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.io.*;
import java.util.Base64;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SerializerUtil {
    /**
     * Serialize object to byte array.
     *
     * @param underlying to serialize
     * @return byte array
     */
    public static byte[] objectToByteArr(Object underlying) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(underlying);
            oos.close();
            baos.close();
        } catch (IOException e) {
            throw new RuntimeException("IO error serializing object: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    /**
     * Deserialize byte arry to object.
     *
     * @param bytes to read
     * @return object
     */
    public static Object byteArrToObject(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream ois = new ObjectInputStreamWithTCCL(bais);
            return ois.readObject();
        } catch (IOException e) {
            if (e.getMessage() != null) {
                throw new RuntimeException("IO error de-serializing object: " + e.getMessage(), e);
            }
            throw new RuntimeException("IO error de-serializing object", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found de-serializing object: " + e.getMessage(), e);
        }
    }

    public static String objectToByteArrBase64(Serializable userObject) {
        byte[] bytes = objectToByteArr(userObject);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param base64 encoded string
     * @return object
     */
    public static Object byteArrBase64ToObject(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return byteArrToObject(bytes);
    }

    public static CodegenExpression expressionForUserObject(Object userObject) {
        if (userObject == null) {
            return constantNull();
        }
        boolean serialize = isUseSerialize(userObject.getClass());
        if (!serialize) {
            return constant(userObject);
        }
        String value = SerializerUtil.objectToByteArrBase64((Serializable) userObject);
        return staticMethod(SerializerUtil.class, "byteArrBase64ToObject", constant(value));
    }

    private static boolean isUseSerialize(Class clazz) {
        if (JavaClassHelper.isJavaBuiltinDataType(clazz)) {
            return false;
        }
        if (clazz.isArray()) {
            return isUseSerialize(clazz.getComponentType());
        }
        return true;
    }

}
