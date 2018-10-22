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
package com.espertech.esper.regressionlib.support.bean;

import java.io.Serializable;

public class SupportSpatialPoint implements Serializable {
    private String id;
    private Double px;
    private Double py;
    private String category;

    public SupportSpatialPoint(String id, Double px, Double py) {
        this.id = id;
        this.px = px;
        this.py = py;
    }

    public SupportSpatialPoint(String id, Double px, Double py, String category) {
        this.id = id;
        this.px = px;
        this.py = py;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public Double getPx() {
        return px;
    }

    public Double getPy() {
        return py;
    }

    public String getCategory() {
        return category;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPx(Double px) {
        this.px = px;
    }

    public void setPy(Double py) {
        this.py = py;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
