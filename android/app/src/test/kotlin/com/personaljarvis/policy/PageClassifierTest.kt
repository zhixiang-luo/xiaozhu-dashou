package com.personaljarvis.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class PageClassifierTest {
    @Test
    fun classifiesPaymentPasswordScreen() {
        val page = PageClassifier.classify(
            packageName = "com.example",
            texts = listOf("请输入支付密码"),
            hasSensitiveInput = true
        )

        assertEquals(PageKind.PAYMENT, page)
    }

    @Test
    fun classifiesOtpScreenOnlyWithSensitiveInput() {
        val page = PageClassifier.classify(
            packageName = "com.example",
            texts = listOf("请输入验证码"),
            hasSensitiveInput = true
        )

        assertEquals(PageKind.OTP, page)
    }

    @Test
    fun keepsNormalChatNormal() {
        val page = PageClassifier.classify(
            packageName = "com.tencent.mm",
            texts = listOf("张三：晚上一起吃饭吗", "发送"),
            hasSensitiveInput = false
        )

        assertEquals(PageKind.NORMAL, page)
    }
}
