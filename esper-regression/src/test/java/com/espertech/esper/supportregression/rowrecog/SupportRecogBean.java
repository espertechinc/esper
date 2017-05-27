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
package com.espertech.esper.supportregression.rowrecog;

public class SupportRecogBean {
    private String theString;
    private int value;
    private String cat;

    public SupportRecogBean(String theString) {
        this.theString = theString;
    }

    public SupportRecogBean(String theString, int value) {
        this.theString = theString;
        this.value = value;
    }

    public SupportRecogBean(String theString, String cat, int value) {
        this.theString = theString;
        this.cat = cat;
        this.value = value;
    }

    public String getTheString() {
        return theString;
    }

    public String getCat() {
        return cat;
    }

    public void setCat(String cat) {
        this.cat = cat;
    }

    public void setTheString(String theString) {
        this.theString = theString;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String toString() {
        return theString;
    }
}
