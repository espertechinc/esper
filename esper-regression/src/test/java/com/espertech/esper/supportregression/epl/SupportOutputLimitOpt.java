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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.client.annotation.HintEnum;

public enum SupportOutputLimitOpt {
    DEFAULT(""),
    ENABLED("@Hint('" + HintEnum.ENABLE_OUTPUTLIMIT_OPT.getValue() + "')"),
    DISABLED("@Hint('" + HintEnum.DISABLE_OUTPUTLIMIT_OPT.getValue() + "')");

    private final String hint;

    SupportOutputLimitOpt(String hint) {
        this.hint = hint;
    }

    public String getHint() {
        return hint;
    }
}
