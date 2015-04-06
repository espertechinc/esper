/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.filter;

import com.espertech.esper.client.ConfigurationEngineDefaults;

/**
 * Static factory for implementations of the {@link FilterService} interface.
 */
public final class FilterServiceProvider
{
    /**
     * Creates an implementation of the FilterEvaluationService interface.
     * @return implementation
     */
    public static FilterServiceSPI newService(ConfigurationEngineDefaults.FilterServiceProfile filterServiceProfile)
    {
        if (filterServiceProfile == ConfigurationEngineDefaults.FilterServiceProfile.READMOSTLY) {
            return new FilterServiceLockCoarse();
        }
        else {
            return new FilterServiceLockFine();
        }
    }
}
