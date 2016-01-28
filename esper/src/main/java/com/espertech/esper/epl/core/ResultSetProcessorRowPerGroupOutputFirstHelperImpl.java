/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.core;

import com.espertech.esper.epl.view.OutputConditionPolled;

import java.util.HashMap;
import java.util.Map;

public class ResultSetProcessorRowPerGroupOutputFirstHelperImpl implements ResultSetProcessorRowPerGroupOutputFirstHelper {
    private final Map<Object, OutputConditionPolled> outputState = new HashMap<Object, OutputConditionPolled>();

    public void remove(Object key) {
        outputState.remove(key);
    }

    public OutputConditionPolled get(Object mk) {
        return outputState.get(mk);
    }

    public void put(Object mk, OutputConditionPolled outputStateGroup) {
        outputState.put(mk, outputStateGroup);
    }

    public void destroy() {
        // no action required
    }
}
