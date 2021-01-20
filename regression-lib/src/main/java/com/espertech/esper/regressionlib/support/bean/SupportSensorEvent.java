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

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportSensorEvent implements Serializable {
    private static final long serialVersionUID = -3023868194284732488L;
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
