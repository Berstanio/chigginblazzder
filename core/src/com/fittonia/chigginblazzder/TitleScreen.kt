package com.fittonia.chigginblazzder;

import com.badlogic.gdx.*
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils.*
import com.badlogic.gdx.math.Vector3

class TitleScreen(val game: MainApp, val assets: Assets) : Screen {

  val width = 128f
  var height = width*(Gdx.graphics.height.toFloat() / Gdx.graphics.width.toFloat())
  var cam = OrthographicCamera(width,height)

  var batch: SpriteBatch? = null
  var fbo: FrameBuffer?   = null

  lateinit var font: BitmapFont
  private val sr = ShapeRenderer()
  private lateinit var titleSprite: Sprite
  private lateinit var tapstartSprite: Sprite
  private lateinit var highscoreSprite: Sprite
  private lateinit var musicSprite: Sprite
  private lateinit var noMusicSprite: Sprite
  private lateinit var versionSprite: Sprite

  private var highscore = 0
  private lateinit var music: Music
  private var isMusic = true
  private lateinit var prefs: Preferences

  override fun hide() {
  }

  override fun show() {
    initializeFBO()
    initHighScore()
    font = assets.getFont()
    font.data.setScale(0.25f)

    titleSprite = Sprite(assets.manager.get("title_sprite.png", Texture::class.java))
    titleSprite.setOriginCenter()
    titleSprite.setScale(0.45f)
    titleSprite.setOriginBasedPosition(0f, height*0.22f)

    tapstartSprite = Sprite(assets.manager.get("text_taptostart.png", Texture::class.java))
    tapstartSprite.setPosition(-tapstartSprite.width/2, -height*0.4f)

    highscoreSprite = Sprite(assets.manager.get("text_highscore.png", Texture::class.java))
    highscoreSprite.setOriginCenter()
    highscoreSprite.setScale(0.5f)
    highscoreSprite.setOriginBasedPosition(0f,height*0.125f*0.25f)

    musicSprite = Sprite(assets.manager.get("text_music.png", Texture::class.java))
    musicSprite.setPosition(width*0.475f-musicSprite.width, -height*0.4f)

    noMusicSprite = Sprite(assets.manager.get("text_nomusic.png", Texture::class.java))
    noMusicSprite.setPosition(width*0.475f-noMusicSprite.width, -height*0.4f)

    versionSprite = Sprite(assets.manager.get("text_version.png", Texture::class.java))
    versionSprite.setPosition(-width*0.4f-(versionSprite.width/2+1), -height*0.4f)

    music = assets.getTitleMusic()
    music.volume = 2.5f/5f
    music.isLooping = true

    if (assets.isMusicOn) {
      music.play()
    }

    Gdx.input.inputProcessor = object : InputAdapter() {

      override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val touch = Vector3(screenX.toFloat(), screenY.toFloat(),0f)
        cam.unproject(touch)
        if (touch.x > (width*.4f*0.5f) && touch.y < (-height*0.4f*0.5f)) {
          assets.isMusicOn = !assets.isMusicOn
        } else  {
          if (music.isPlaying) music.stop()
          prefs.putBoolean("music", assets.isMusicOn)
          prefs.flush()
          game.screen = PlayScreen(game,assets)
        }
        return super.touchUp(screenX, screenY, pointer, button)
      }
    }
  }

  private fun initHighScore() {
    prefs = Gdx.app.getPreferences("chigginblazzder.scores")
    highscore = prefs.getInteger("HiScore", -1)
    isMusic = prefs.getBoolean("music", true)
    assets.isMusicOn = isMusic
    if (highscore < 0) highscore = 0
  }

  override fun render(delta: Float) {
    if (assets.isMusicOn) {
      if (!music.isPlaying) music.play()
    } else {
      music.stop()
    }

    cam.update()
    sr.projectionMatrix = cam.combined
    batch?.projectionMatrix = cam.combined
    drawSceneToFBO()
    val col = Color.valueOf("#31A2F2")
    Gdx.gl20.glClearColor(col.r, col.g, col.b, 0f)
    Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)

    batch?.projectionMatrix?.setToOrtho2D(0f, 0f, width, height)
    batch?.begin()
    batch?.draw(fbo?.colorBufferTexture, 0f, 0f, width, height, 0f, 0f, 1f, 1f)
    batch?.end()
  }

  private fun initializeFBO() {
    if (fbo != null) fbo?.dispose()
      print("width $width")
      print("height $height")
    fbo = FrameBuffer(Pixmap.Format.RGBA8888,width.toInt()*2, height.toInt()*2, false)
    fbo?.colorBufferTexture?.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    if (batch != null) batch?.dispose()
    batch = SpriteBatch()
  }

  private fun drawSceneToFBO() {
    this.fbo?.begin()
    val col = Color.valueOf("#31A2F2")
    Gdx.gl20.glClearColor(col.r, col.g, col.b, 0f)
    Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)
    batch?.begin()
    batch?.projectionMatrix?.setToOrtho2D(0f,0f, fbo?.width!!.toFloat(), fbo?.height!!.toFloat())

    val glyph = GlyphLayout()
    var sinMove = sin(Gdx.graphics.frameId.toFloat()*0.05f)*1.5f
    val offsetY= -(width/2)*0.175f

    glyph.setText(font, "${highscore}")
    batch?.draw(
        highscoreSprite,
        highscoreSprite.x,
        highscoreSprite.y+offsetY,
        highscoreSprite.originX,
        highscoreSprite.originY,
        highscoreSprite.width,
        highscoreSprite.height,
        highscoreSprite.scaleX,
        highscoreSprite.scaleY,
        0f
        )

    font.draw(batch,
      "${highscore}", -glyph.width/2,offsetY-highscoreSprite.height+(4))

    batch?.draw(tapstartSprite,
        tapstartSprite.x,
        tapstartSprite.y,
        tapstartSprite.originX,
        tapstartSprite.originY,
        tapstartSprite.width,
        tapstartSprite.height,
        0.5f,
        0.5f,
        0f
    )
    batch?.color = Color.WHITE
    batch?.draw(titleSprite,
        titleSprite.x,
        titleSprite.y+sinMove,
        titleSprite.originX,
        titleSprite.originY,
        titleSprite.width,
        titleSprite.height,
        titleSprite.scaleX,
        titleSprite.scaleY,
        0f)
    if (!assets.isMusicOn) {
      batch?.draw(musicSprite,
          musicSprite.x,
          musicSprite.y,
          musicSprite.originX,
          musicSprite.originY,
          musicSprite.width,
          musicSprite.height,
          0.5f,
          0.5f,
          0f)
    } else {
      batch?.draw(noMusicSprite,
          noMusicSprite.x,
          noMusicSprite.y,
          noMusicSprite.originX,
          noMusicSprite.originY,
          noMusicSprite.width,
          noMusicSprite.height,
          0.5f,
          0.5f,
          0f)
    }
    batch?.draw(versionSprite,
        versionSprite.x,
        versionSprite.y,
        versionSprite.originX,
        versionSprite.originY,
        versionSprite.width,
        versionSprite.height,
        0.5f,
        0.5f,
        0f)
    batch?.end()
    fbo?.end()
  }

  override fun pause() {
  }

  override fun resume() {
  }

  override fun resize(width: Int, height: Int) {
    initializeFBO()
  }

  override fun dispose() {
    assets.dispose()
    fbo?.dispose()
  }

}
