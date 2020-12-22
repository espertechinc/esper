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
package com.espertech.esper.common.internal.compile.stage2;

public class FilterSpecAttributionStreamPattern implements FilterSpecAttribution {
    private final int streamNum;
    private final int factoryNodeId;

    public FilterSpecAttributionStreamPattern(int streamNum, int factoryNodeId) {
        this.streamNum = streamNum;
        this.factoryNodeId = factoryNodeId;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public int getFactoryNodeId() {
        return factoryNodeId;
    }

    public <T> T accept(FilterSpecAttributionVisitor<T> visitor) {
        return visitor.accept(this);
    }
}
