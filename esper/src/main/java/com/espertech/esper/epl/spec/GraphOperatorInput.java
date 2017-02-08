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
import java.util.ArrayList;
import java.util.List;

public class GraphOperatorInput implements Serializable {
    private static final long serialVersionUID = -8341400118423881624L;
    private final List<GraphOperatorInputNamesAlias> streamNamesAndAliases = new ArrayList<GraphOperatorInputNamesAlias>();

    public List<GraphOperatorInputNamesAlias> getStreamNamesAndAliases() {
        return streamNamesAndAliases;
    }
}
