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
package com.espertech.esper.common.internal.epl.ontrigger;

import java.util.List;

/**
 * Factory for handles for updates/inserts/deletes/select
 */
public class InfraOnMergeHelper {
    private final InfraOnMergeActionIns insertUnmatched;
    private final List<InfraOnMergeMatch> matched;
    private final List<InfraOnMergeMatch> unmatched;
    private final boolean requiresTableWriteLock;

    public InfraOnMergeHelper(InfraOnMergeActionIns insertUnmatched, List<InfraOnMergeMatch> matched, List<InfraOnMergeMatch> unmatched, boolean requiresTableWriteLock) {
        this.insertUnmatched = insertUnmatched;
        this.matched = matched;
        this.unmatched = unmatched;
        this.requiresTableWriteLock = requiresTableWriteLock;
    }

    public InfraOnMergeActionIns getInsertUnmatched() {
        return insertUnmatched;
    }

    public List<InfraOnMergeMatch> getMatched() {
        return matched;
    }

    public List<InfraOnMergeMatch> getUnmatched() {
        return unmatched;
    }

    public boolean isRequiresTableWriteLock() {
        return requiresTableWriteLock;
    }
}