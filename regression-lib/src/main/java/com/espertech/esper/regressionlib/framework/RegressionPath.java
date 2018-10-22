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
package com.espertech.esper.regressionlib.framework;

import com.espertech.esper.common.client.EPCompiled;

import java.util.ArrayList;
import java.util.List;

public class RegressionPath {
    private final List<EPCompiled> compileds = new ArrayList<>();

    public void add(EPCompiled compiled) {
        compileds.add(compiled);
    }

    public List<EPCompiled> getCompileds() {
        return compileds;
    }

    public void clear() {
        compileds.clear();
    }
}
