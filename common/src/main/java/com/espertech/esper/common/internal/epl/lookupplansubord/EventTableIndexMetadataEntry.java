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
package com.espertech.esper.common.internal.epl.lookupplansubord;

import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;

import java.util.HashSet;
import java.util.Set;

public class EventTableIndexMetadataEntry extends EventTableIndexEntryBase {
    private final boolean primary;
    private final Set<String> referencedByDeployment;
    private final QueryPlanIndexItem optionalQueryPlanIndexItem; // not required at compile-time for explicit indexes
    private final String explicitIndexNameIfExplicit;
    private final String explicitIndexModuleNameIfExplicit;
    private final String deploymentId;

    public EventTableIndexMetadataEntry(String optionalIndexName, String optionalIndexModuleName, boolean primary, QueryPlanIndexItem optionalQueryPlanIndexItem, String explicitIndexNameIfExplicit, String explicitIndexModuleNameIfExplicit, String deploymentId) {
        super(optionalIndexName, optionalIndexModuleName);
        this.primary = primary;
        this.optionalQueryPlanIndexItem = optionalQueryPlanIndexItem;
        referencedByDeployment = primary ? null : new HashSet<>();
        this.explicitIndexNameIfExplicit = explicitIndexNameIfExplicit;
        this.explicitIndexModuleNameIfExplicit = explicitIndexModuleNameIfExplicit;
        this.deploymentId = deploymentId;
    }

    public void addReferringDeployment(String deploymentId) {
        if (!primary) {
            referencedByDeployment.add(deploymentId);
        }
    }

    public boolean removeReferringStatement(String deploymentId) {
        if (!primary) {
            referencedByDeployment.remove(deploymentId);
            if (referencedByDeployment.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean isPrimary() {
        return primary;
    }

    public String[] getReferringDeployments() {
        return referencedByDeployment.toArray(new String[referencedByDeployment.size()]);
    }

    public QueryPlanIndexItem getOptionalQueryPlanIndexItem() {
        return optionalQueryPlanIndexItem;
    }

    public String getExplicitIndexNameIfExplicit() {
        return explicitIndexNameIfExplicit;
    }

    public String getExplicitIndexModuleNameIfExplicit() {
        return explicitIndexModuleNameIfExplicit;
    }

    public String getDeploymentId() {
        return deploymentId;
    }
}
