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

import java.util.ArrayList;
import java.util.List;

public class CodegenClassMethods {
    private final List<CodegenMethod> publicMethods = new ArrayList<>(2);
    private final List<CodegenMethod> privateMethods = new ArrayList<>();

    public List<CodegenMethod> getPublicMethods() {
        return publicMethods;
    }
    public List<CodegenMethod> getPrivateMethods() {
        return privateMethods;
    }
}
