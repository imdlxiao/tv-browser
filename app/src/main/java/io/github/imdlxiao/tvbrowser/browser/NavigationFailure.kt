/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.browser

import java.net.URI

/** Preserve the engine's actual error, without presenting guesses as facts. */
data class NavigationFailure(val code: String, val description: String, val url: String, val advice: String) {
    fun display(): String = "$code · $description\n$url\n$advice"

    companion object {
        fun web(code: Int, description: String, url: String): NavigationFailure {
            val local = runCatching { URI(url).host?.endsWith(".local", true) == true }.getOrDefault(false)
            val (name, advice) = when {
                description.contains("CLEARTEXT", true) -> "CLEARTEXT_BLOCKED" to "系统阻止了 HTTP 明文访问，请检查浏览器网络策略。"
                code == -2 -> "ERROR_HOST_LOOKUP" to if (local)
                    "电视未能解析 .local 名称。旧系统可能不支持 mDNS；请用“修改地址”输入电脑的局域网 IP 对照测试，也要检查同网段和组播。"
                    else "域名解析失败，请检查网址、DNS 和网络连接。"
                code == -6 -> "ERROR_CONNECT" to "连接服务器失败。检查电脑是否开机、网站是否启动、端口和防火墙；百度可访问不能证明局域网可达。"
                code == -8 -> "ERROR_TIMEOUT" to "连接超时。检查同一 Wi-Fi、访客网络/AP 隔离、VPN 或防火墙。"
                code == -11 -> "ERROR_FAILED_SSL_HANDSHAKE" to "TLS 握手失败。检查系统时间和证书；不能用 HTTPS 打开只提供 HTTP 的 8765 端口。"
                code == -10 -> "ERROR_UNSUPPORTED_SCHEME" to "网址协议不支持，请使用 http:// 或 https://。"
                else -> "WEBVIEW_ERROR" to "请保留此错误码与原始描述，使用连接诊断进一步区分域名、网络和服务故障。"
            }
            return NavigationFailure("$name ($code)", description, safeUrl(url), advice)
        }

        fun safeUrl(url: String): String = runCatching {
            val uri = URI(url)
            URI(uri.scheme, null, uri.host, uri.port, uri.path, null, null).toString()
        }.getOrDefault("地址格式无效")
    }
}
