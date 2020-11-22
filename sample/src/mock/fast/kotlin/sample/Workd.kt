package mock.fast.kotlin.sample

import mock.fast.kotlin.Mockable

@Mockable
interface World<K, V> {
    fun get(): List<K>
    fun add(elm: V)
}