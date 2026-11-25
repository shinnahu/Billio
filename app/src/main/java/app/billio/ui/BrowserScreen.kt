package app.billio.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.billio.R
import app.billio.data.Bill
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Everything a person can read on the page: its text, plus what is shown inside boxes. Never passwords. */
private const val PAGE_TEXT = """
(function () {
  function read(doc) {
    var out = '';
    try {
      if (doc.body) out += doc.body.innerText + '\n';
      var boxes = doc.querySelectorAll('input:not([type=password]):not([type=hidden]), output');
      for (var i = 0; i < boxes.length; i++) { if (boxes[i].value) out += boxes[i].value + '\n'; }
      var frames = doc.querySelectorAll('iframe');
      for (var j = 0; j < frames.length; j++) { try { out += read(frames[j].contentDocument); } catch (e) {} }
    } catch (e) {}
    return out;
  }
  return read(document);
})()
"""

/** Writes a value into the box the person tapped, the way typing would, so the page notices it. */
private fun fillFocused(value: String): String = """
(function (v) {
  var e = document.activeElement;
  if (!e || (e.tagName !== 'INPUT' && e.tagName !== 'TEXTAREA') || e.type === 'password') return false;
  var proto = e.tagName === 'INPUT' ? HTMLInputElement.prototype : HTMLTextAreaElement.prototype;
  Object.getOwnPropertyDescriptor(proto, 'value').set.call(e, v);
  e.dispatchEvent(new Event('input', { bubbles: true }));
  e.dispatchEvent(new Event('change', { bubbles: true }));
  return true;
})(${Json.encodeToString(value)})
"""

/**
 * The provider's own website, opened inside the app. The person signs in or types their number there,
 * as in any browser; when the bill is on screen, "Find amount" reads the page text and offers the amounts in it.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    bill: Bill,
    handleBack: Boolean,
    onClose: () -> Unit,
    onPageText: (String) -> Unit,
    onCopyAccount: () -> Unit,
) {
    StatusBarIcons(white = false)
    val context = LocalContext.current
    var progress by remember { mutableIntStateOf(0) }
    var address by remember { mutableStateOf(bill.checkUrl) }
    var canGoBack by remember { mutableStateOf(false) }
    var tip by remember { mutableStateOf(false) }

    fun openOutside(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    val web = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportMultipleWindows(false)
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val scheme = request.url.scheme.orEmpty()
                    if (scheme == "http" || scheme == "https") return false
                    // Links meant for another app (wallet, bank, phone, mail) open there.
                    openOutside(request.url.toString())
                    return true
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                    address = url.orEmpty()
                    canGoBack = view.canGoBack()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress = newProgress
                }
            }
            // Bill PDFs and other files are handed to the phone's browser, which knows how to save them.
            setDownloadListener { url, _, _, _, _ -> openOutside(url) }
            loadUrl(bill.checkUrl)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            web.stopLoading()
            web.destroy()
        }
    }
    BackHandler(enabled = handleBack && canGoBack) { web.goBack() }

    // imePadding: when the keyboard opens the page gets shorter, so the box being typed in stays in view.
    Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White).imePadding()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(Icons.Rounded.Close, stringResource(R.string.close), onClose)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(bill.name, color = Palette.Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (address.startsWith("https://")) {
                        Icon(Icons.Rounded.Lock, null, tint = Palette.Muted, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(host(address), color = Palette.Muted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            CircleButton(Icons.Rounded.Refresh, stringResource(R.string.reload), { web.reload() })
            Spacer(Modifier.width(8.dp))
            CircleButton(Icons.AutoMirrored.Rounded.OpenInNew, stringResource(R.string.browser_open_external), { openOutside(address) })
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Palette.Line)) {
            if (progress in 1..99) Box(Modifier.fillMaxWidth(progress / 100f).height(2.dp).background(Palette.Blue))
        }

        AndroidView(factory = { web }, modifier = Modifier.weight(1f).fillMaxWidth())

        Column(Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.White).navigationBarsPadding().padding(12.dp)) {
            Hint(
                stringResource(if (tip) R.string.browser_tap_field else R.string.browser_hint),
                Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (bill.accountNumber.isNotBlank()) {
                    SoftButton(
                        stringResource(R.string.browser_paste),
                        onClick = {
                            web.evaluateJavascript(fillFocused(bill.accountNumber)) { done ->
                                if (done != "true") {
                                    onCopyAccount()
                                    tip = true
                                }
                            }
                        },
                        leading = Icons.Rounded.ContentPaste,
                        modifier = Modifier.weight(1f),
                    )
                }
                PrimaryButton(
                    stringResource(R.string.browser_find),
                    onClick = {
                        web.evaluateJavascript(PAGE_TEXT) { result ->
                            onPageText(runCatching { Json.decodeFromString<String>(result ?: "\"\"") }.getOrDefault(""))
                        }
                    },
                    icon = Icons.Rounded.Search,
                    modifier = Modifier.weight(1.25f),
                )
            }
        }
    }
}

fun host(url: String): String = runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.") ?: url
