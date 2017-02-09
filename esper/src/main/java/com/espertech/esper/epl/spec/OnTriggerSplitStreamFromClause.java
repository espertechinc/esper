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

public class OnTriggerSplitStreamFromClause implements Serializable {
    private static final long serialVersionUID = 6732032046677935097L;

    private PropertyEvalSpec propertyEvalSpec;
    private String optionalStreamName;

    public OnTriggerSplitStreamFromClause(PropertyEvalSpec propertyEvalSpec, String optionalStreamName) {
        this.propertyEvalSpec = propertyEvalSpec;
        this.optionalStreamName = optionalStreamName;
    }

    public PropertyEvalSpec getPropertyEvalSpec() {
        return propertyEvalSpec;
    }

    public void setPropertyEvalSpec(PropertyEvalSpec propertyEvalSpec) {
        this.propertyEvalSpec = propertyEvalSpec;
    }

    public String getOptionalStreamName() {
        return optionalStreamName;
    }

    public void setOptionalStreamName(String optionalStreamName) {
        this.optionalStreamName = optionalStreamName;
    }
}
