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
package com.espertech.esper.example.matchmaker.eventbean;

public class MobileUserBean {
    private int userId;
    private double locationX;
    private double locationY;
    private Gender myGender;
    private HairColor myHairColor;
    private AgeRange myAgeRange;
    private Gender preferredGender;
    private HairColor preferredHairColor;
    private AgeRange preferredAgeRange;

    public MobileUserBean(int userId, double locationX, double locationY, Gender myGender, HairColor myHairColor, AgeRange myAgeRange, Gender preferredGender, HairColor preferredHairColor, AgeRange preferredAgeRange) {
        this.userId = userId;
        this.locationX = locationX;
        this.locationY = locationY;
        this.myGender = myGender;
        this.myHairColor = myHairColor;
        this.myAgeRange = myAgeRange;
        this.preferredGender = preferredGender;
        this.preferredHairColor = preferredHairColor;
        this.preferredAgeRange = preferredAgeRange;
    }

    public int getUserId() {
        return userId;
    }

    public double getLocationX() {
        return locationX;
    }

    public double getLocationY() {
        return locationY;
    }

    public void setLocation(double locationX, double locationY) {
        this.locationX = locationX;
        this.locationY = locationY;
    }

    public void setLocationY(double locationY) {
        this.locationY = locationY;
    }

    public String getMyGender() {
        return myGender.toString();
    }

    public String getMyHairColor() {
        return myHairColor.toString();
    }

    public String getMyAgeRange() {
        return myAgeRange.toString();
    }

    public String getPreferredGender() {
        return preferredGender.toString();
    }

    public String getPreferredHairColor() {
        return preferredHairColor.toString();
    }

    public String getPreferredAgeRange() {
        return preferredAgeRange.toString();
    }
}
