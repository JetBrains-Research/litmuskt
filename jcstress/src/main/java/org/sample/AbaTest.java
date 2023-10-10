package org.sample;

import komem.litmus.LitmusTest;

public class AbaTest<S> {

    private List<Function1<S, Unit>> fs;
    private Function1<S, Object> arbiter;

    public AbaTest(LitmusTest<?> test) {
        fs = test.getThreadFunctions();
        arbiter = test.getOutcomeFinalizer();
    }

}
