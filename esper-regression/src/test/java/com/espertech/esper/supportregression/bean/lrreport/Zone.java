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
package com.espertech.esper.supportregression.bean.lrreport;

public class Zone {

    private final String name;
    private final Rectangle rectangle;

    public Zone(String name, Rectangle rectangle) {
        this.name = name;
        this.rectangle = rectangle;
    }

    public String getName() {
        return name;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public static String[] getZoneNames() {
        return new String[]{"Z1", "Z2"};
    }
}
