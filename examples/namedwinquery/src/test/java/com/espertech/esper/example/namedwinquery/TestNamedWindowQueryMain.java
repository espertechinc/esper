package com.espertech.esper.example.namedwinquery;

import junit.framework.TestCase;

public class TestNamedWindowQueryMain extends TestCase {
    public void testNamedWindowQuery() {
        NamedWindowQueryMain main = new NamedWindowQueryMain();
        main.runExample(true, "NamedWindowMain");
    }
}

