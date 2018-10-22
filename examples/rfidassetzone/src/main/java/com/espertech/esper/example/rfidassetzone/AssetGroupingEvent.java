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

public class AssetGroupingEvent {
    private final int groupId;
    private final String[] assetIdsGrouped;

    public AssetGroupingEvent(int groupId, String[] assetIdsGrouped) {
        this.groupId = groupId;
        this.assetIdsGrouped = assetIdsGrouped;
    }

    public int getGroupId() {
        return groupId;
    }

    public String[] getAssetIdsGrouped() {
        return assetIdsGrouped;
    }
}
