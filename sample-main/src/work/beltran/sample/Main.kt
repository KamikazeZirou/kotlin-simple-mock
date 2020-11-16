package work.beltran.sample

@GenName
class Hello {
    fun foo() {}
    fun bar(value: Int): Int { return 0 }
    fun hoge(): World { return World() }
}

class World {

}

fun main(args: Array<String>) {
//    println("Hello World")
    println("Hello ${Generated_Hello().getName()}")
}