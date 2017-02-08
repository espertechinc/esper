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

public class Item {

    private String assetId;
    private Location location;
    private String type;
    private String assetIdPassenger;

    public Item(String assetId, Location location) {
        this.assetId = assetId;
        this.location = location;
    }

    public Item(String assetId, Location location, String type, String assetIdPassenger) {
        this.assetId = assetId;
        this.location = location;
        this.type = type;
        this.assetIdPassenger = assetIdPassenger;
    }

    public String getType() {
        return type;
    }

    public String getAssetIdPassenger() {
        return assetIdPassenger;
    }

    public String getAssetId() {
        return assetId;
    }

    public Location getLocation() {
        return location;
    }
}
