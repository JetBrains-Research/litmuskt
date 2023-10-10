import komem.litmus.LitmusTest;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import java.util.List;

public class AbaTest<S> {

    private static List<Function1<S, Unit>> fs;
    private static Function1<S, Object> arbiter;

    public AbaTest(LitmusTest<?> test) {
        fs = test.getThreadFunctions();
        arbiter = test.getOutcomeFinalizer();
    }

}


/*

class LitmusJcsState<S> {
    private static (!!!) List<Function1<...>> tfs = test.getThreadFunctions();

    private S state = stateProducer();

    public void actor0() {
        tfs[0].invoke(state);
    }
}
 */
