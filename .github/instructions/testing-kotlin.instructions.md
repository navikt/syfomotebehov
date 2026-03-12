---
applyTo: "**/*.test.kt,**/*.spec.kt,**/*Test.kt,**/*Spec.kt,**/*Spek.kt"
---
<!-- Managed by esyfo-cli. Do not edit manually. Changes will be overwritten.
     For repo-specific customizations, create your own files without this header. -->

# Testing Standards (Kotlin)

## General
- Tests should describe behavior, not implementation
- Each test should test one thing
- Use descriptive test names that explain expected behavior
- Arrange → Act → Assert pattern

## Kotest + MockK
- Check build.gradle.kts for the Kotest version and available test dependencies
- Use `should` matchers for assertions (Kotest or Kluent `shouldBeEqualTo`)
- **Check existing tests first** — follow the repo's established test style for consistency
- If the file you're editing already uses a different Kotest style (FunSpec, StringSpec, etc.), keep that style — don't mix specs in the same file
- **DescribeSpec is team standard** for new test classes. When creating a new test file without existing precedent nearby, use DescribeSpec
- Use MockK for mocking — prefer `coEvery` for suspend functions
- Use Testcontainers for integration tests with real databases
- Use MockOAuth2Server for auth testing

```kotlin
// Option A: DescribeSpec (team standard for new test classes)
class ResourceServiceTest : DescribeSpec({
    val service = ResourceService(mockk())

    describe("process") {
        it("should process event correctly") {
            val input = createTestInput()
            val result = service.process(input)
            result shouldBe expectedResult
            result.status shouldBe "completed"
        }
    }
})

// Option B: JUnit5 with Kotest matchers (common in Spring Boot repos)
class ResourceServiceTest {
    private val service = ResourceService(mockk())

    @Test
    fun `should process event correctly`() {
        val input = createTestInput()
        val result = service.process(input)
        result shouldBe expectedResult
        result.status shouldBe "completed"
    }
}
```

### Testing Auth (MockOAuth2Server)

```kotlin
private val mockOAuth2Server = MockOAuth2Server()

// Issue a test token — use with your framework's test client (MockMvc, testApplication, etc.)
val token = mockOAuth2Server.issueToken(
    issuerId = "azuread",
    subject = "test-user",
    claims = mapOf("preferred_username" to "test@nav.no")
)
```

## Integration Tests
- Use real dependencies where feasible (Testcontainers for databases)
- Test the full flow, not just units in isolation
- Clean up test data after each test

## Test Naming

```kotlin
// ✅ Good
`should create user when valid data provided`
`should throw exception when email is invalid`

// ❌ Bad
`test1`
`createUserTest`
```

## Boundaries

### ✅ Always
- Write tests for new code before committing
- Test both success and error cases
- Use descriptive test names
- Run full test suite before pushing

### ⚠️ Ask First
- Changing test framework or structure
- Disabling or skipping tests

### 🚫 Never
- Commit failing tests
- Skip tests without good reason
- Share mutable state between tests
