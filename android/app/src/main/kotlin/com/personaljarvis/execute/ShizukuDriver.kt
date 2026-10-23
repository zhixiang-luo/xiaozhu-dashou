package com.personaljarvis.execute

import com.personaljarvis.policy.DriverResult
import com.personaljarvis.policy.RuntimeAction

interface ShizukuBridge {
    val isAvailable: Boolean
    fun execute(action: RuntimeAction): DriverResult
    fun cancel()
}

class ShizukuDriver(
    private val bridge: ShizukuBridge? = null
) : ExecutionDriver {
    override val isAvailable: Boolean
        get() = bridge?.isAvailable == true

    override fun execute(action: RuntimeAction): DriverResult {
        val activeBridge = bridge
        if (activeBridge?.isAvailable != true) {
            return DriverResult.Unsupported("shizuku_unavailable")
        }
        return activeBridge.execute(action)
    }

    override fun cancel() {
        bridge?.cancel()
    }
}
