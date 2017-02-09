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
package com.espertech.esper.epl.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * Invocation handler for EPL and application-specified annotations.
 */
public class EPLAnnotationInvocationHandler implements InvocationHandler {
    private final Class annotationClass;
    private String toStringResult;
    private final Map<String, Object> attributes;
    private volatile Integer hashCode;

    /**
     * Ctor.
     *
     * @param annotationClass annotation class
     * @param attributes      attribute values
     */
    public EPLAnnotationInvocationHandler(Class annotationClass, Map<String, Object> attributes) {
        this.annotationClass = annotationClass;
        this.attributes = attributes;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("equals")) {
            if (args.length != 0) {
                return handleEquals(args[0]);
            }
        }
        if (method.getName().equals("hashCode")) {
            return handleHashCode();
        }
        if (method.getName().equals("toString")) {
            if (toStringResult == null) {
                StringBuilder buf = new StringBuilder();
                buf.append("@");
                buf.append(annotationClass.getSimpleName());
                if (!attributes.isEmpty()) {
                    String delimiter = "";
                    buf.append("(");

                    if ((attributes.size() == 1) && (attributes.containsKey("value"))) {
                        if (attributes.get("value") instanceof String) {
                            buf.append("\"");
                            buf.append(attributes.get("value"));
                            buf.append("\"");
                        } else {
                            buf.append(attributes.get("value"));
                        }
                    } else {
                        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
                            buf.append(delimiter);
                            buf.append(attribute.getKey());
                            buf.append("=");
                            if (attribute.getValue() instanceof String) {
                                buf.append("\"");
                                buf.append(attribute.getValue());
                                buf.append("\"");
                            } else {
                                buf.append(attribute.getValue());
                            }
                            delimiter = ", ";
                        }
                    }
                    buf.append(")");
                }
                toStringResult = buf.toString();
            }
            return toStringResult;
        }
        if (method.getName().equals("annotationType")) {
            return annotationClass;
        }
        if (attributes.containsKey(method.getName())) {
            return attributes.get(method.getName());
        }
        return null;
    }

    private Object handleEquals(Object arg) {
        if (this == arg) {
            return true;
        }

        if (!(arg instanceof Annotation)) {
            return false;
        }

        Annotation other = (Annotation) arg;
        if (other.annotationType() != annotationClass) {
            return false;
        }

        if (!Proxy.isProxyClass(arg.getClass())) {
            return false;
        }
        InvocationHandler handler = Proxy.getInvocationHandler(arg);
        if (!(handler instanceof EPLAnnotationInvocationHandler)) {
            return false;
        }

        EPLAnnotationInvocationHandler that = (EPLAnnotationInvocationHandler) handler;
        if (annotationClass != null ? !annotationClass.equals(that.annotationClass) : that.annotationClass != null) {
            return false;
        }

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (!that.attributes.containsKey(entry.getKey())) {
                return false;
            }

            Object thisValue = entry.getValue();
            Object thatValue = that.attributes.get(entry.getKey());
            if (thisValue != null ? !thisValue.equals(thatValue) : thatValue != null) {
                return false;
            }
        }

        return true;
    }

    private Object handleHashCode() {
        if (hashCode != null) {
            return hashCode;
        }

        int result = annotationClass.hashCode();
        for (String key : attributes.keySet()) {
            result = 31 * result + key.hashCode();
        }
        hashCode = result;
        return hashCode;
    }
}
