package com.personaljarvis.policy

object PageClassifier {
    private val paymentMarkers = listOf(
        "支付密码",
        "确认支付",
        "立即付款",
        "收付款",
        "付款码"
    )
    private val otpMarkers = listOf("验证码", "动态码", "一次性密码")
    private val loginMarkers = listOf("登录密码", "账号登录", "手机号登录")
    private val permissionPackages = setOf(
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller"
    )

    fun classify(
        packageName: String,
        texts: List<String>,
        hasSensitiveInput: Boolean
    ): PageKind {
        val joined = texts.joinToString("\n")

        if (
            packageName in permissionPackages ||
            joined.contains("请求权限") ||
            joined.contains("允许访问")
        ) {
            return PageKind.PERMISSION
        }
        if (paymentMarkers.any(joined::contains)) {
            return PageKind.PAYMENT
        }
        if (
            hasSensitiveInput &&
            otpMarkers.any(joined::contains)
        ) {
            return PageKind.OTP
        }
        if (
            hasSensitiveInput &&
            loginMarkers.any(joined::contains)
        ) {
            return PageKind.LOGIN
        }
        return PageKind.NORMAL
    }
}
