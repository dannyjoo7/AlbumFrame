package kr.joolabs.albumframe

import android.app.Application

class MomentFrameApplication : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}
