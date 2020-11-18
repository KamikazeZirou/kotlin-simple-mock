package mock.fast.kotlin.sample

import kotlinx.coroutines.flow.Flow
import mock.fast.kotlin.Mockable

@Mockable
interface TodosRepository {
    suspend fun add(todo: Todo)
    suspend fun get(): Flow<List<Todo>>
    suspend fun update(todo: Todo)
    suspend fun remove(todo: Todo)
}