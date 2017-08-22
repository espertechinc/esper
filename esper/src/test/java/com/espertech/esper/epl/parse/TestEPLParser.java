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
package com.espertech.esper.epl.parse;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.epl.parse.SupportEPLTreeWalkerFactory;
import com.espertech.esper.supportunit.epl.parse.SupportParserHelper;
import junit.framework.TestCase;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestEPLParser extends TestCase {

    public void testDisplayAST() throws Exception {
        String expression = "select * from A where exp > ANY (select a from B)";

        log.debug(".testDisplayAST parsing: " + expression);
        Pair<Tree, CommonTokenStream> ast = parse(expression);
        SupportParserHelper.displayAST(ast.getFirst());

        log.debug(".testDisplayAST walking...");
        EPLTreeWalkerListener listener = SupportEPLTreeWalkerFactory.makeWalker(ast.getSecond());

        ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
        walker.walk(listener, (ParseTree) ast.getFirst()); // initiate walk of tree with listener
    }

    public void testInvalidCases() throws Exception {
        String className = SupportBean.class.getName();

        assertIsInvalid(className + "(val=10000).");
        assertIsInvalid("select * from com.xxx().std:win(3) where a not is null");
        assertIsInvalid(className + "().a:someview");
        assertIsInvalid(className + "().a:someview(");
        assertIsInvalid(className + "().a:someview)");
        assertIsInvalid(className + "().lenght()");
        assertIsInvalid(className + "().:lenght()");
        assertIsInvalid(className + "().win:lenght(0,)");
        assertIsInvalid(className + "().win:lenght(,0)");
        assertIsInvalid(className + "().win:lenght(0,0,)");
        assertIsInvalid(className + "().win:lenght(0,0,\")");
        assertIsInvalid(className + "().win:lenght(\"\"5)");
        assertIsInvalid(className + "().win:lenght(,\"\")");
        assertIsInvalid(className + "().win:lenght.(,\"\")");
        assertIsInvalid(className + "().win:lenght().");
        assertIsInvalid(className + "().win:lenght().lenght");
        assertIsInvalid(className + "().win:lenght().lenght(");
        assertIsInvalid(className + "().win:lenght().lenght)");
        assertIsInvalid(className + "().win:lenght().lenght().");
        assertIsInvalid(className + "().win:lenght().lenght().lenght");
        assertIsInvalid(className + "().win:lenght({}))");
        assertIsInvalid(className + "().win:lenght({\"s\")");
        assertIsInvalid(className + "().win:lenght(\"s\"})");
        assertIsInvalid(className + "().win:lenght({{\"s\"})");
        assertIsInvalid(className + "().win:lenght({{\"s\"}})");
        assertIsInvalid(className + "().win:lenght({\"s\"}})");
        assertIsInvalid(className + "().win:lenght('s\"");
        assertIsInvalid(className + "().win:lenght(\"s')");

        assertIsInvalid("select * from A.std:win(3) where a not is null");
        assertIsInvalid("select * from com.xxx().std:win(3) where a = not null");
        assertIsInvalid("select * from com.xxx().std:win(3) where not not");
        assertIsInvalid("select * from com.xxx().std:win(3) where not ||");
        assertIsInvalid("select * from com.xxx().std:win(3) where a ||");
        assertIsInvalid("select * from com.xxx().std:win(3) where || a");

        assertIsInvalid("select a] from com.xxx().std:win(3)");
        assertIsInvalid("select * from com.xxx().std:win(3) where b('aaa)=5");

        assertIsInvalid("select sum() from b#length(1)");
        assertIsInvalid("select sum(1+) from b#length(1)");
        assertIsInvalid("select sum(distinct) from b#length(1)");
        assertIsInvalid("select sum(distinct distinct a) from b#length(1)");
        assertIsInvalid("select avg() from b#length(1)");
        assertIsInvalid("select count() from b#length(1)");
        assertIsInvalid("select count(* *) from b#length(1)");
        assertIsInvalid("select count(*2) from b#length(1)");
        assertIsInvalid("select median() from b#length(1)");
        assertIsInvalid("select stddev() from b#length(1)");
        assertIsInvalid("select stddev(distinct) from b#length(1)");
        assertIsInvalid("select avedev() from b#length(1)");
        assertIsInvalid("select avedev(distinct) from b#length(1)");

        // group-by
        assertIsInvalid("select 1 from b#length(1) group by");
        assertIsInvalid("select 1 from b#length(1) group by group");
        assertIsInvalid("select 1 from b#length(1) group a");
        assertIsInvalid("select 1 from b#length(1) group by a group by b");
        assertIsInvalid("select 1 from b#length(1) by a ");
        assertIsInvalid("select 1 from b#length(1) group by a a");
        assertIsInvalid("select 1 from b#length(1) group by a as dummy");

        // having
        assertIsInvalid("select 1 from b#length(1) group by a having a>5,b<4");

        // insert into
        assertIsInvalid("insert into select 1 from b#length(1)");
        assertIsInvalid("insert into 38484 select 1 from b#length(1)");
        assertIsInvalid("insert into A B select 1 from b#length(1)");
        assertIsInvalid("insert into A (a,) select 1 from b#length(1)");
        assertIsInvalid("insert into A (,) select 1 from b#length(1)");
        assertIsInvalid("insert into A(,a) select 1 from b#length(1)");
        assertIsInvalid("insert xxx into A(,a) select 1 from b#length(1)");

        assertIsInvalid("select coalesce(processTimeEvent.price) from x");

        // time periods
        assertIsInvalid("select * from x#time(sec 99)");
        assertIsInvalid("select * from x#time(99 min min)");
        assertIsInvalid("select * from x#time(88 sec day)");
        assertIsInvalid("select * from x#time(1 sec 88 days)");
        assertIsInvalid("select * from x#time(1 day 2 hours 1 day)");

        // in
        assertIsInvalid("select * from x where a in()");
        assertIsInvalid("select * from x where a in(a,)");
        assertIsInvalid("select * from x where a in(,a)");
        assertIsInvalid("select * from x where a in(, ,)");
        assertIsInvalid("select * from x where a in not(1,2)");

        // between
        assertIsInvalid("select * from x where between a");
        assertIsInvalid("select * from x where between and b");
        assertIsInvalid("select * from x where between in and b");
        assertIsInvalid("select * from x where between");

        // like and regexp
        assertIsInvalid("select * from x where like");
        assertIsInvalid("select * from x where like escape");
        assertIsInvalid("select * from x where like a escape");
        assertIsInvalid("select * from x where order");
        assertIsInvalid("select * from x where field rlike 'aa' escape '!'");
        assertIsInvalid("select * from x where field regexp 'aa' escape '!'");
        assertIsInvalid("select * from x where regexp 'aa'");
        assertIsInvalid("select * from x where a like b escape c");

        // database join
        assertIsInvalid("select * from x, sql ");
        assertIsInvalid("select * from x, sql:xx ");
        assertIsInvalid("select * from x, sql:xx ");
        assertIsInvalid("select * from x, sql:xx [' dsfsdf \"]");
        assertIsInvalid("select * from x, sql:xx [\"sfsf ']");

        // Previous and prior function
        assertIsInvalid("select prior(10) from x");
        assertIsInvalid("select prior(price, a*b) from x");

        // subqueries
        assertIsInvalid("select (select a) from x");
        assertIsInvalid("select (select a from X, Y) from x");
        assertIsInvalid("select (select a from ) from x");
        assertIsInvalid("select (select from X) from x");
        assertIsInvalid("select * from x where (select q from pattern [A->B])");
        assertIsInvalid("select c from A where q*9 in in (select g*5 from C#length(100)) and r=6");
        assertIsInvalid("select c from A in (select g*5 from C#length(100)) and r=6");
        assertIsInvalid("select c from A where a in (select g*5 from C#length(100)) 9");

        // Substitution parameters
        assertIsInvalid("select ? ? from A");
        assertIsInvalid("select * from A(??)");

        // cast, instanceof, isnumeric and exists dynamic property
        assertIsInvalid("select * from A(boolean = exists(a, b))");
        assertIsInvalid("select * from A (boolean = exists())");
        assertIsInvalid("select * from A (boolean = exists(1))");
        assertIsInvalid("select * from A where exists(1 + a.b.c?.d.e)");
        assertIsInvalid("select * from A(boolean = instanceof(, a))");
        assertIsInvalid("select * from A(boolean = instanceof(b))");
        assertIsInvalid("select * from A(boolean = instanceof('agc', ,))");
        assertIsInvalid("select * from A(boolean = instanceof(b com.espertech.esper.support.AClass))");
        assertIsInvalid("select * from A(cast(b, +1))");
        assertIsInvalid("select * from A(cast(b?, a + 1))");
        assertIsInvalid("select * from A(cast((), a + 1))");

        // named window
        assertIsInvalid("create window AAA as MyType B");
        assertIsInvalid("create window AAA as select from MyType");
        assertIsInvalid("create window AAA as , *, b from MyType");
        assertIsInvalid("create window as select a from MyType");
        assertIsInvalid("create window AAA as select from MyType");
        assertIsInvalid("create window AAA#length(10)");
        assertIsInvalid("create window AAA");
        assertIsInvalid("create window AAA as select a*5 from MyType");

        // on-delete statement
        assertIsInvalid("on MyEvent from MyNamedWindow");
        assertIsInvalid("on  delete from MyNamedWindow");
        assertIsInvalid("on MyEvent abc def delete from MyNamedWindow");
        assertIsInvalid("on MyEvent(a<2)(a) delete from MyNamedWindow");
        assertIsInvalid("on MyEvent delete from MyNamedWindow where");

        // on-select statement
        assertIsInvalid("on MyEvent select from MyNamedWindow");
        assertIsInvalid("on MyEvent select * from MyNamedWindow#time(30)");
        assertIsInvalid("on MyEvent select * from MyNamedWindow where");
        assertIsInvalid("on MyEvent insert into select * from MyNamedWindow");
        assertIsInvalid("on MyEvent select a,c,b where a=y select 1,2,2,2 where 2=4");
        assertIsInvalid("on MyEvent insert into A select a,c,b where a=y select 1,2,2,2 where 2=4");
        assertIsInvalid("on MyEvent insert into A select a,c,b where a=y insert into D where 2=4");
        assertIsInvalid("on MyEvent insert into A select a,c,b where a=y insert into D where 2=4 output xyz");
        assertIsInvalid("on MyEvent insert into A select a,c,b where a=y insert into D where 2=4 output");

        // on-set statement
        assertIsInvalid("on MyEvent set");
        assertIsInvalid("on MyEvent set a=dkdkd a");
        assertIsInvalid("on MyEvent set a=, b=");

        // on-update statement
        assertIsInvalid("on MyEvent update ABC as abc");
        assertIsInvalid("on MyEvent update ABC set");
        assertIsInvalid("on pattern[every B] update ABC as abc set a=");
    }

    public void testValidCases() throws Exception {
        String className = SupportBean.class.getName();
        String preFill = "select * from " + className;

        // output rate limiting
        assertIsValid("select a from B output snapshot every 1 milliseconds");
        assertIsValid("select a from B output snapshot every 1 millisecond");
        assertIsValid("select a from B output snapshot every 1 msec");
        assertIsValid("select a from B output snapshot every 10 seconds");
        assertIsValid("select a from B output snapshot every 10 second");
        assertIsValid("select a from B output snapshot every 10 sec");
        assertIsValid("select a from B output snapshot every 3 minutes");
        assertIsValid("select a from B output snapshot every 3 minute");
        assertIsValid("select a from B output snapshot every 3 min");
        assertIsValid("select a from B output snapshot every 3 hours");
        assertIsValid("select a from B output snapshot every 3 hour");
        assertIsValid("select a from B output snapshot every 3 days");
        assertIsValid("select a from B output snapshot every 3 day");
        assertIsValid("select a from B output snapshot every 1 day 2 hours 3 minutes 4 seconds 5 milliseconds");
        assertIsValid("select a from B output first every 5 events");
        assertIsValid("select a from B output snapshot at (123, 333, 33, 33, 3)");
        assertIsValid("select a from B output snapshot at (*, *, *, *, *)");
        assertIsValid("select a from B output snapshot when myvar*count > 10");
        assertIsValid("select a from B output snapshot when myvar*count > 10 then set myvar = 1, myvar2 = 2*5");

        assertIsValid(preFill + "(string='test',intPrimitive=20).win:lenght(100)");
        assertIsValid(preFill + "(string in ('b', 'a'))");
        assertIsValid(preFill + "(string in ('b'))");
        assertIsValid(preFill + "(string in ('b', 'c', 'x'))");
        assertIsValid(preFill + "(string in [1:2))");
        assertIsValid(preFill + "(string in [1:2])");
        assertIsValid(preFill + "(string in (1:2))");
        assertIsValid(preFill + "(string in (1:2])");
        assertIsValid(preFill + "(intPrimitive = 08)");
        assertIsValid(preFill + "(intPrimitive = 09)");
        assertIsValid(preFill + "(intPrimitive = 008)");
        assertIsValid(preFill + "(intPrimitive = 0008)");
        assertIsValid(preFill + "(intPrimitive between 1 and 2)");
        assertIsValid(preFill + "(intPrimitive not between 1 and 2)");
        assertIsValid(preFill + "(intPrimitive not in [1:2])");
        assertIsValid(preFill + "(intPrimitive not in (1, 2, 3))");
        assertIsValid(preFill + "().win:lenght()");
        assertIsValid(preFill + "().win:lenght(4,5)");
        assertIsValid(preFill + "().win:lenght(4)");
        assertIsValid(preFill + "().win:lenght(\"\",5)");
        assertIsValid(preFill + "().win:lenght(10.9,1E30,-4.4,\"\",5)");
        assertIsValid(preFill + "().win:lenght(4).n:c(3.3, -3.3).n:other(\"price\")");
        assertIsValid(preFill + "().win:lenght().n:c().n:da().n:e().n:f().n:g().n:xh(2.0)");
        assertIsValid(preFill + "().win:lenght({\"s\"})");
        assertIsValid(preFill + "().win:lenght({\"a\",\"b\"})");
        assertIsValid(preFill + "().win:lenght({\"a\",\"b\",\"c\"})");
        assertIsValid(preFill + "().win:lenght('')");
        assertIsValid(preFill + "().win:lenght('s')");
        assertIsValid(preFill + "().win:lenght('s',5)");
        assertIsValid(preFill + "().win:lenght({'s','t'},5)");
        assertIsValid(preFill + "().win:some_window('count','l','a').win:lastevent('s','tyr')");
        assertIsValid(preFill + "().win:some_view({'count'},'l','a')");
        assertIsValid(preFill + "().win:some_view({})");
        assertIsValid(preFill + "(string != 'test').win:lenght(100)");
        assertIsValid(preFill + "(string in (1:2) or katc=3 or lax like '%e%')");
        assertIsValid(preFill + "(string in (1:2) and dodo=3, lax like '%e%' and oppol / yyy = 5, yunc(3))");
        assertIsValid(preFill + "()[myprop]");
        assertIsValid(preFill + "[myprop]#keepall");
        assertIsValid(preFill + "[myprop as orderId][mythirdprop]#keepall");
        assertIsValid(preFill + "[select *, abc, a.b from myprop as orderId where a=s][mythirdprop]#keepall");
        assertIsValid(preFill + "[xyz][select *, abc, a.b from myprop]#keepall");
        assertIsValid(preFill + "[xyz][myprop where a=x]#keepall");
        assertIsValid("select * from A where (select * from B[myprop])");

        assertIsValid("select max(intPrimitive, intBoxed) from " + className + "().std:win(20)");
        assertIsValid("select max(intPrimitive, intBoxed, longBoxed) from " + className + "().std:win(20)");
        assertIsValid("select min(intPrimitive, intBoxed) from " + className + "().std:win(20)");
        assertIsValid("select min(intPrimitive, intBoxed, longBoxed) from " + className + "().std:win(20)");

        assertIsValid(preFill + "().win:lenght(3) where a = null");
        assertIsValid(preFill + "().win:lenght(3) where a is null");
        assertIsValid(preFill + "().win:lenght(3) where 10 is a");
        assertIsValid(preFill + "().win:lenght(3) where 10 is not a");
        assertIsValid(preFill + "().win:lenght(3) where 10 <> a");
        assertIsValid(preFill + "().win:lenght(3) where a <> 10");
        assertIsValid(preFill + "().win:lenght(3) where a != 10");
        assertIsValid(preFill + "().win:lenght(3) where 10 != a");
        assertIsValid(preFill + "().win:lenght(3) where not (a = 5)");
        assertIsValid(preFill + "().win:lenght(3) where not (a = 5 or b = 3)");
        assertIsValid(preFill + "().win:lenght(3) where not 5 < 4");
        assertIsValid(preFill + "().win:lenght(3) where a or (not b)");
        assertIsValid(preFill + "().win:lenght(3) where a % 3 + 6 * (c%d)");
        assertIsValid(preFill + "().win:lenght(3) where a || b = 'a'");
        assertIsValid(preFill + "().win:lenght(3) where a || b || c = 'a'");
        assertIsValid(preFill + "().win:lenght(3) where a + b + c = 'a'");

        assertIsValid("select not a, not (b), not (a > 5) from " +
                className + "(a=1).win:lenght(10) as win1," +
                className + "(a=2).win:lenght(10) as win2 " +
                "where win1.f1 = win2.f2"
        );

        assertIsValid("select intPrimitive from " +
                className + "(a=1).win:lenght(10) as win1," +
                className + "(a=2).win:lenght(10) as win2 " +
                "where win1.f1 = win2.f2"
        );

        // outer joins
        tryJoin("left");
        tryJoin("right");
        tryJoin("full");
        assertIsValid("select * from A left outer join B on a = b and c=d");
        assertIsValid("select * from A left outer join B on a = b and c=d inner join C on d=c");

        // complex property access
        assertIsValid("select array[1], map('a'), map(\"b\"), nested.nested " +
                "from a.b(string='test',intPrimitive=20).win:lenght(100) " +
                "where array[1].map('a').nested = 5");
        assertIsValid("select array[1] as b " +
                "from a.b(string[0]='test').win:lenght(100) as x " +
                "left outer join " +
                "a.b(string[0]='test').win:lenght(100) as y " +
                "on y.array[1].map('a').nested = x.nested2");
        assertIsValid("select * " +
                "from A " +
                "left outer join " +
                "B" +
                " on a = b and c=d");
        assertIsValid("select a and b from b#length(1)");
        assertIsValid("select a or b from b#length(1)");
        assertIsValid("select a = b from b#length(1)");
        assertIsValid("select a != b from b#length(1)");
        assertIsValid("select a.* from b#length(1) as a");
        assertIsValid("select a.* as myfield from b#length(1) as abc");
        assertIsValid("select a.*, b.*, c.* from b#length(1) as a");
        assertIsValid("select a.* as x1, b.* as x2, x.* as x3 from b#length(1) as a, t as x");

        assertIsValid("select sum(a), avg(b) from b#length(1)");
        assertIsValid("select sum(all a), avg(all b), avg(all b/c) from b#length(1)");
        assertIsValid("select sum(distinct a), avg(distinct b) from b#length(1)");
        assertIsValid("select sum(sum(a)) from b#length(1)");
        assertIsValid("select sum(3*a), sum(a - b - c) from b#length(1)");
        assertIsValid("select count(*), count(a), count(all b), count(distinct 2*a), count(5*a/2) from b#length(1)");
        assertIsValid("select max(volume), min(volume), min(all volume/44), min(distinct 2*a), max(distinct 5*a/2) from b#length(1)");
        assertIsValid("select median(volume), median(all volume*2/3), median(distinct 2*a) from b#length(1)");
        assertIsValid("select stddev(volume), stddev(all volume), stddev(distinct 2*a) from b#length(1)");
        assertIsValid("select avedev(volume), avedev(all volume), avedev(distinct 2*a) from b#length(1)");

        // group-by
        assertIsValid("select sum(a), x, y from b#length(1) group by a");
        assertIsValid("select 1 from b#length(1) where a=b and b=d group by a,b,3*x,max(4, 3),'a', \"a\", true, 5*(1+a+y/2)");
        assertIsValid("select 1 from b#length(1) where a"); // since a could be a boolean
        assertIsValid("select sum(distinct a), x, y from b#length(1) group by a");

        // having
        assertIsValid("select sum(a), x, y from b#length(1) group by a having x > y");
        assertIsValid("select 1 from b#length(1) where a=b and b=d group by a having (max(3*b - 2, 5) > 1) or 'a'=b");
        assertIsValid("select 1 from b#length(1) group by a having a");   // a could be boolean
        assertIsValid("select 1 from b#length(1) having a>5");
        assertIsValid("SELECT 1 FROM b#length(1) WHERE a=b AND b=d GROUP BY a HAVING (max(3*b - 2, 5) > 1) OR 'a'=b");

        // insert into
        assertIsValid("insert into MyEvent select 1 from b#length(1)");
        assertIsValid("insert into MyEvent (a) select 1 from b#length(1)");
        assertIsValid("insert into MyEvent (a, b) select 1 from b#length(1)");
        assertIsValid("insert into MyEvent (a, b, c) select 1 from b#length(1)");
        assertIsValid("insert istream into MyEvent select 1 from b#length(1)");
        assertIsValid("insert rstream into MyEvent select 1 from b#length(1)");

        // pattern inside
        assertIsValid("select * from pattern [a=" + SupportBean.class.getName() + "]");
        assertIsValid("select * from pattern [a=" + SupportBean.class.getName() + "] as xyz");
        assertIsValid("select * from pattern [a=" + SupportBean.class.getName() + "]#length(100) as xyz");
        assertIsValid("select * from pattern [a=" + SupportBean.class.getName() + "]#length(100)#someview() as xyz");
        assertIsValid("select * from xxx");
        assertIsValid("select rstream * from xxx");
        assertIsValid("select istream * from xxx");
        assertIsValid("select rstream 1, 2 from xxx");
        assertIsValid("select istream 1, 2 from xxx");

        // coalesce
        assertIsValid("select coalesce(processTimeEvent.price, 0) from x");
        assertIsValid("select coalesce(processTimeEvent.price, null, -1) from x");
        assertIsValid("select coalesce(processTimeEvent.price, processTimeEvent.price, processTimeEvent.price, processTimeEvent.price) from x");

        // time intervals
        assertIsValid("select * from x#time(1 seconds)");
        assertIsValid("select * from x#time(1.5 second)");
        assertIsValid("select * from x#time(120230L sec)");
        assertIsValid("select * from x#time(1.5d milliseconds)");
        assertIsValid("select * from x#time(1E30 millisecond)");
        assertIsValid("select * from x#time(1.0 msec)");
        assertIsValid("select * from x#time(1.5d microseconds)");
        assertIsValid("select * from x#time(1 usec)");
        assertIsValid("select * from x#time(101L microsecond)");
        assertIsValid("select * from x#time(0001 minutes)");
        assertIsValid("select * from x#time(.1 minute)");
        assertIsValid("select * from x#time(1.1111001 min)");
        assertIsValid("select * from x#time(5 hours)");
        assertIsValid("select * from x#time(5 hour)");
        assertIsValid("select * from x#time(5 days)");
        assertIsValid("select * from x#time(5 day)");
        assertIsValid("select * from x#time(3 years 1 month 2 weeks 5 days 2 hours 88 minutes 1 seconds 9.8 milliseconds)");
        assertIsValid("select * from x#time(3 years 1 month 2 weeks 5 days 2 hours 88 minutes 1 seconds 9.8 milliseconds 1001 microseconds)");
        assertIsValid("select * from x#time(5 days 2 hours 88 minutes 1 seconds 9.8 milliseconds)");
        assertIsValid("select * from x#time(5 day 2 hour 88 minute 1 second 9.8 millisecond)");
        assertIsValid("select * from x#time(5 days 2 hours 88 minutes 1 seconds)");
        assertIsValid("select * from x#time(5 days 2 hours 88 minutes)");
        assertIsValid("select * from x#time(5 days 2 hours)");
        assertIsValid("select * from x#time(2 hours 88 minutes 1 seconds 9.8 milliseconds)");
        assertIsValid("select * from x#time(2 hours 88 minutes 1 seconds)");
        assertIsValid("select * from x#time(2 hours 88 minutes)");
        assertIsValid("select * from x#time(88 minutes 1 seconds 9.8 milliseconds)");
        assertIsValid("select * from x#time(88 minutes 1 seconds)");
        assertIsValid("select * from x#time(1 seconds 9.8 milliseconds)");
        assertIsValid("select * from x#time(1 seconds 9.8 milliseconds)#goodie(1 sec)");
        assertIsValid("select * from x#time(1 seconds 9.8 milliseconds)#win:goodie(1 sec)#win:otto(1.1 days 1.1 msec)");

        // in
        assertIsValid("select * from x where a in('a')");
        assertIsValid("select * from x where abc in ('a', 'b')");
        assertIsValid("select * from x where abc in (8*2, 1.001, 'a' || 'b', coalesce(0,null), null)");
        assertIsValid("select * from x where abc in (sum(x), max(2,2), true)");
        assertIsValid("select * from x where abc in (y,z, y+z)");
        assertIsValid("select * from x where abc not in (1)");
        assertIsValid("select * from x where abc not in (1, 2, 3)");
        assertIsValid("select * from x where abc*2/dog not in (1, 2, 3)");

        // between
        assertIsValid("select * from x where abc between 1 and 10");
        assertIsValid("select * from x where abc between 'a' and 'x'");
        assertIsValid("select * from x where abc between 1.1 and 1E1000");
        assertIsValid("select * from x where abc between a and b");
        assertIsValid("select * from x where abc between a*2 and sum(b)");
        assertIsValid("select * from x where abc*3 between a*2 and sum(b)");

        // custom aggregation func
        assertIsValid("select myfunc(price) from x");

        // like and regexp
        assertIsValid("select * from x where abc like 'dog'");
        assertIsValid("select * from x where abc like '_dog'");
        assertIsValid("select * from x where abc like '%dog'");
        assertIsValid("select * from x where abc like null");
        assertIsValid("select * from x where abc like '%dog' escape '\\\\'");
        assertIsValid("select * from x where abc like '%dog%' escape '!'");
        assertIsValid("select * from x where abc like '%dog' escape \"a\"");
        assertIsValid("select * from x where abc||'hairdo' like 'dog'");
        assertIsValid("select * from x where abc not like 'dog'");
        assertIsValid("select * from x where abc not regexp '[a-z]'");
        assertIsValid("select * from x where abc regexp '[a-z]'");
        assertIsValid("select * from x where a like b escape 'aa'");

        // database joins
        assertIsValid("select * from x, sql:mydb [\"whetever SQL $x.id google\"]");
        assertIsValid("select * from x, sql:mydb ['whetever SQL $x.id google']");
        assertIsValid("select * from x, sql:mydb ['']");
        assertIsValid("select * from x, sql:mydb ['   ']");
        assertIsValid("select * from x, sql:mydb ['whetever SQL $x.id google' metadatasql 'select 1 as myint']");

        // Previous and prior function
        assertIsValid("select prev(10, price) from x");
        assertIsValid("select prev(0, price) from x");
        assertIsValid("select prev(1000, price) from x");
        assertIsValid("select prev(index, price) from x");
        assertIsValid("select prior(10, price) from x");
        assertIsValid("select prior(0, price) from x");
        assertIsValid("select prior(1000, price) from x");
        assertIsValid("select prior(2, symbol) from x");

        // array constants and expressions
        assertIsValid("select {'a', 'b'} from x");
        assertIsValid("select {'a'} from x");
        assertIsValid("select {} from x");
        assertIsValid("select {'a', 'b'} as yyy from x");
        assertIsValid("select * from x where MyFunc.func({1,2}, xx)");
        assertIsValid("select {1,2,3} from x");
        assertIsValid("select {1.1,'2',3E5, 7L} from x");
        assertIsValid("select * from x where oo = {1,2,3}");
        assertIsValid("select {a, b}, {c, d} from x");

        // subqueries
        assertIsValid("select (select a from B) from x");
        assertIsValid("select (select a, b,c from B) from x");
        assertIsValid("select (select a||b||c from B) from x");
        assertIsValid("select (select 3*222 from B) from x");
        assertIsValid("select (select 3*222 from B#length(100)) from x");
        assertIsValid("select (select x from B#length(100) where a=b) from x");
        assertIsValid("select (select x from B#length(100) where a=b), (select y from C.w:g().e:o(11)) from x");
        assertIsValid("select 3 + (select a from B) from x");
        assertIsValid("select (select x from B) / 100, 9 * (select y from C.w:g().e:o(11))/2 from x");
        assertIsValid("select * from x where id = (select a from B)");
        assertIsValid("select * from x where id = -1 * (select a from B)");
        assertIsValid("select * from x where id = (5-(select a from B))");
        assertIsValid("select * from X where (select a from B where X.f = B.a) or (select a from B where X.f = B.c)");
        assertIsValid("select * from X where exists (select * from B where X.f = B.a)");
        assertIsValid("select * from X where exists (select * from B)");
        assertIsValid("select * from X where not exists (select * from B where X.f = B.a)");
        assertIsValid("select * from X where not exists (select * from B)");
        assertIsValid("select exists (select * from B where X.f = B.a) from A");
        assertIsValid("select B or exists (select * from B) from A");
        assertIsValid("select c in (select * from C) from A");
        assertIsValid("select c from A where b in (select * from C)");
        assertIsValid("select c from A where b not in (select b from C)");
        assertIsValid("select c from A where q*9 not in (select g*5 from C#length(100)) and r=6");

        // dynamic properties
        assertIsValid("select b.c.d? from E");
        assertIsValid("select b.c.d?.e? from E");
        assertIsValid("select b? from E");
        assertIsValid("select b? as myevent from E");
        assertIsValid("select * from pattern [every OrderEvent(item.name?)]");
        assertIsValid("select * from pattern [every OrderEvent(item?.parent.name?='foo')]");
        assertIsValid("select b.c[0].d? from E");
        assertIsValid("select b.c[0]?.mapped('a')? from E");
        assertIsValid("select b?.c[0].mapped('a') from E");

        // Allow comments in EPL and patterns
        assertIsValid("select b.c.d /* some comment */ from E");
        assertIsValid("select b /* ajajaj */ .c.d /* some comment */ from E");
        assertIsValid("select * from pattern [ /* filter */ every A() -> B() /* for B */]");
        assertIsValid("select * from pattern [ \n// comment\nevery A() -> B() // same line\n]");

        // Substitution parameters
        assertIsValid(preFill + "(string=?)");
        assertIsValid(preFill + "(string in (?, ?))");
        assertIsValid(preFill + " where string=? and ?=val");
        assertIsValid(preFill + " having avg(volume) > ?");
        assertIsValid(preFill + " having avg(?) > ?");
        assertIsValid("select sum(?) from b#length(1)");
        assertIsValid("select ?||'a' from B(a=?) where c=? group by ? having d>? output every 10 events order by a, ?");
        assertIsValid("select a from B output snapshot every 10 events order by a, ?");

        // cast, instanceof, isnumeric and exists dynamic property
        assertIsValid(preFill + "(boolean = exists(a))");
        assertIsValid(preFill + "(boolean = exists(a?))");
        assertIsValid(preFill + "(boolean = exists(a?))");
        assertIsValid(preFill + " where exists(a.b.c?.d.e)");
        assertIsValid(preFill + "(boolean = instanceof(a + 2, a))");
        assertIsValid(preFill + "(boolean = instanceof(b, a))");
        assertIsValid(preFill + "(boolean = instanceof('agc', string, String, java.lang.String))");
        assertIsValid(preFill + "(boolean = instanceof(b, com.espertech.esper.support.AClass))");
        assertIsValid(preFill + "(boolean = instanceof(b, com.espertech.esper.support.AClass, int, long, java.lang.Long))");
        assertIsValid(preFill + "(cast(b as boolean))");
        assertIsValid(preFill + "(cast(b? as Boolean))");
        assertIsValid(preFill + "(cast(b, boolean))");
        assertIsValid(preFill + "(cast(b?, Boolean))");
        assertIsValid(preFill + "(cast(b?, java.lang.String))");
        assertIsValid(preFill + "(cast(b?, long))");
        assertIsValid(preFill + "(cast(a + 5, long))");
        assertIsValid(preFill + "(isnumeric(b?))");
        assertIsValid(preFill + "(isnumeric(b + 2))");
        assertIsValid(preFill + "(isnumeric(\"aa\"))");

        // timestamp
        assertIsValid("select timestamp() from B#length(1)");

        // named window
        assertIsValid("create window AAA as MyType");
        assertIsValid("create window AAA as com.myclass.MyType");
        assertIsValid("create window AAA as select * from MyType");
        assertIsValid("create window AAA as select a, *, b from MyType");
        assertIsValid("create window AAA as select a from MyType");
        assertIsValid("create window AAA#length(10) select a from MyType");
        assertIsValid("create window AAA select a from MyType");
        assertIsValid("create window AAA#length(10) as select a from MyType");
        assertIsValid("create window AAA#length(10) as select a,b from MyType");
        assertIsValid("create window AAA#length(10)#time(1 sec) as select a,b from MyType");
        assertIsValid("create window AAA as select 0 as val, 2 as noway, '' as stringval, true as boolval from MyType");
        assertIsValid("create window AAA as (a b, c d, e f)");
        assertIsValid("create window AAA (a b, c d, e f)");
        assertIsValid("create window AAA as select * from MyOtherNamedWindow insert");
        assertIsValid("create window AAA as MyOtherNamedWindow insert where b=4");

        // on-delete statement
        assertIsValid("on MyEvent delete from MyNamedWindow");
        assertIsValid("on MyEvent delete from MyNamedWindow where key = myotherkey");
        assertIsValid("on MyEvent(myval != 0) as myevent delete from MyNamedWindow as mywin where mywin.key = myevent.otherKey");
        assertIsValid("on com.my.MyEvent(a=1, b=2 or c.d>3) as myevent delete from MyNamedWindow as mywin where a=b and c<d");
        assertIsValid("on MyEvent yyy delete from MyNamedWindow xxx where mywin.key = myevent.otherKey");
        assertIsValid("on pattern [every MyEvent or every MyEvent] delete from MyNamedWindow");

        // on-select statement
        assertIsValid("on MyEvent select * from MyNamedWindow");
        assertIsValid("on MyEvent select a, b, c from MyNamedWindow");
        assertIsValid("on MyEvent select a, b, c from MyNamedWindow where a<b");
        assertIsValid("on MyEvent as event select a, b, c from MyNamedWindow as win where a.b = b.a");
        assertIsValid("on MyEvent(hello) select *, c from MyNamedWindow");
        assertIsValid("on pattern [every X] select a, b, c from MyNamedWindow");
        assertIsValid("on MyEvent insert into YooStream select a, b, c from MyNamedWindow");
        assertIsValid("on MyEvent insert into YooStream (p, q) select a, b, c from MyNamedWindow");
        assertIsValid("on MyEvent select a, b, c from MyNamedWindow where a=b group by c having d>e order by f");
        assertIsValid("on MyEvent insert into A select * where 1=2 insert into B select * where 2=4");
        assertIsValid("on MyEvent insert into A select * where 1=2 insert into B select * where 2=4 insert into C select *");
        assertIsValid("on MyEvent insert into A select a,c,b insert into G select 1,2,2,2 where 2=4 insert into C select * where a=x");
        assertIsValid("on MyEvent insert into A select a,c,b where a=y group by p having q>r order by x,y insert into G select 1,2,2,2 where 2=4 insert into C select * where a=x");
        assertIsValid("on MyEvent insert into A select a,c,b where a=y insert into D select * where 2=4 output first");
        assertIsValid("on MyEvent insert into A select a,c,b where a=y insert into D select * where 2=4 output all");

        // on-set statement
        assertIsValid("on MyEvent set var=1");
        assertIsValid("on MyEvent set var = true");
        assertIsValid("on MyEvent as event set var = event.val");
        assertIsValid("on MyEvent as event set var = event.val");
        assertIsValid("on MyEvent as event set var = event.val * 2, var2='abc', var3='def'");

        // on-update statement
        assertIsValid("on MyEvent update ABC as abc set a=1, b=c");
        assertIsValid("on MyEvent update ABC set a=1");
        assertIsValid("on pattern[every B] update ABC as abc set a=1, b=abc.c");

        // create variable
        assertIsValid("create variable integer a = 77");
        assertIsValid("create variable sometype b = 77");
        assertIsValid("create variable sometype b");

        // use variable in output clause
        assertIsValid("select count(*) from A output every VAR1 events");

        // join with method result
        assertIsValid("select * from A, method:myClass.myname() as b where a.x = b.x");
        assertIsValid("select method, a, b from A, METHOD:com.maypack.myClass.myname() as b where a.x = b.x");
        assertIsValid("select method, a, b from A, someident:com.maypack.myClass.myname() as b where a.x = b.x");

        // unidirectional join
        assertIsValid("select * from A as x unidirectional, method:myClass.myname() as b where a.x = b.x");
        assertIsValid("select a, b from A as y unidirectional, B as b where a.x = b.x");
        assertIsValid("select a, b from A as y unidirectional, B unidirectional where a.x = b.x");

        // expessions and event properties are view/guard/observer parameters
        assertIsValid("select * from A.win:x(myprop.nested, a.c('s'), 'ss', abc, null)");
        assertIsValid("select * from pattern[every X where a:b(myprop.nested, a.c('s'), 'ss', *, null)]");
        assertIsValid("select * from pattern[every X:b(myprop.nested, a.c('s'), 'ss', *, null)]");

        // properties escaped
        assertIsValid("select a\\.b, a\\.b\\.c.d.e\\.f, zz\\.\\.\\.aa\\.\\.\\.b\\.\\. from A");
        assertIsValid("select count from A");

        // limit
        assertIsValid("select count from A limit 1");
        assertIsValid("select count from A limit 1,2");
        assertIsValid("select count from A limit 1 offset 2");
        assertIsValid("select count from A where a=b group by x having c=d output every 5 events order by r limit 1 offset 2");
        assertIsValid("select count from A limit myvar");
        assertIsValid("select count from A limit myvar,myvar2");
        assertIsValid("select count from A limit myvar offset myvar2");
        assertIsValid("select count from A limit -1");

        // any, some, all
        assertIsValid("select * from A where 1 = ANY (1, exp, 3)");
        assertIsValid("select * from A where 1 = SOME ({1,2,3}, myvar, 2*2)");
        assertIsValid("select * from A where exp = ALL ()");
        assertIsValid("select * from A where 1 != ALL (select a from B)");
        assertIsValid("select * from A where 1 = SOME (select a from B)");
        assertIsValid("select * from A where exp > ANY (select a from B)");
        assertIsValid("select * from A where 1 <= ANY (select a from B)");
        assertIsValid("select * from A where {1,2,3} > ALL (1,2,3)");

        // annotations
        assertIsValid("@SOMEANNOTATION select * from B");
        assertIsValid("@SomeOther(a=1, b=true, c='a', d=\"alal\") select * from B");
        assertIsValid("@SomeOther(@inner2(a=3)) select * from B");
        assertIsValid("@SomeOther(@inner1) select * from B");
        assertIsValid("@SomeOther(a=com.myenum.VAL1,b=a.VAL2) select * from B");
        assertIsValid("@SomeOther(tags=@inner1(a=4), moretags=@inner2(a=3)) select * from B");
        assertIsValid("@SomeOther(innerdata={1, 2, 3}) select * from B");
        assertIsValid("@SomeOther(innerdata={1, 2, 3}) select * from B");
        String text = "@EPL(\n" +
                "  name=\"MyStmtName\", \n" +
                "  description=\"Selects all fields\", \n" +
                "  onUpdate=\"some test\", \n" +
                "  onUpdateRemove=\"some text\", \n" +
                "  tags=@Tags" +
                ")\n" +
                "select * from MyField";
        assertIsValid(text);
        text = "@EPL(name=\"MyStmtName\"," +
                "  tags=@Tags(" +
                "    {@Tag(name=\"vehicleId\", type='int', value=100), " +
                "     @Tag(name=\"vehicleId\", type='int', value=100)" +
                "    } " +
                "  )" +
                ")\n" +
                "select * from MyField";
        assertIsValid(text);
        assertIsValid("@Name('MyStatementName')\n" +
                "@Description('This statement does ABC')\n" +
                "@Tag(name='abc', value='cde')\n" +
                "select a from B");

        // row pattern recog
        assertIsValid("select * from A match_recognize (measures A.symbol as A\n" +
                "pattern (A B)\n" +
                "define B as (B.price > A.price)" +
                ")");
        assertIsValid("select * from A match_recognize (measures A.symbol as A\n" +
                "pattern (A* B+ C D?)\n" +
                "define B as (B.price > A.price)" +
                ")");
        assertIsValid("select * from A match_recognize (measures A.symbol as A\n" +
                "pattern (A | B)\n" +
                "define B as (B.price > A.price)" +
                ")");
        assertIsValid("select * from A match_recognize (measures A.symbol as A\n" +
                "pattern ( (A B) | (C D))\n" +
                "define B as (B.price > A.price)" +
                ")");
        assertIsValid("select * from A match_recognize (measures A.symbol as A\n" +
                "pattern ( (A | B) (C | D) )\n" +
                "define B as (B.price > A.price)" +
                ")");
        assertIsValid("select * from A match_recognize (measures A.symbol as A\n" +
                "pattern ( (A) | (B | (D | E+)) )\n" +
                "define B as (B.price > A.price)" +
                ")");
        assertIsValid("select * from A match_recognize (measures A.symbol as A\n" +
                "pattern ( A (C | D)? E )\n" +
                "define B as (B.price > A.price)" +
                ")");
    }

    public void testBitWiseCases() throws Exception {
        String className = SupportBean.class.getName();
        String eplSmt = "select (intPrimitive & intBoxed) from " + className;
        assertIsValid(eplSmt + ".win:lenght()");
        eplSmt = "select boolPrimitive|boolBoxed from " + className;
        assertIsValid(eplSmt + "().std:win(20)");
        eplSmt = "select bytePrimitive^byteBoxed from " + className;
        assertIsValid(eplSmt + "().win:some_view({})");
    }

    public void testIfThenElseCase() throws Exception {
        String className = SupportBean.class.getName();
        String eplSmt = "select case when 1 then (a + 1) when 2 then (a*2) end from " + className;
        assertIsValid(eplSmt + ".win:lenght()");
        eplSmt = "select case a when 1 then (a + 1) end from " + className;
        assertIsValid(eplSmt + ".win:lenght()");
        eplSmt = "select case count(*) when 10 then sum(a) when 20 then max(a*b) end from " + className;
        assertIsValid(eplSmt + ".win:lenght()");
        eplSmt = "select case (a>b) when true then a when false then b end from " + className;
        assertIsValid(eplSmt + ".win:lenght()");
        eplSmt = "select case a when true then a when false then b end from " + className;
        assertIsValid(eplSmt + ".win:lenght()");
        eplSmt = "select case when (a=b) then (a+b) when false then b end as p1 from " + className;
        assertIsValid(eplSmt + ".win:lenght()");
        eplSmt = "select case (a+b) when (a*b) then count(a+b) when false then a ^ b end as p1 from " + className;
        assertIsValid(eplSmt + ".win:lenght()");
    }

    private void tryJoin(String joinType) throws Exception {
        String className = SupportBean.class.getName();
        assertIsValid("select intPrimitive from " +
                className + "(a=1).win:lenght(10) as win1 " +
                joinType + " outer join " +
                className + "(a=2).win:lenght(10) as win2 " +
                "on win1.f1 = win2.f2"
        );

        assertIsValid("select intPrimitive from " +
                className + "(a=1).win:lenght(10) as win1 " +
                joinType + " outer join " +
                className + "(a=2).win:lenght(10) as win2 " +
                "on win1.f1 = win2.f2 " +
                joinType + " outer join " +
                className + "(a=2).win:lenght(10) as win3 " +
                "on win1.f1 = win3.f3"
        );
    }

    private void assertIsValid(String text) throws Exception {
        log.debug(".assertIsValid Trying text=" + text);
        Pair<Tree, CommonTokenStream> ast = parse(text);
        log.debug(".assertIsValid success, tree walking...");

        SupportParserHelper.displayAST(ast.getFirst());
        log.debug(".assertIsValid done");
    }

    private void assertIsInvalid(String text) throws Exception {
        log.debug(".assertIsInvalid Trying text=" + text);

        try {
            parse(text);
            assertFalse(true);
        } catch (Exception ex) {
            log.debug(".assertIsInvalid Expected ParseException exception was thrown and ignored, message=" + ex.getMessage());
        }
    }

    private Pair<Tree, CommonTokenStream> parse(String expression) throws Exception {
        return SupportParserHelper.parseEPL(expression);
    }

    static Logger log = LoggerFactory.getLogger(TestEPLParser.class);
}
