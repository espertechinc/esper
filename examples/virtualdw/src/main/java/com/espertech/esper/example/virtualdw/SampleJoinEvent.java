package com.espertech.esper.example.virtualdw;

public class SampleJoinEvent {
    private String propOne;
    private String propTwo;

    public SampleJoinEvent(String propOne, String propTwo) {
        this.propOne = propOne;
        this.propTwo = propTwo;
    }

    public String getPropOne() {
        return propOne;
    }

    public String getPropTwo() {
        return propTwo;
    }
}
