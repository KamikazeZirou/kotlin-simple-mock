package work.beltran.sample

class Hoge() {}

@Mockable
interface Hello {
    fun foo(): Hoge
    fun bar(value: Int): Int
    fun add(a: Float, b: Float): Float
    fun gets(a: List<String>): List<String>
}

fun main(args: Array<String>) {
    println("Hello World")
    val mockHello = MockHello()
    mockHello.addFuncHandler = { a, b ->
        a + b
    }

    println(mockHello.add(1.toFloat(), 2.toFloat()))
}
