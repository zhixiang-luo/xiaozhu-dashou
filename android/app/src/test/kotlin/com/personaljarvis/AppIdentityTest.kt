package com.personaljarvis

import org.junit.Assert.assertEquals
import org.junit.Test

class AppIdentityTest {
    @Test
    fun appIdentityUsesPersonalJarvisName() {
        assertEquals("Personal Jarvis", AppIdentity.displayName)
    }

    @Test
    fun appIdentityUsesStablePackage() {
        assertEquals("com.personaljarvis", AppIdentity.applicationId)
    }
}
