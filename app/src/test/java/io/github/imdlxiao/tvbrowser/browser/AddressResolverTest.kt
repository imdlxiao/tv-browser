/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.browser

import org.junit.Assert.*
import org.junit.Test

class AddressResolverTest {
    @Test fun `blank input does not navigate`() {
        assertNull(AddressResolver.resolve("   "))
    }

    @Test fun `domain gets https and preserves path query and fragment`() {
        assertEquals("https://example.com/path?q=tv#top", AddressResolver.resolve(" example.com/path?q=tv#top "))
        assertEquals("https://example.com:8080/video", AddressResolver.resolve("example.com:8080/video"))
    }

    @Test fun `explicit http and https are preserved`() {
        assertEquals("http://192.168.1.2:8000", AddressResolver.resolve("http://192.168.1.2:8000"))
        assertEquals("https://example.com", AddressResolver.resolve("https://example.com"))
    }

    @Test fun `keywords including punctuation are encoded`() {
        assertEquals("https://www.baidu.com/s?wd=%E7%94%B5%E8%A7%86+%26+4K",
            AddressResolver.resolve("电视 & 4K"))
        assertEquals("https://www.baidu.com/s?wd=hello+world.com", AddressResolver.resolve("hello world.com"))
    }

    @Test fun `executable local and external application schemes are blocked`() {
        listOf("javascript:alert(1)", "file:///sdcard/key", "content://private/file",
            "intent://example.com", "data:text/html,hello", "mailto:a@example.com").forEach {
            assertNull(it, AddressResolver.resolve(it))
            assertFalse(AddressResolver.isWebUrl(it))
        }
    }

    @Test fun `malformed authority and embedded credentials are rejected`() {
        assertNull(AddressResolver.resolve("https://"))
        assertNull(AddressResolver.resolve("https://user:password@example.com"))
        assertNull(AddressResolver.resolve("https://exa mple.com"))
    }
}
