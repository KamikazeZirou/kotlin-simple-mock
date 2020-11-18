package work.beltran.sample

class Hoge() {}

@Mockable
interface Hello {
    val num: Int
    var sum: Int
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
    val (a, b) = mockHello.addFuncArgValues.first()
    println(a == 1.toFloat())
    println(b == 2.toFloat())
}
