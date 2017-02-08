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

import java.util.ArrayList;
import java.util.List;

public class ZoneFactory {

    public static Iterable<Zone> getZones() {
        List<Zone> zones = new ArrayList<Zone>();
        zones.add(new Zone("Z1", new Rectangle(0, 0, 20, 20)));
        return zones;
    }
}
