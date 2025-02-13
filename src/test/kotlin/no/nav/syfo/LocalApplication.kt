package no.nav.syfo

import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration

@TestConfiguration(proxyBeanMethods = false)
class LocalApplication

fun main(args: Array<String>) {
    fromApplication<Application>().with(LocalApplication::class.java).run(*args)
}
