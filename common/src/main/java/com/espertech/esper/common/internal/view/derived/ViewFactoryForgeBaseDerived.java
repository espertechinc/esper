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
package com.espertech.esper.common.internal.view.derived;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.view.core.ViewFactoryForgeBase;

import java.util.List;

public abstract class ViewFactoryForgeBaseDerived extends ViewFactoryForgeBase {
    protected List<ExprNode> viewParameters;
    protected StatViewAdditionalPropsForge additionalProps;

    public List<ExprNode> getViewParameters() {
        return viewParameters;
    }

    public StatViewAdditionalPropsForge getAdditionalProps() {
        return additionalProps;
    }
}
