/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.support.core;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.EngineImportServiceImpl;

import java.util.TimeZone;

public class SupportEngineImportServiceFactory {

    public static EngineImportServiceImpl make() {
        return new EngineImportServiceImpl(true, true, true, false, null, TimeZone.getDefault(), ConfigurationEngineDefaults.ThreadingProfile.NORMAL);
    }
}
