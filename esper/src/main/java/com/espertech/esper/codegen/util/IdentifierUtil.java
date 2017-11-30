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
package com.espertech.esper.codegen.util;

public class IdentifierUtil {
    public static String getIdentifierMayStartNumeric(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (Character.isJavaIdentifierPart(str.charAt(i)))
                sb.append(str.charAt(i));
            else
                sb.append((int) str.charAt(i));
        }
        return sb.toString();
    }
}
