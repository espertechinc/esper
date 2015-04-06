/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.util;

import java.io.StringWriter;

public class SupportJVMRuntimeUtil {

    private String memoryUse() {
        StringWriter writer = new StringWriter();
        Runtime runtime = Runtime.getRuntime();
        int mb = 1024*1024;

        writer.append("Used Memory MB:");
        writer.append(Double.toString((runtime.totalMemory() - runtime.freeMemory()) / mb));

        writer.append("  Free Memory MB:");
        writer.append(Double.toString(runtime.freeMemory() / mb));

        writer.append("  Total Memory MB:");
        writer.append(Double.toString(runtime.totalMemory() / mb));

        writer.append("  Max Memory:");
        writer.append(Double.toString(runtime.maxMemory() / mb));

        return writer.toString();
    }

}
