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
package com.espertech.esper.example.rfidassetzone;

public class LocationReport {
    private String assetId;
    private int zone;

    public LocationReport(String assetId, int zone) {
        this.assetId = assetId;
        this.zone = zone;
    }

    public String getAssetId() {
        return assetId;
    }

    public int getZone() {
        return zone;
    }
}
