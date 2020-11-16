package work.beltran.sample

@Mockable
interface Hello {
    fun foo() {}
    fun bar(value: Int): Int
    fun add(a: Float, b: Float): Float
}

fun main(args: Array<String>) {
    println("Hello World")
}