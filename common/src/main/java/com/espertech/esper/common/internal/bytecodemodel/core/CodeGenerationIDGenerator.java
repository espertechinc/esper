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
package com.espertech.esper.common.internal.bytecodemodel.core;

import com.espertech.esper.common.internal.util.UuidGenerator;

import java.io.StringWriter;

public class CodeGenerationIDGenerator {
    public static String generateClassNameUUID() {
        return UuidGenerator.generate().replace("-", "_");
    }

    public static String generateClassNameWithUUID(Class interfaceClass, String postfix, String uuid) {
        StringWriter writer = new StringWriter();
        writer.append(interfaceClass.getSimpleName()).append("_").append(postfix.replace("(", "_").replace(")", "_")).append("_").append(uuid);
        return writer.toString();
    }

    public static String generateClassNameSimple(Class interfaceClass, String postfix) {
        return interfaceClass.getSimpleName() + "_" + postfix;
    }

    public static String generateClassNameWithPackage(String packageName, Class interfaceClass, String postfix) {
        String simple = generateClassNameSimple(interfaceClass, postfix);
        if (packageName == null || packageName.isEmpty()) {
            return simple;
        }
        return packageName + "." + simple;
    }
}
