/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.browser

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** A bounded DNS/TCP probe: no credentials, media, TLS bypass or network scanning. */
object NetworkDiagnostics {
    fun check(url: String): String {
        val uri = runCatching { URI(url) }.getOrNull() ?: return "诊断失败：地址格式无效"
        val host = uri.host ?: return "诊断失败：地址没有主机名"
        val port = if (uri.port > 0) uri.port else if (uri.scheme == "https") 443 else 80
        val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "tv-dns-probe").apply { isDaemon = true } }
        val addresses = try {
            worker.submit<Array<InetAddress>> { InetAddress.getAllByName(host) }.get(3, TimeUnit.SECONDS)
        } catch (error: Exception) {
            return "DNS：失败 · ${error.cause?.javaClass?.simpleName ?: error.javaClass.simpleName}\n主机：$host\n若 IP 可打开而名称不行，故障在名称解析环节。"
        } finally { worker.shutdownNow() }
        val results = addresses.take(3).map { address ->
            try {
                Socket().use { it.connect(InetSocketAddress(address, port), 2000) }
                "${address.hostAddress}:$port · TCP 可连接"
            } catch (error: Exception) { "${address.hostAddress}:$port · ${error.javaClass.simpleName}" }
        }
        return "DNS：成功\n" + results.joinToString("\n") + "\nTCP 成功只说明端口可连接，页面故障请结合上面的 WebView 错误。"
    }
}
