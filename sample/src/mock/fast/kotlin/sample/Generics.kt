package mock.fast.kotlin.sample

import mock.fast.kotlin.Mockable

@Mockable
interface Generics<out K, in V> {
    fun get(id: Int): K
    fun getAll(): List<K>
    fun add(elm: V)
}