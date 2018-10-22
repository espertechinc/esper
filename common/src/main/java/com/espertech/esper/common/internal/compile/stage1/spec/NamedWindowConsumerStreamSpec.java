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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.internal.epl.contained.PropertyEvaluatorForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;

import java.util.List;

/**
 * Specification for use of an existing named window.
 */
public class NamedWindowConsumerStreamSpec extends StreamSpecBase implements StreamSpecCompiled {
    private NamedWindowMetaData namedWindow;
    private List<ExprNode> filterExpressions;
    private PropertyEvaluatorForge optPropertyEvaluator;
    private int namedWindowConsumerId = -1;

    public NamedWindowConsumerStreamSpec(NamedWindowMetaData namedWindow, String optionalAsName, ViewSpec[] viewSpecs, List<ExprNode> filterExpressions, StreamSpecOptions streamSpecOptions, PropertyEvaluatorForge optPropertyEvaluator) {
        super(optionalAsName, viewSpecs, streamSpecOptions);
        this.namedWindow = namedWindow;
        this.filterExpressions = filterExpressions;
        this.optPropertyEvaluator = optPropertyEvaluator;
    }

    /**
     * Returns list of filter expressions onto the named window, or no filter expressions if none defined.
     *
     * @return list of filter expressions
     */
    public List<ExprNode> getFilterExpressions() {
        return filterExpressions;
    }

    public PropertyEvaluatorForge getOptPropertyEvaluator() {
        return optPropertyEvaluator;
    }

    public void setNamedWindowConsumerId(int namedWindowConsumerId) {
        this.namedWindowConsumerId = namedWindowConsumerId;
    }

    public int getNamedWindowConsumerId() {
        return namedWindowConsumerId;
    }

    public NamedWindowMetaData getNamedWindow() {
        return namedWindow;
    }
}
