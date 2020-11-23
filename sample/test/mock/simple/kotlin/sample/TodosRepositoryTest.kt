package mock.simple.kotlin.sample

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test

class TodosRepositoryTest {
    internal lateinit var mock: MockTodosRepository

    @Before
    fun setUp() {
        mock = MockTodosRepository()
    }

    @Test
    fun add() = runBlockingTest {
        // Given
        mock.addFuncHandler = {}

        // When
        mock.add(Todo("foobar"))

        // Then
        assertThat(mock.addCallCount).isEqualTo(1)
        assertThat(mock.addFuncArgValues.first()).isEqualTo(listOf(Todo("foobar")))
    }

    @Test
    fun get() = runBlockingTest {
        // Given
        mock.getFuncHandler = {
            flowOf(listOf(Todo("foobar")))
        }

        // When
        val todos = mock.get().first()

        // Then
        assertThat(mock.getCallCount).isEqualTo(1)
        assertThat(todos).isEqualTo(listOf(Todo("foobar")))
    }

    @Test
    fun update() = runBlockingTest {
        // Given
        mock.updateFuncHandler = {}

        // When
        mock.update(Todo("foobar"))

        // Then
        assertThat(mock.updateCallCount).isEqualTo(1)
        assertThat(mock.updateFuncArgValues.first()).isEqualTo(listOf(Todo("foobar")))
    }

    @Test
    fun remove() = runBlockingTest {
        // Given
        mock.removeFuncHandler = {}

        // When
        mock.remove(Todo("foobar"))

        // Then
        assertThat(mock.removeCallCount).isEqualTo(1)
        assertThat(mock.removeFuncArgValues.first()).isEqualTo(listOf(Todo("foobar")))
    }
}