package com.espertech.esper.example.virtualdw;

public class SampleMergeEvent {
    private String propOne;
    private String propTwo;

    public SampleMergeEvent(String propOne, String propTwo) {
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
