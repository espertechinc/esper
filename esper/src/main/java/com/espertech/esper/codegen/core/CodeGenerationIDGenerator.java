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
package com.espertech.esper.codegen.core;

import com.espertech.esper.util.UuidGenerator;

public class CodeGenerationIDGenerator {
    public static String generateMethod() {
        return "m" + UuidGenerator.generateNoDash();
    }

    public static String generateMember() {
        return "_" + UuidGenerator.generateNoDash();
    }

    public static String generateClass() {
        return "c" + UuidGenerator.generateNoDash();
    }
}
