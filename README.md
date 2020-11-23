# kotlin-simple-mock

kotlin-simple-mock is a simple mock generator for Kotlin.
This library uses kapt and generates interface mock classes with specific annotations.

## Motivation

The main motivation is to reduce the execution time of a few test cases.
The execution time of a test using mockK can take more than 1 sec for even a few simple test cases. This feels very slow.
Mockito is much faster than mockK, but it still takes a hundred milliseconds or more to run a few of tests.

(At this time, it is untested whether the decrease in test run time is greater than the increase in build time.)

The second motivation is to simplify the mock.

## Setup(Gradle Kotlin DSL)

#### Step 1. Add it in your root build.gradle at the end of repositories.

```
allprojects {
    repositories {
        ...
        maven(url = "https://jitpack.io")
    }
}
```

#### Step 2. Add the dependency.

```
dependencies {
    implementation("com.github.KamikazeZirou.kotlin-simple-mock:processor:0.0.3")
    kapt("com.github.KamikazeZirou.kotlin-simple-mock:processor:0.0.3")
}
```

#### Step 3. Setup kapt.

```
plugins {
    kotlin("kapt")
}

kapt {
    arguments {
        arg("kotlin.simple.mock.generated", "${buildDir}/generated/kotlin-simple-mock")
    }
}

sourceSets {
    // Here I assume that the test code is placed in the "test" directory of the module. 
    getByName("test").java.srcDirs("test", "${buildDir}/generated/kotlin-simple-mock")
}
```

## How to use

#### Step 1. Annotate to the interface you want to generate mock.

```
import mock.simple.kotlin.Mockable

@Mockable
interface Hello {
    fun add(a: Int, b: Int): Int
    val num: Int
}
```

#### Step 2. Build annotated class.

The following code will be generated.

```
public class MockHello : Hello {
  public var addFuncHandler: ((a: Int, b: Int) -> Int)? = null

  public var addCallCount: Int = 0

  public var addFuncArgValues: MutableList<List<Any>> = mutableListOf()

  public override fun add(a: Int, b: Int): Int {
    addCallCount += 1
    addFuncArgValues.add(listOf(a,b))
    return addFuncHandler!!(a,b)
  }
}
```

Note: Here is an example of a method, but the property can also be mocked.

#### Step 3. Write test code with the generated mock.

```
class HelloTest {
    @Test
    fun add() {
        // Given
        val mock = MockHello()
        mock.addFuncHandler = { a, b ->
            a + b
        }

        // Then
        val result = mock.add(1, 2)

        // When
        assertEquals(3, result)
        assertEquals(1, mock.addCallCount)
        assertEquals(listOf(1, 2), mock.addFuncArgValues.first())
    }
}
```

#### Step 4. Run test.

## Limitations

- Only interface supported for now.
- Generic functions are not supported.
- The super interface mock is not generated.
