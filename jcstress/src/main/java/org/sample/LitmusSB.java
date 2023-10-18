package org.sample;

import komem.litmus.LitmusTest;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import komem.litmus.testsuite.ClassicTestsKt;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;

import java.util.List;

@JCStressTest
@State
public class LitmusSB {

    private static final LitmusTest<Object> sb = (LitmusTest<Object>) ClassicTestsKt.getSB();
    private static final Function1<Object, Unit> fT0 = sb.getThreadFunctions().get(0);
    private static final Function1<Object, Unit> fT1 = sb.getThreadFunctions().get(1);
    private static final Function1<Object, Object> fA = sb.getOutcomeFinalizer();

    public LitmusSB() {
    }

    public Object state = sb.getStateProducer().invoke();

    @Actor
    public void t1() {
        fT0.invoke(state);
    }

    @Actor
    public void t2() {
        fT1.invoke(state);
    }

    @Arbiter
    public void a(II_Result r) {
        List<Integer> result = (List<Integer>) fA.invoke(state);
        r.r1 = result.get(0);
        r.r2 = result.get(1);
    }

}
