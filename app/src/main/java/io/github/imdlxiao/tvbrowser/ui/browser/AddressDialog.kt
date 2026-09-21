/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.ui.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import io.github.imdlxiao.tvbrowser.browser.AddressResolver

@Composable
fun AddressDialog(current: String, dismiss: () -> Unit, canSave: Boolean, open: (String, Boolean) -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue(current, TextRange(0, current.length))) }
    var error by remember { mutableStateOf(false) }
    var save by remember { mutableStateOf(canSave) }
    val focus = remember { FocusRequester() }
    fun submit() {
        val address = AddressResolver.resolve(text.text)
        if (address == null || !AddressResolver.isWebUrl(address)) error=true else open(address, save)
    }
    AlertDialog(onDismissRequest=dismiss, title={ Text("修改访问地址") }, text={
        Column {
            LaunchedEffect(Unit) { focus.requestFocus() }
            Text(".local 不能解析时，可输入素材电脑的局域网 IP 和端口，例如 http://电脑IP:8765/。地址固定需在路由器保留电脑的 DHCP 地址。")
            OutlinedTextField(value=text, onValueChange={ text=it; error=false }, singleLine=true,
                modifier=Modifier.focusRequester(focus), label={ Text("网址") }, isError=error,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Uri, imeAction=ImeAction.Go),
                keyboardActions=KeyboardActions(onGo={ submit() }))
            if (error) Text("请输入有效的 HTTP(S) 地址")
            if (canSave) Row(Modifier.toggleable(value=save, onValueChange={ save=it })) {
                Checkbox(checked=save, onCheckedChange=null)
                Text("同时更新首页收藏地址")
            }
        }
    }, confirmButton={ TextButton(onClick={ submit() }) { Text("打开地址") } },
       dismissButton={ TextButton(onClick=dismiss) { Text("取消") } })
}
