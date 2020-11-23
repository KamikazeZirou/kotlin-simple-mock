package mock.simple.kotlin.sample

import mock.simple.kotlin.Mockable

@Mockable
interface Hello {
    fun add(a: Int, b: Int): Int
    var num: Int
}