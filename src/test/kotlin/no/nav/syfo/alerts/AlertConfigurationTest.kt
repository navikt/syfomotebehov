package no.nav.syfo.alerts

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path

class AlertConfigurationTest :
    DescribeSpec({
        val repositoryRoot = Path.of("").toAbsolutePath()
        val workflow = Files.readString(repositoryRoot.resolve(".github/workflows/alerts.yaml"))
        val rules = loadRules(repositoryRoot.resolve("nais/alerts-gcp.yaml"))

        describe("alert deployment") {
            it("watches the same prod-gcp alert resource that it deploys") {
                val resource = workflow.requiredValue("RESOURCE")
                val watchedPaths = workflow.requiredList("paths")

                Files.exists(repositoryRoot.resolve(resource)) shouldBe true
                watchedPaths.contains(resource) shouldBe true
                watchedPaths.contains(".nais/alerts-gcp.yaml") shouldBe false
                workflow.requiredValue("CLUSTER") shouldBe "prod-gcp"
            }
        }

        describe("HTTP and availability alerts") {
            it("keeps unavailable replicas as an unrouted shadow candidate") {
                val rule = rules.requiredRule("SyfomotebehovUnavailable")
                val expression = rule.requiredString("expr")
                val availableSelector = expression.selectors("kube_deployment_status_replicas_available").single()
                val desiredSelector = expression.selectors("kube_deployment_spec_replicas").single()

                availableSelector.shouldHaveProdDeploymentLabels()
                desiredSelector.shouldHaveProdDeploymentLabels()
                expression.canonicalPromql() shouldContain
                    "maxby(deployment,k8s_cluster_name,namespace)(${availableSelector.canonical()})==0" +
                    "andon(deployment,k8s_cluster_name,namespace)" +
                    "maxby(deployment,k8s_cluster_name,namespace)(${desiredSelector.canonical()})>0"
                rule.requiredString("for") shouldBe "5m"
                rule.shouldHaveOperationalContext()
                rule.shouldHaveLabels(
                    mapOf(
                        "app" to "syfomotebehov",
                        "namespace" to "team-esyfo",
                        "severity" to "warning",
                        "response" to "ticket",
                        "pager_candidate" to "blocked",
                        "alert_stage" to "shadow",
                        "alert_type" to "custom",
                    ),
                )
            }

            it("uses live prod server spans and requires both traffic and multiple 5xx responses") {
                val rule = rules.requiredRule("SyfomotebehovHigh5xxRatio")
                val expression = rule.requiredString("expr")
                val canonicalExpression = expression.canonicalPromql()
                val selectors = expression.selectors("traces_spanmetrics_calls_total")
                val errorStatus = LabelMatcher("=~", "^5[0-9][0-9]$")
                val totalStatus = LabelMatcher("=~", "^[1-5][0-9][0-9]$")

                selectors.isNotEmpty() shouldBe true
                selectors.forEach { it.shouldHaveProdServerSpanLabels() }
                val errorSelector =
                    selectors
                        .filter { it.matchers["http_response_status_code"] == errorStatus }
                        .map { it.canonical() }
                        .toSet()
                        .single()
                val totalSelector =
                    selectors
                        .filter { it.matchers["http_response_status_code"] == totalStatus }
                        .map { it.canonical() }
                        .toSet()
                        .single()
                selectors.all {
                    it.matchers["http_response_status_code"] in setOf(errorStatus, totalStatus)
                } shouldBe true

                val errorIncrease = "sumby(service_name)(increase($errorSelector[15m]))"
                val totalIncrease = "sumby(service_name)(increase($totalSelector[15m]))"
                val zeroSeededErrors = "(${errorIncrease}oron(service_name)(0*$totalIncrease))"

                canonicalExpression shouldContain "(100*$zeroSeededErrors/$totalIncrease)>2"
                canonicalExpression shouldContain "andon(service_name)$totalIncrease>=20"
                canonicalExpression shouldContain "andon(service_name)$zeroSeededErrors>=3"
                canonicalExpression shouldNotContain "nginx_ingress_controller_requests"
                rule.requiredString("for") shouldBe "5m"
                rule.shouldHaveOperationalContext()
                rule.shouldHaveLabels(
                    mapOf(
                        "app" to "syfomotebehov",
                        "namespace" to "team-esyfo",
                        "severity" to "warning",
                        "response" to "ticket",
                        "alert_stage" to "shadow",
                        "alert_type" to "custom",
                    ),
                )
            }

            it("keeps generic 4xx outside the alert rules") {
                rules.map { it.requiredString("alert") }.none { it.contains("4xx", ignoreCase = true) } shouldBe true
                rules.joinToString() shouldNotContain "nginx_ingress_controller_requests"
                rules
                    .flatMap { it.requiredString("expr").selectors("traces_spanmetrics_calls_total") }
                    .mapNotNull { it.matchers["http_response_status_code"] }
                    .none { it.value.startsWith("^4") } shouldBe true
            }

            it("links shadow rules to the canonical service-filtered control room") {
                listOf("SyfomotebehovUnavailable", "SyfomotebehovHigh5xxRatio").forEach { name ->
                    val annotations = rules.requiredRule(name).requiredMap("annotations")
                    annotations.requiredString("dashboard_url") shouldBe CONTROL_ROOM_URL
                }
            }
        }
    })

private const val CONTROL_ROOM_URL =
    "https://grafana.nav.cloud.nais.io/d/team-esyfo-kontrollrom/team-esyfo-kontrollrom" +
        "?orgId=1&from=now-1h&to=now&timezone=browser&refresh=2m&var-service=syfomotebehov"

private fun loadRules(path: Path): List<Map<String, Any?>> {
    val document = Files.newInputStream(path).use { Yaml().load<Map<String, Any?>>(it) }
    val spec = document.requiredMap("spec")
    val groups = spec.requiredList("groups")
    return groups.flatMap { it.requiredList("rules") }
}

private fun String.requiredValue(key: String): String =
    Regex("""(?m)^\s*$key:\s*['\"]?([^'\"\s]+)['\"]?\s*$""")
        .find(this)
        ?.groupValues
        ?.get(1)
        ?: error("Mangler $key")

private fun String.requiredList(key: String): List<String> {
    val lines = lines()
    val headerIndex = lines.indexOfFirst { it.trim() == "$key:" }
    if (headerIndex < 0) error("Mangler listefeltet $key")
    val headerIndent = lines[headerIndex].length - lines[headerIndex].trimStart().length
    return lines
        .drop(headerIndex + 1)
        .takeWhile { it.isBlank() || it.length - it.trimStart().length > headerIndent }
        .map { it.trim() }
        .filter { it.startsWith("- ") }
        .map {
            it
                .removePrefix("- ")
                .trim()
                .removeSurrounding("'")
                .removeSurrounding("\"")
        }.toList()
}

private fun List<Map<String, Any?>>.requiredRule(name: String): Map<String, Any?> =
    singleOrNull { it["alert"] == name } ?: error("Mangler unik alert $name")

private fun Map<String, Any?>.shouldHaveOperationalContext() {
    val annotations = requiredMap("annotations")
    annotations.requiredString("consequence")
    annotations.requiredString("action")
    annotations.requiredString("runbook_url").shouldContain("https://")
    annotations.requiredString("dashboard_url").shouldContain("https://")
}

private fun Map<String, Any?>.shouldHaveLabels(expected: Map<String, String>) {
    val labels = requiredMap("labels")
    expected.forEach { (name, value) -> labels[name] shouldBe value }
}

private fun Map<String, Any?>.requiredString(key: String): String = this[key] as? String ?: error("Mangler tekstfeltet $key")

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.requiredMap(key: String): Map<String, Any?> =
    this[key] as? Map<String, Any?> ?: error("Mangler objektfeltet $key")

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.requiredList(key: String): List<Map<String, Any?>> =
    this[key] as? List<Map<String, Any?>> ?: error("Mangler listefeltet $key")

private val metricSelectorPattern = Regex("""([a-zA-Z_:][a-zA-Z0-9_:]*)\{([^}]*)}""")
private val labelMatcherPattern = Regex("""([a-zA-Z_][a-zA-Z0-9_]*)(=~|!~|!=|=)\"([^\"]*)\"""")
private val labelListPattern = Regex("""(by|on)\(([^)]*)\)""")

private data class LabelMatcher(
    val operator: String,
    val value: String,
)

private data class MetricSelector(
    val metric: String,
    val matchers: Map<String, LabelMatcher>,
) {
    fun canonical(): String =
        "$metric{" +
            matchers
                .toSortedMap()
                .entries
                .joinToString(",") { (name, matcher) ->
                    "$name${matcher.operator}\"${matcher.value}\""
                } +
            "}"
}

private fun String.selectors(metric: String): List<MetricSelector> =
    metricSelectorPattern
        .findAll(this.replace(Regex("\\s+"), ""))
        .map { it.value.toMetricSelector() }
        .filter { it.metric == metric }
        .toList()

private fun String.toMetricSelector(): MetricSelector {
    val selector = metricSelectorPattern.matchEntire(this) ?: error("Ugyldig metric selector: $this")
    val matchers =
        selector.groupValues[2]
            .split(',')
            .filter { it.isNotBlank() }
            .associate { matcherText ->
                val matcher = labelMatcherPattern.matchEntire(matcherText) ?: error("Ugyldig label matcher: $matcherText")
                matcher.groupValues[1] to LabelMatcher(matcher.groupValues[2], matcher.groupValues[3])
            }
    return MetricSelector(selector.groupValues[1], matchers)
}

private fun String.canonicalPromql(): String {
    val compact = replace(Regex("\\s+"), "")
    val selectorsCanonical = metricSelectorPattern.replace(compact) { it.value.toMetricSelector().canonical() }
    return labelListPattern.replace(selectorsCanonical) { match ->
        val labels =
            match.groupValues[2]
                .split(',')
                .sorted()
                .joinToString(",")
        "${match.groupValues[1]}($labels)"
    }
}

private fun MetricSelector.shouldHaveMatchers(expected: Map<String, LabelMatcher>) {
    expected.forEach { (name, matcher) -> matchers[name] shouldBe matcher }
}

private fun MetricSelector.shouldHaveProdDeploymentLabels() {
    shouldHaveMatchers(
        mapOf(
            "k8s_cluster_name" to LabelMatcher("=", "prod"),
            "namespace" to LabelMatcher("=", "team-esyfo"),
            "deployment" to LabelMatcher("=", "syfomotebehov"),
        ),
    )
}

private fun MetricSelector.shouldHaveProdServerSpanLabels() {
    shouldHaveMatchers(
        mapOf(
            "service_namespace" to LabelMatcher("=", "team-esyfo"),
            "service_name" to LabelMatcher("=", "syfomotebehov"),
            "k8s_cluster_name" to LabelMatcher("=", "prod"),
            "span_kind" to LabelMatcher("=", "SPAN_KIND_SERVER"),
        ),
    )
}
