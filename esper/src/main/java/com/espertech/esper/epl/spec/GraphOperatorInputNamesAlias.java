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
package com.espertech.esper.epl.spec;

import java.io.Serializable;

public class GraphOperatorInputNamesAlias implements Serializable {
    private static final long serialVersionUID = -1755557830393035854L;
    private final String[] inputStreamNames;
    private final String optionalAsName;

    public GraphOperatorInputNamesAlias(String[] inputStreamNames, String optionalAsName) {
        this.inputStreamNames = inputStreamNames;
        this.optionalAsName = optionalAsName;
    }

    public String[] getInputStreamNames() {
        return inputStreamNames;
    }

    public String getOptionalAsName() {
        return optionalAsName;
    }
}
