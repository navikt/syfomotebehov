---
applyTo: "**/*Test.kt,**/*Spec.kt"
---

# Testing Standards (Kotlin)

## Test Framework

- **Framework**: Kotest `DescribeSpec` with JUnit 5 runner + `@ApplyExtension(SpringExtension::class)`
- **Base class**: `IntegrationTest` (extends DescribeSpec, clears all mocks in `afterTest`)
- **Mocking**: MockK + springmockk (`@MockkBean`)
- **HTTP mocking**: WireMock (Kotest extension)
- **Database**: TestContainers PostgreSQL via `LocalApplication` with `@ServiceConnection`
- **Assertions**: Mix of Kotest assertions (`shouldBe`) and AssertJ (`assertThat`)
- **Test files**: Named `*Test.kt`

## Test Structure

```kotlin
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
@ApplyExtension(SpringExtension::class)
class MotebehovArbeidstakerControllerV4Test : IntegrationTest() {
    @MockkBean
    lateinit var pdlConsumer: PdlConsumer

    @Autowired
    lateinit var motebehovDAO: MotebehovDAO

    init {
        describe("MotebehovArbeidstakerControllerV4") {
            beforeTest {
                // Setup mocks
                every { pdlConsumer.person(any()) } returns generatePdlHentPerson()
            }

            it("should return motebehov status for arbeidstaker") {
                // Arrange
                val expected = createTestData()

                // Act
                val result = controller.motebehovStatusArbeidstaker()

                // Assert
                result shouldBe expected
            }

            it("should submit new motebehov") {
                val submission = MotebehovFormSubmissionDTO(...)

                controller.lagreMotebehovArbeidstaker(submission)

                verify(exactly = 1) { esyfovarselService.sendVarsel(any()) }
            }
        }
    }
}
```

## Mocking with MockK

```kotlin
// Springmockk integration
@MockkBean
lateinit var pdlConsumer: PdlConsumer

@MockkBean
lateinit var esyfovarselService: EsyfovarselService

// Specific behavior
every { pdlConsumer.person(any()) } returns generatePdlHentPerson()
every { pdlConsumer.person(ARBEIDSTAKER_FNR) } returns specificPerson

// Verification
verify(exactly = 1) { esyfovarselService.sendVarsel(any()) }
verify(exactly = 0) { esyfovarselService.sendVarsel(any()) }
```

## Database Cleanup

Use `@Sql` annotations to clean database between tests:

```kotlin
@Sql(statements = ["DELETE FROM MOTEBEHOV", "DELETE FROM MOTEBEHOV_FORM_VALUES"])
```

Or clean in `beforeTest` blocks.

## Test Data Helpers

Located in `src/test/kotlin/no/nav/syfo/testhelper/`:

- **`UserConstants`** — Test FNR, aktørID, virksomhetsnummer, veileder ID
- **`generator/MotebehovGenerator`** — Creates test motebehov submissions
- **`generator/generateOppfolgingstilfellePerson()`** — Creates oppfølgingstilfelle test data
- **`generator/generatePdlHentPerson()`** — Creates PDL person response
- **`assertion/`** — Custom assertion helpers (`assertMotebehovStatus`)

## Assertions

```kotlin
// Kotest matchers
result shouldBe expected
result shouldNotBe null
list shouldHaveSize 3

// AssertJ (also used in this project)
assertThat(result).isNotNull
assertThat(result.harMotebehov).isTrue
assertEquals(expected, actual)
```

## Running Tests

```bash
# All tests
./gradlew test

# Single test class
./gradlew test --tests "no.nav.syfo.motebehov.api.MotebehovArbeidstakerControllerV4Test"

# Build + test (recommended before commit)
./gradlew build
```

All tests require Docker running for TestContainers (PostgreSQL).

## Boundaries

### ✅ Always

- Extend `IntegrationTest` for integration tests
- Use `@SpringBootTest(classes = [LocalApplication::class])`
- Use test data generators from `testhelper/generator/`
- Clean up database state between tests
- Test both success and error cases

### ⚠️ Ask First

- Modifying `IntegrationTest` base class
- Changing `LocalApplication` TestContainers setup
- Adding new shared test utilities

### 🚫 Never

- Use real fødselsnummer in test data
- Share mutable state between tests
- Skip tests without good reason
