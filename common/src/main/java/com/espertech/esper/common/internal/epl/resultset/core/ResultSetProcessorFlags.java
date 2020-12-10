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
package com.espertech.esper.common.internal.epl.resultset.core;

import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitLimitType;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitSpec;

public class ResultSetProcessorFlags {
    private final boolean join;
    private final OutputLimitSpec spec;
    private final ResultSetProcessorOutputConditionType outputConditionType;

    public ResultSetProcessorFlags(boolean join, OutputLimitSpec spec, ResultSetProcessorOutputConditionType outputConditionType) {
        this.join = join;
        this.spec = spec;
        this.outputConditionType = outputConditionType;
    }

    public boolean isJoin() {
        return join;
    }

    public boolean isHasOutputLimit() {
        return spec != null;
    }

    public ResultSetProcessorOutputConditionType getOutputConditionType() {
        return outputConditionType;
    }

    public boolean isOutputLimitWSnapshot() {
        return spec != null && spec.getDisplayLimit() == OutputLimitLimitType.SNAPSHOT;
    }

    public boolean isOutputLimitNoSnapshot() {
        return spec != null && spec.getDisplayLimit() != OutputLimitLimitType.SNAPSHOT;
    }
}
