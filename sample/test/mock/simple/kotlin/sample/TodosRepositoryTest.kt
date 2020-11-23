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
        mock.addHandler = {}

        // When
        mock.add(Todo("foobar"))

        // Then
        assertThat(mock.addCallCount).isEqualTo(1)
        assertThat(mock.addArgValues.first()).isEqualTo(listOf(Todo("foobar")))
    }

    @Test
    fun get() = runBlockingTest {
        // Given
        mock.getHandler = {
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
        mock.updateHandler = {}

        // When
        mock.update(Todo("foobar"))

        // Then
        assertThat(mock.updateCallCount).isEqualTo(1)
        assertThat(mock.updateArgValues.first()).isEqualTo(listOf(Todo("foobar")))
    }

    @Test
    fun remove() = runBlockingTest {
        // Given
        mock.removeHandler = {}

        // When
        mock.remove(Todo("foobar"))

        // Then
        assertThat(mock.removeCallCount).isEqualTo(1)
        assertThat(mock.removeArgValues.first()).isEqualTo(listOf(Todo("foobar")))
    }
}