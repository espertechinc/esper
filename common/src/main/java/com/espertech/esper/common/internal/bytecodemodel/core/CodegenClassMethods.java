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

import java.util.ArrayList;
import java.util.List;

public class CodegenClassMethods {
    private final List<CodegenMethodWGraph> publicMethods = new ArrayList<>(2);
    private final List<CodegenMethodWGraph> privateMethods = new ArrayList<>();

    public List<CodegenMethodWGraph> getPublicMethods() {
        return publicMethods;
    }

    public List<CodegenMethodWGraph> getPrivateMethods() {
        return privateMethods;
    }

    public int size() {
        return publicMethods.size() + privateMethods.size();
    }
}
