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
package com.espertech.esper.regressionlib.support.filter;

public class SupportFilterPlan {
    private SupportFilterPlanPath[] paths;
    private String controlConfirm;
    private String controlNegate;

    public SupportFilterPlan(String controlConfirm, String controlNegate, SupportFilterPlanPath... paths) {
        this.paths = paths;
        this.controlConfirm = controlConfirm;
        this.controlNegate = controlNegate;
    }

    public SupportFilterPlan(SupportFilterPlanPath... paths) {
        this(null, null, paths);
    }

    public SupportFilterPlanPath[] getPaths() {
        return paths;
    }

    public void setPaths(SupportFilterPlanPath[] paths) {
        this.paths = paths;
    }

    public String getControlConfirm() {
        return controlConfirm;
    }

    public void setControlConfirm(String controlConfirm) {
        this.controlConfirm = controlConfirm;
    }

    public String getControlNegate() {
        return controlNegate;
    }

    public void setControlNegate(String controlNegate) {
        this.controlNegate = controlNegate;
    }
}
