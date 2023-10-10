package komem.litmus

interface LitmusTestState<S> {
    fun LitmusTestScope<S>.setup()
}

fun <S : LitmusTestState<S>> (() -> S).toTest(): LitmusTest<S> {
    val scope = LitmusTestScope(this)
    this().run { scope.setup() }
    return scope.build()
}
