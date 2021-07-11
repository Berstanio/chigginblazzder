package com.fittonia.chigginblazzder

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class LoadingScreen (val game: MainApp, val assets: Assets) : Screen {

  var fbo: FrameBuffer? = null
  var batch: SpriteBatch? = null
  var sr = ShapeRenderer()
  lateinit var logoSprite: Sprite
  var showing = false

  val width = 100f
  var height = width*(Gdx.graphics.height.toFloat() / Gdx.graphics.width.toFloat())
  var cam = OrthographicCamera(width,height)
  val col = Color.valueOf("#31A2F2")
  var fittoniaSound: Sound? = null

  override fun hide() {}

  override fun show() {
    logoSprite = Sprite(Texture(Gdx.files.internal("fittonia_logo.png")))
    logoSprite.setOriginCenter()
    logoSprite.setOriginBasedPosition(0f,5f)

    initializeFBO()
    assets.loadAssets()
    fittoniaSound = Gdx.audio.newSound(Gdx.files.internal("sounds/fittonia_logo_sound.wav"))
  }

  override fun render(delta: Float) {
    if (!showing) {
      showing = true
      fittoniaSound?.play(4f/5f)
    }

    val loadingComplete = assets.manager.update()
    if (loadingComplete) {
      fittoniaSound?.dispose()
      game.screen = TitleScreen(game,assets)
    }
    cam.update()
    sr.projectionMatrix = cam.combined
    batch?.projectionMatrix = cam.combined
    drawSceneToFBO()

    Gdx.gl20.glClearColor(col.r,col.g,col.b,col.a)
    Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)

    batch?.projectionMatrix?.setToOrtho2D(0f, 0f, width, height)
    batch?.begin()
    batch?.draw(fbo?.colorBufferTexture, 0f, 0f, width, height, 0f, 0f, 1f, 1f)
    batch?.end()
  }

  private fun initializeFBO() {
    // (re)Init the FBO
    if (fbo != null) fbo?.dispose()
    fbo = FrameBuffer(Pixmap.Format.RGBA8888,200, 200, false)
    fbo?.colorBufferTexture?.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

    // (re)Init the spritebatch
    if (batch != null) batch?.dispose()
    batch = SpriteBatch()
  }

  private fun drawSceneToFBO() {
    this.fbo?.begin()

    Gdx.gl20.glClearColor(col.r,col.g,col.b,col.a)
    Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)

    val progressBarWidth = (assets.manager.progress * 40)

    batch?.begin()
    batch?.projectionMatrix?.setToOrtho2D(0f,0f, fbo?.width!!.toFloat(), fbo?.height!!.toFloat())
    logoSprite.draw(batch)
    batch?.end()

    sr.begin(ShapeRenderer.ShapeType.Filled)
    sr.setColor(Color.WHITE)
    sr.rect(-25f+5,-18f, progressBarWidth, 2f)
    sr.end()
    fbo?.end()
  }

  override fun pause() {
  }

  override fun resume() {
  }

  override fun resize(width: Int, height: Int) {
  }

  override fun dispose() {
    assets.dispose()
    fbo?.dispose()
  }
}