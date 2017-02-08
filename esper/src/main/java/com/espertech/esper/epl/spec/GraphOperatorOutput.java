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

public class GraphOperatorOutput implements Serializable {
    private static final long serialVersionUID = 6972206796605218643L;
    private final List<GraphOperatorOutputItem> items;

    public GraphOperatorOutput() {
        this.items = new ArrayList<GraphOperatorOutputItem>();
    }

    public List<GraphOperatorOutputItem> getItems() {
        return items;
    }
}
