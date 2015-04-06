package com.espertech.esper.example.namedwinquery;

import com.espertech.esper.client.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import org.junit.Assert;
import junit.framework.TestCase;

public class TestNamedWindowQueryMain extends TestCase
{
    public void testNamedWindowQuery()
    {
        NamedWindowQueryMain main = new NamedWindowQueryMain();
        main.runExample(true, "NamedWindowMain");
    }
}

