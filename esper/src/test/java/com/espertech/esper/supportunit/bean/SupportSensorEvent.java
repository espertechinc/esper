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
package com.espertech.esper.supportunit.bean;

import java.io.Serializable;

public class SupportSensorEvent implements Serializable {
    private int id;
    private String type;
    private String device;
    private double measurement;
    private double confidence;

    private SupportSensorEvent() {
    }

    public SupportSensorEvent(int id, String type, String device, double measurement, double confidence) {
        this.id = id;
        this.type = type;
        this.device = device;
        this.measurement = measurement;
        this.confidence = confidence;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDevice() {
        return device;
    }

    public double getMeasurement() {
        return measurement;
    }

    public double getConfidence() {
        return confidence;
    }


    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setMeasurement(double measurement) {
        this.measurement = measurement;
    }

    public void setType(String type) {
        this.type = type;
    }
}
