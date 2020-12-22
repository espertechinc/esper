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

public class FilterSpecAttributionStream implements FilterSpecAttribution {
    private final int streamNum;

    public FilterSpecAttributionStream(int streamNum) {
        this.streamNum = streamNum;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public <T> T accept(FilterSpecAttributionVisitor<T> visitor) {
        return visitor.accept(this);
    }
}
