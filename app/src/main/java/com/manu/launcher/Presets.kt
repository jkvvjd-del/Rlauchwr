package com.manu.launcher

object Presets {

    fun fps120(): String = """
    {
      "DFIntTaskSchedulerTargetFps": 120,
      "FFlagDebugDisplayFPS": "True"
    }
    """.trimIndent()

    fun potato(): String = """
    {
      "DFIntTaskSchedulerTargetFps": 60,
      "DFIntDebugFRMQualityLevelOverride": 1,
      "FIntDebugForceMSAASamples": 0,
      "DFFlagTextureQualityOverrideEnabled": "True",
      "DFIntTextureQualityOverride": 1,
      "FFlagDisablePostFx": "True",
      "FFlagDebugSkyGray": "True"
    }
    """.trimIndent()

    fun graySky(): String = """
    {
      "FFlagDebugSkyGray": "True",
      "FFlagDebugDisplayFPS": "True"
    }
    """.trimIndent()

    fun lowGfx(): String = """
    {
      "DFIntDebugFRMQualityLevelOverride": 1,
      "FIntDebugForceMSAASamples": 0,
      "DFFlagTextureQualityOverrideEnabled": "True",
      "DFIntTextureQualityOverride": 1
    }
    """.trimIndent()
}
