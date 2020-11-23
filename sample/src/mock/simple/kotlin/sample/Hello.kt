package mock.simple.kotlin.sample

import mock.simple.kotlin.Mockable

class Hoge()

@Mockable
interface Hello {
    val num: Int
    var sum: Int
    fun foo(): Hoge
    fun bar(value: Int): Int
    fun add(a: Float, b: Float): Float
    fun gets(a: List<String>): List<String>
}