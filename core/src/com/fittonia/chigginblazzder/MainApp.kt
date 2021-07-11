package com.fittonia.chigginblazzder

import com.badlogic.gdx.Application
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx

/** [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms.  */
class MainApp(val isCursorVisible: Boolean) : Game() {

  val assets = Assets()
  lateinit var playScreen: LoadingScreen
  val game = this

  override fun create() {
    Gdx.app.logLevel = Application.LOG_NONE
    playScreen = LoadingScreen(this, assets)
    setScreen(LoadingScreen(this, assets))
  }

  override fun dispose() {
    playScreen.dispose()
    assets.dispose()
  }
}