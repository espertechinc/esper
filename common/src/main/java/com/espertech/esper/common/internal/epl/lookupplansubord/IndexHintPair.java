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

import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerWindowDesc;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.common.internal.epl.join.hint.IndexHint;

public class IndexHintPair {
    private final IndexHint indexHint;
    private final ExcludePlanHint excludePlanHint;

    public IndexHintPair(IndexHint indexHint, ExcludePlanHint excludePlanHint) {
        this.indexHint = indexHint;
        this.excludePlanHint = excludePlanHint;
    }

    public IndexHint getIndexHint() {
        return indexHint;
    }

    public ExcludePlanHint getExcludePlanHint() {
        return excludePlanHint;
    }

    public static IndexHintPair getIndexHintPair(OnTriggerDesc onTriggerDesc, String streamZeroAsName, StatementRawInfo statementRawInfo, StatementCompileTimeServices services)
            throws ExprValidationException {
        IndexHint indexHint = IndexHint.getIndexHint(statementRawInfo.getAnnotations());
        ExcludePlanHint excludePlanHint = null;
        if (onTriggerDesc instanceof OnTriggerWindowDesc) {
            OnTriggerWindowDesc onTriggerWindowDesc = (OnTriggerWindowDesc) onTriggerDesc;
            String[] streamNames = {onTriggerWindowDesc.getOptionalAsName(), streamZeroAsName};
            excludePlanHint = ExcludePlanHint.getHint(streamNames, statementRawInfo, services);
        }
        return new IndexHintPair(indexHint, excludePlanHint);
    }
}
