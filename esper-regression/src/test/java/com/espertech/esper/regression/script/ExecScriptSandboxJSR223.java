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
package com.espertech.esper.regression.script;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.epl.script.jsr223.JSR223Helper;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import javax.script.*;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExecScriptSandboxJSR223 implements RegressionExecution {

    /**
     * MVEL does not support JSR 223.
     * Making MVEL an Esper compile-time dependency is not desired.
     * Script and MVEL performance comparison is not close and MVEL is faster.
     */
    public void run(EPServiceProvider epService) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        String expressionFib = "fib(num); function fib(n) { if(n <= 1) return n; return fib(n-1) + fib(n-2); };";
        String expressionTwo = "var words = new java.util.ArrayList();\n" +
                "words.add('wordOne');\n" +
                "words.add('wordTwo');\n" +
                "words;\n";
        Compilable compilingEngine = (Compilable) engine;
        CompiledScript script = null;
        try {
            script = compilingEngine.compile(expressionTwo);
        } catch (ScriptException ex) {
            throw new RuntimeException("Script compiler exception: " + JSR223Helper.getScriptCompileMsg(ex), ex);
        }

        Bindings bindings = engine.createBindings();
        bindings.put("epl", new MyEPLContext());

        Object result = script.eval(bindings);
        System.out.println(result + " typed " + (result != null ? result.getClass() : "null"));

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            script.eval(bindings);
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        System.out.println("delta=" + delta);
    }

    private static class MyEPLContext {
        public Long getVariable(String name) {
            return 50L;
        }
    }

    private static class MyScriptContext implements ScriptContext {

        public void setBindings(Bindings bindings, int scope) {
            System.out.println("setBindings " + bindings);
        }

        public Bindings getBindings(int scope) {
            System.out.println("getBindings scope=" + scope);
            return null;
        }

        public void setAttribute(String name, Object value, int scope) {
            System.out.println("setAttribute name=" + name);
        }

        public Object getAttribute(String name, int scope) {
            System.out.println("getAttribute name=" + name);
            return null;
        }

        public Object removeAttribute(String name, int scope) {
            System.out.println("removeAttribute name=" + name);
            return null;
        }

        public Object getAttribute(String name) {
            System.out.println("getAttribute name=" + name);
            return null;
        }

        public int getAttributesScope(String name) {
            System.out.println("getAttributesScope name=" + name);
            return 0;
        }

        public Writer getWriter() {
            System.out.println("getWriter");
            return null;
        }

        public Writer getErrorWriter() {
            System.out.println("getErrorWriter");
            return null;
        }

        public void setWriter(Writer writer) {
            System.out.println("setWriter");
        }

        public void setErrorWriter(Writer writer) {
            System.out.println("setErrorWriter");
        }

        public Reader getReader() {
            System.out.println("getReader");
            return null;
        }

        public void setReader(Reader reader) {
            System.out.println("setReader");
        }

        public List<Integer> getScopes() {
            System.out.println("getScopes");
            return null;
        }
    }

    public class MyBindings implements Bindings {
        public Object put(String name, Object value) {
            System.out.println("put");
            return null;
        }

        public void putAll(Map<? extends String, ? extends Object> toMerge) {
            System.out.println("putAll");
        }

        public boolean containsKey(Object key) {
            System.out.println("containsKey");
            return false;
        }

        public Object get(Object key) {
            System.out.println("get");
            return null;
        }

        public Object remove(Object key) {
            System.out.println("remove");
            return null;
        }

        public int size() {
            System.out.println("size");
            return 0;
        }

        public boolean isEmpty() {
            System.out.println("empty");
            return false;
        }

        public boolean containsValue(Object value) {
            System.out.println("containsValue");
            return false;
        }

        public void clear() {
            System.out.println("clear");
        }

        public Set<String> keySet() {
            System.out.println("keySet");
            return null;
        }

        public Collection<Object> values() {
            System.out.println("values");
            return null;
        }

        public Set<Entry<String, Object>> entrySet() {
            System.out.println("entrySet");
            return null;
        }
    }
}
