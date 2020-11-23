package mock.simple.kotlin.sample

import mock.simple.kotlin.Mockable

@Mockable
interface Generics<out A, in B, C : Comparable<C>> {
    fun get(id: Int): A
    fun getAll(): List<A>
    fun add(elm: B)
    fun compare(a: C, b: C): Int
//    Generic functions are not supported.
//    fun <T> singletonList(item: T): List<T>
}