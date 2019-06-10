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
package com.espertech.esper.common.client.configuration.common;

import com.espertech.esper.common.client.util.ThreadingProfile;

import java.io.Serializable;

/**
 * Holds common execution-related settings.
 */
public class ConfigurationCommonExecution implements Serializable {
    private static final long serialVersionUID = -5811779933558904439L;
    private ThreadingProfile threadingProfile = ThreadingProfile.NORMAL;

    /**
     * Returns the threading profile
     *
     * @return profile
     */
    public ThreadingProfile getThreadingProfile() {
        return threadingProfile;
    }

    /**
     * Sets the threading profile
     *
     * @param threadingProfile profile to set
     */
    public void setThreadingProfile(ThreadingProfile threadingProfile) {
        this.threadingProfile = threadingProfile;
    }
}
