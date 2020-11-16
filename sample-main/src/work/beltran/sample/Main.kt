package work.beltran.sample

@Mockable
class Hello {
    fun foo() {}
    fun bar(value: Int): Int { return 0 }
    fun add(a: Float, b: Float): Float { return a + b }
}

fun main(args: Array<String>) {
    println("Hello World")
}