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
package com.espertech.esper.supportunit.bean;

public class SupportTimeStartEndA extends SupportTimeStartBase {

    public SupportTimeStartEndA(String key, String datestr, long duration) {
        super(key, datestr, duration);
    }

    public static SupportTimeStartEndA make(String key, String datestr, long duration) {
        return new SupportTimeStartEndA(key, datestr, duration);
    }
}
