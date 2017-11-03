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
package com.espertech.esper.epl.spec;

import java.util.List;

/**
 * Specification for the merge statement.
 */
public class OnTriggerMergeDesc extends OnTriggerWindowDesc {
    private static final long serialVersionUID = 3388811105339812571L;

    private OnTriggerMergeActionInsert optionalInsertNoMatch;
    private List<OnTriggerMergeMatched> items;

    public OnTriggerMergeDesc(String windowName, String optionalAsName, OnTriggerMergeActionInsert optionalInsertNoMatch, List<OnTriggerMergeMatched> items) {
        super(windowName, optionalAsName, OnTriggerType.ON_MERGE, false);
        this.optionalInsertNoMatch = optionalInsertNoMatch;
        this.items = items;
    }

    public List<OnTriggerMergeMatched> getItems() {
        return items;
    }

    public OnTriggerMergeActionInsert getOptionalInsertNoMatch() {
        return optionalInsertNoMatch;
    }
}

