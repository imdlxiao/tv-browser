/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.browser

import java.net.URI
import java.net.URLEncoder
import java.util.Locale

/** Pure address/search policy; executable and local-file schemes are rejected. */
object AddressResolver {
    fun resolve(input: String): String? {
        val value = input.trim()
        if (value.isEmpty()) return null
        val hasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(value)
        val isHostWithPort = Regex("^[^/\\s:]+\\.[^/\\s:]+:\\d+.*$").matches(value)
        if (hasScheme && !isHostWithPort) {
            val scheme = value.substringBefore(':').lowercase(Locale.ROOT)
            if (scheme != "http" && scheme != "https") return null
            return value.takeIf(::isWebUrl)
        }
        if ('.' in value && value.none(Char::isWhitespace)) {
            val host = runCatching { URI("http://$value").host.orEmpty().lowercase(Locale.ROOT) }.getOrDefault("")
            val octets = host.split('.').mapNotNull { it.toIntOrNull() }
            val privateIpv4 = octets.size == 4 && octets.all { it in 0..255 } &&
                (octets[0] == 10 || (octets[0] == 192 && octets[1] == 168) || (octets[0] == 172 && octets[1] in 16..31))
            val local = host.endsWith(".local") || privateIpv4
            return "${if (local) "http" else "https"}://$value".takeIf(::isWebUrl)
        }
        return "https://www.baidu.com/s?wd=" + URLEncoder.encode(value, "UTF-8")
    }

    fun isWebUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.lowercase(Locale.ROOT) in setOf("http", "https") &&
            !uri.rawAuthority.isNullOrBlank() && uri.rawUserInfo == null &&
            value.none(Char::isWhitespace)
    }.getOrDefault(false)
}
