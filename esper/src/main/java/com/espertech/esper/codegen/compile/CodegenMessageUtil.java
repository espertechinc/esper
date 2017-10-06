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
package com.espertech.esper.codegen.compile;

import java.io.StringWriter;
import java.util.function.Supplier;

import static com.espertech.esper.codegen.compile.CodeGenerationUtil.codeWithLineNum;

public class CodegenMessageUtil {
    public static String getFailedCompileLogMessageWithCode(Throwable t, Supplier<String> debugInformationProvider, boolean enableFallback) {
        if (!(t instanceof CodegenCompilerException)) {
            StringWriter message = new StringWriter();
            message.append("Failed to code-generate for ")
                    .append(debugInformationProvider.get())
                    .append(": ").append(t.getMessage());
            return message.toString();
        }

        CodegenCompilerException ex = (CodegenCompilerException) t;
        StringWriter message = new StringWriter();
        message.append("Failed to code-generate for ")
                .append(debugInformationProvider.get())
                .append(" (invalid code follows");
        if (enableFallback) {
            message.append(", falling back to regular evaluation");
        }
        message.append("): ").append(ex.getMessage());
        message.append("\r\ncode-in-error (please provide with issue reports):\r\n")
                .append(codeWithLineNum(ex.getCode()));
        return message.toString();
    }
}
