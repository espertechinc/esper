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
package com.espertech.esper.supportregression.bean;

public class SupportTimeStartEndB extends SupportTimeStartBase {

    public SupportTimeStartEndB(String key, String datestr, long duration) {
        super(key, datestr, duration);
    }

    public static SupportTimeStartEndB make(String key, String datestr, long duration) {
        return new SupportTimeStartEndB(key, datestr, duration);
    }
}

