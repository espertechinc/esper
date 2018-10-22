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
package com.espertech.esper.common.internal.support;

public class SupportJavaVersionUtil {
    public final static double JAVA_VERSION = getVersion();

    public static String getCastMessage(Class from, Class to) {
        if (JAVA_VERSION >= 11) {
            return "class " + from.getName() + " cannot be cast to class " + to.getName() +
                    " (" + from.getName() + " and " + to.getName() + " are in module java.base of loader 'bootstrap'";
        }
        if (JAVA_VERSION >= 10) {
            return "java.base/" + from.getName() + " cannot be cast to java.base/" + to.getName();
        }
        return from.getName() + " cannot be cast to " + to.getName();
    }

    private static double getVersion() {
        String version = System.getProperty("java.version");
        int pos = version.indexOf('.');
        if (pos == -1) {
            return Double.parseDouble(version);
        }
        pos = version.indexOf('.', pos + 1);
        return Double.parseDouble(version.substring(0, pos));
    }
}
