package mock.simple.kotlin.sample

import kotlinx.coroutines.flow.Flow
import mock.simple.kotlin.Mockable

@Mockable
interface TodosRepository {
    suspend fun add(todo: Todo)
    suspend fun get(): Flow<List<Todo>>
    suspend fun update(todo: Todo)
    suspend fun remove(todo: Todo)
}