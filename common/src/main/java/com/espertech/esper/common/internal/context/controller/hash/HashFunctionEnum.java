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
package com.espertech.esper.common.internal.context.controller.hash;

import java.io.StringWriter;
import java.util.Locale;

public enum HashFunctionEnum {
    CONSISTENT_HASH_CRC32,
    HASH_CODE;

    public static HashFunctionEnum determine(String contextName, String name) {
        String nameTrim = name.toLowerCase(Locale.ENGLISH).trim();
        for (HashFunctionEnum val : HashFunctionEnum.values()) {
            if (val.name().toLowerCase(Locale.ENGLISH).trim().equals(nameTrim)) {
                return val;
            }
        }

        return null;
    }

    public static String getStringList() {
        StringWriter message = new StringWriter();
        String delimiter = "";
        for (HashFunctionEnum val : HashFunctionEnum.values()) {
            message.append(delimiter);
            message.append(val.name().toLowerCase(Locale.ENGLISH).trim());
            delimiter = ", ";
        }
        return message.toString();
    }
}
