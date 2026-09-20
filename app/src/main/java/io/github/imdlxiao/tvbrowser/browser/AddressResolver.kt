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
            return "https://$value".takeIf(::isWebUrl)
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