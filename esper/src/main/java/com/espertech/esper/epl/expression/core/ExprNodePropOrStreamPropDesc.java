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
package com.espertech.esper.epl.expression.core;

public class ExprNodePropOrStreamPropDesc implements ExprNodePropOrStreamDesc {

    private final int streamNum;
    private final String propertyName;

    public ExprNodePropOrStreamPropDesc(int streamNum, String propertyName) {
        this.streamNum = streamNum;
        this.propertyName = propertyName;
        if (propertyName == null) {
            throw new IllegalArgumentException("Property name is null");
        }
    }

    public String getPropertyName() {
        return propertyName;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public String getTextual() {
        return "property '" + propertyName + "'";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExprNodePropOrStreamPropDesc)) return false;

        ExprNodePropOrStreamPropDesc propDesc = (ExprNodePropOrStreamPropDesc) o;

        if (streamNum != propDesc.streamNum) return false;
        if (!propertyName.equals(propDesc.propertyName)) return false;

        return true;
    }

    public int hashCode() {
        int result = streamNum;
        result = 31 * result + propertyName.hashCode();
        return result;
    }
}
