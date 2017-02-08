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
import java.util.List;

public class GraphOperatorOutputItem implements Serializable {
    private static final long serialVersionUID = 261430636902379399L;
    private final String streamName;
    private final List<GraphOperatorOutputItemType> typeInfo;

    public GraphOperatorOutputItem(String streamName, List<GraphOperatorOutputItemType> typeInfo) {
        this.streamName = streamName;
        this.typeInfo = typeInfo;
    }

    public String getStreamName() {
        return streamName;
    }

    public List<GraphOperatorOutputItemType> getTypeInfo() {
        return typeInfo;
    }
}
