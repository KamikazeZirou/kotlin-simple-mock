package mock.fast.kotlin.other

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import mock.fast.kotlin.sample.Todo
import mock.fast.kotlin.sample.TodosRepository
import org.junit.Before
import org.junit.Test

class MockitoTodosRepositoryTest {
    lateinit var mock: TodosRepository

    @Before
    fun setUp() {
        mock = mock()
    }

    @Test
    fun add() = runBlockingTest {
        // Given

        // When
        mock.add(Todo("foobar"))


        // Then
        verify(mock, times((1))).add(Todo("foobar"))
    }

    @Test
    fun get() = runBlockingTest {
        // Given
        mock.stub {
            onBlocking { get() } doReturn flowOf(listOf(Todo("foobar")))
        }

        // When
        val todos = mock.get().first()

        // Then
        verify(mock, times(1)).get()
        assertThat(todos).isEqualTo(listOf(Todo("foobar")))
    }

    @Test
    fun update() = runBlockingTest {
        // Given

        // When
        mock.update(Todo("foobar"))


        // Then
        verify(mock, times((1))).update(Todo("foobar"))
    }

    @Test
    fun remove() = runBlockingTest {
        // Given

        // When
        mock.remove(Todo("foobar"))


        // Then
        verify(mock, times((1))).remove(Todo("foobar"))
    }
}