package mock.simple.kotlin.mockk

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import mock.simple.kotlin.sample.Todo
import mock.simple.kotlin.sample.TodosRepository
import org.junit.Before
import org.junit.Test

class MockKTodosRepositoryTest {
    lateinit var mock: TodosRepository

    @Before
    fun setUp() {
        mock = mockk(relaxed = true)
    }

    @Test
    fun add() = runBlockingTest {
        // Given

        // When
        mock.add(Todo("foobar"))


        // Then
        coVerify(atLeast = 1) { mock.add(Todo("foobar") )}
    }

    @Test
    fun get() = runBlockingTest {
        // Given
        coEvery { mock.get() } returns flowOf(listOf(Todo("foobar")))

        // When
        val todos = mock.get().first()

        // Then
        coVerify(atLeast = 1) { mock.get() }
        assertThat(todos).isEqualTo(listOf(Todo("foobar")))
    }

    @Test
    fun update() = runBlockingTest {
        // Given

        // When
        mock.update(Todo("foobar"))


        // Then
        coVerify(atLeast = 1) { mock.update(Todo("foobar") )}
    }

    @Test
    fun remove() = runBlockingTest {
        // Given

        // When
        mock.remove(Todo("foobar"))


        // Then
        coVerify(atLeast = 1) { mock.remove(Todo("foobar") )}
    }
}