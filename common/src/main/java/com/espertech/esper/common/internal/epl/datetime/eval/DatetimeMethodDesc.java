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
package com.espertech.esper.common.internal.epl.datetime.eval;

import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;

public class DatetimeMethodDesc {
    private final DatetimeMethodEnum datetimeMethod;
    private final DatetimeMethodProviderForgeFactory forgeFactory;
    private final DotMethodFP[] parameters;

    public DatetimeMethodDesc(DatetimeMethodEnum datetimeMethod, DatetimeMethodProviderForgeFactory forgeFactory, DotMethodFP[] parameters) {
        this.datetimeMethod = datetimeMethod;
        this.forgeFactory = forgeFactory;
        this.parameters = parameters;
    }

    public DatetimeMethodEnum getDatetimeMethod() {
        return datetimeMethod;
    }

    public DatetimeMethodProviderForgeFactory getForgeFactory() {
        return forgeFactory;
    }

    public DotMethodFP[] getFootprints() {
        return parameters;
    }
}
