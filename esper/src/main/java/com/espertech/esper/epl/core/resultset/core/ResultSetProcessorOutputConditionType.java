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
package com.espertech.esper.epl.core.resultset.core;

import com.espertech.esper.epl.spec.OutputLimitLimitType;

public enum ResultSetProcessorOutputConditionType {
    SNAPSHOT,
    POLICY_FIRST,
    POLICY_LASTALL_UNORDERED,
    POLICY_NONFIRST;

    public static ResultSetProcessorOutputConditionType getConditionType(OutputLimitLimitType displayLimit, boolean isAggregated, boolean hasOrderBy, boolean hasOptHint, boolean isGrouped) {
        if (displayLimit == OutputLimitLimitType.SNAPSHOT) {
            return SNAPSHOT;
        } else if (displayLimit == OutputLimitLimitType.FIRST && !isGrouped) {
            // For FIRST without groups we are using a special logic that integrates the first-flag, in order to still conveniently use all sorts of output conditions.
            // FIRST with group-by is handled by setting the output condition to null (OutputConditionNull) and letting the ResultSetProcessor handle first-per-group.
            // Without having-clause there is no required order of processing, thus also use regular policy.
            return POLICY_FIRST;
        } else if (!isAggregated && !isGrouped && displayLimit == OutputLimitLimitType.LAST) {
            return POLICY_LASTALL_UNORDERED;
        } else if (hasOptHint && displayLimit == OutputLimitLimitType.ALL && !hasOrderBy) {
            return POLICY_LASTALL_UNORDERED;
        } else if (hasOptHint && displayLimit == OutputLimitLimitType.LAST && !hasOrderBy) {
            return POLICY_LASTALL_UNORDERED;
        } else {
            return POLICY_NONFIRST;
        }
    }
}
