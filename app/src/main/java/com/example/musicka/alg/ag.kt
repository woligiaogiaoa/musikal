package com.example.musicka.alg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.webkit.JavascriptInterface
import android.webkit.WebView

//用户协议，隐私协议
@JavascriptInterface
fun setEventFunction(type: String) { //1 用户协议 2.隐私协议
    if (webViewWeakReference.get() == null) {
        return
    }
    val webView: WebView = webViewWeakReference.get()
    if (webView.context is Activity) {
        var url = ""
        if (type == "1") {
            url = PublicationSDK.goodsAndPrivacy.getPtl()
        }
        if (type == "2") {
            url = PublicationSDK.goodsAndPrivacy.getPvy()
        }
        if (!TextUtils.isEmpty(url)) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                webView.context.startActivity(browserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}