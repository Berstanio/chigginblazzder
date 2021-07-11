package com.fittonia.chigginblazzder


import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.FixtureDef


/** First ps of the application. Displayed after the application is created.  */
class PlayScreen(val game: MainApp, val assets: Assets) : Screen {

  public enum class GameState {
    RUNNING,
    PAUSED
  }

  //val RUNNING = 1
  val PAUSED = 0
  var gameState: GameState = GameState.RUNNING;
  val width = 128f
  var height = width*(Gdx.graphics.height.toFloat()/Gdx.graphics.width.toFloat())
  val GROUND_HEIGHT = (-height/2)+5f // 5f = 10f/2. Ground body height is 10f...
  var cam = OrthographicCamera(width,height)
  val sr = ShapeRenderer()
//  var debugRenderer = Box2DDebugRenderer()
  var pausedBatch = SpriteBatch()

  var chickens        = arrayListOf<Chicken>()
  var dedChickens     = ArrayList<Chicken>()
  var livingChickens  = ArrayList<Chicken>()

  var font = BitmapFont()
  var popParticleEffect   = ParticleEffect()
  var dustParticleEffect  = ParticleEffect()
  lateinit var dustPool: ParticleEffectPool
  lateinit var popPool: ParticleEffectPool
  var dustEffects = Array<PooledEffect>()
  var popEffects = Array<PooledEffect>()
  val glyph = GlyphLayout()

  /** Object to be instanced in show()*/
  lateinit var world: World
  lateinit var staticBody: Body

  /** the ground */
  lateinit var bodyDef: BodyDef

  var spriteBatch: SpriteBatch?   = null
  var fbo: FrameBuffer?           = null
  var mouseAttack: Body?          = null /** eg. shoot. created on mouseclicks */

  lateinit var cursor: Sprite
  lateinit var ground: Sprite
  lateinit var cloudsFG: Sprite
  lateinit var cloudsBG: Sprite
  lateinit var background: Sprite
  lateinit var newHighScore: Sprite
  val gameData = GameDataMachine(game.isCursorVisible)
  val showDebugBox2D  = false

  private lateinit var music: Music
  private lateinit var gameOverMusic: Music

  var initCamX = 0f
  var initCamY = 0f

  var bgCloudScroll = 0f
  var bgCloud2Scroll = 0f
  var bgStarsScroll = 0f

  var backToTitle = false

  var prefs = Gdx.app.getPreferences("chigginblazzder.scores")
  private var highScore = 0

  override fun show() {
    initializeFBO()
    loadHighScoreFromPreferences()
    initCursor()

    /** FONT MAKING */
    font = assets.getFont()
    font.data.setScale(0.3f)

    /** Init that box2d world. Add ground. */
//    Box2D.init()
    world = World(Vector2(0f,-70f), true)
    createGround()

    /** Init Input Processor. */
    Gdx.input.inputProcessor = InputMachine(this)

    /** Init Particle Effects. */
    popParticleEffect = assets.manager.get("particleEffects/pop.p")
    dustParticleEffect = assets.manager.get("particleEffects/dustcloud.particle")
    dustParticleEffect.setEmittersCleanUpBlendFunction(false)
    popParticleEffect.scaleEffect(width/Gdx.graphics.width)
    dustParticleEffect.scaleEffect(width/Gdx.graphics.width*2)
    dustPool = ParticleEffectPool(dustParticleEffect,2,8)
    popPool = ParticleEffectPool(popParticleEffect,2,4)
    val effect = popPool.obtain()
    popEffects.add(effect)

    initCamX = cam.position.x
    initCamY = cam.position.y

    music = assets.getGamePlayMusic()
    music.volume = 3f/5f
    music.isLooping = true
    if (assets.isMusicOn) {
      music.play()
    }

    gameOverMusic = assets.getGameOverMusic()
    gameOverMusic.volume = 3f/5f
    gameOverMusic.isLooping = false
  }


  override fun render(delta: Float) {
    handleGameMusic()
    presentRunning()
  }

  fun presentRunning() {
    if (!gameData.isGameOver) { // if not game over
      if (gameState == GameState.RUNNING) {
        handleChickenSpawning()
        handleScreenShake()
        world.step(Gdx.graphics.deltaTime, 6, 2)
        removeDeadChickens()
      }
      spriteBatch?.projectionMatrix = cam.combined
      sr.projectionMatrix = cam.combined

      cam.update()
      drawSceneToFBO()
      /** Clear ps and draw FBO texture to ps for pixelated dustEffects */
      Gdx.gl20.glClearColor(0f, 0f, 0f, 0f)
      Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)

      spriteBatch?.projectionMatrix?.setToOrtho2D(0f, 0f, width, height)
      spriteBatch?.begin()
      spriteBatch?.draw(fbo?.colorBufferTexture, 0f, 0f, width, height, 0f, 0f, 1f, 1f)
      spriteBatch?.end()

//      if (showDebugBox2D) debugRenderer.render(world, cam.combined)
      if (mouseAttack != null) {
        world.destroyBody(mouseAttack)
        mouseAttack = null
      }

    } else { // game over!
      spriteBatch?.projectionMatrix = cam.combined
      sr.projectionMatrix = cam.combined
      cam.update()
      handleChickenSpawning()
      if (gameState == GameState.RUNNING) world.step(Gdx.graphics.deltaTime, 6, 2)
      removeDeadChickens()
      drawSceneToFBO()
      /** Clear ps and draw FBO texture to ps for pixelated dustEffects */
      Gdx.gl20.glClearColor(1f, 1f, 1f, 1f)
      Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)

      spriteBatch?.projectionMatrix?.setToOrtho2D(0f, 0f, width, height)
      spriteBatch?.begin()
      spriteBatch?.draw(fbo?.colorBufferTexture, 0f, 0f, width, height, 0f, 0f, 1f, 1f)
      spriteBatch?.end()

//      if (showDebugBox2D) debugRenderer.render(world, cam.combined)

    }
  }

  /** Simple screen shake for juiciness */
  fun handleScreenShake() {
    if (System.currentTimeMillis() - gameData.scrShakeTimer < 75) {
      val offSetX = if (ThreadLocalRandom.current().nextFloat() > 0.5f) -0.5f else 0.5f
      val offSetY = if (ThreadLocalRandom.current().nextFloat() > 0.5f) -0.5f else 0.5f
      val x = cam.position.x + offSetX
      val y = cam.position.y + offSetY
      if (x > 4 || x < -4 || y > 4 || y < -4)
        cam.position.set(initCamX, initCamY, 0f)
      else
        cam.position.set(x,y,0f)
    } else {
      cam.position.set(initCamX, initCamY, 0f)
    }
  }

  /** Initially a chicken will spawn every 600ms.
   * As kills go up, the spawn rate time decreases */
  fun handleChickenSpawning() {
    val now = System.currentTimeMillis()
    val timeToSpawn = gameData.timeToSpawn + (ThreadLocalRandom.current().nextLong(100))
    if (now - gameData.spawnRedChickenTimer - gameData.timePaused > gameData.canSpawnRedChicken) {
      chickens.add(RedChicken(world,this))
      gameData.spawnRedChickenTimer = now
      gameData.canSpawnRedChicken = gameData.generateNextRedChickenTime()
      gameData.timePaused = 0L
    }
    if (now - gameData.spawnTimer > timeToSpawn ) {
      val c = Chicken(world,this)
      c.initChicken()
      chickens.add(c)
      if (gameData.spawnBigChicken) {
        if (gameData.numBigChickens < gameData.bigChickenLimit) {
          val bc = BigChicken(world, this)
          bc.initChicken()
          chickens.add(bc)
          gameData.spawnBigChicken = false
          gameData.numBigChickens++
        }
      }
      gameData.spawnTimer = now
    }
  }

  private fun loadHighScoreFromPreferences() {
    highScore = prefs.getInteger("HiScore", -1)
    if (highScore < 0) {
      prefs.putInteger("HiScore", 0)
      highScore = 0
    }
    prefs.flush()
  }

  private fun handleGameMusic() {
    if (assets.isMusicOn)  {
      if (gameState == GameState.PAUSED) {
        if (music.isPlaying) music.volume = 0f
        if (gameOverMusic.isPlaying) gameOverMusic.volume = 0f
        if (!assets.getGamePauseMusic().isPlaying) {
          assets.getGamePauseMusic().volume = assets.masterVol
          assets.getGamePauseMusic().play()
        }
      } else {
        if (assets.getGamePauseMusic().isPlaying) assets.getGamePauseMusic().stop()
        music.volume = assets.masterVol
        gameOverMusic.volume = assets.masterVol
      }
      if (gameData.isGameOver) {
        if (music.isPlaying) music.stop()
        if (!gameOverMusic.isPlaying &&  gameData.gameOverMusicTimesPlayed < 1) {
          gameOverMusic.play()
          gameData.gameOverMusicTimesPlayed = 1
        }
      } else {
        if (!music.isPlaying) music.play()
        if (gameOverMusic.isPlaying) gameOverMusic.stop()
      }
      if(!gameData.isGameOver && music.isPlaying) music.play()
    } else {
      if (music.isPlaying) music.stop()
      if (gameOverMusic.isPlaying) gameOverMusic.stop()
    }
  }

  /** Set up frame buffer object to render to offscreen, twice as large
   * (in world units) to create pixelated effect. */
  private fun initializeFBO() {
    if (fbo != null) fbo?.dispose()
    fbo = FrameBuffer(Pixmap.Format.RGBA8888,width.toInt()*2, height.toInt()*2, false)
    fbo?.colorBufferTexture?.setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
    if (spriteBatch != null) spriteBatch?.dispose()
    spriteBatch = SpriteBatch()
  }

  /** Load up background art (clouds, ground, etc) */
  private fun initBGSprites() {
    ground = Sprite(assets.manager.get("bg_ground.png", Texture::class.java))
    cloudsFG = Sprite(assets.manager.get("bg_clouds_FG.png", Texture::class.java))
    cloudsBG = Sprite(assets.manager.get("bg_clouds_BG.png", Texture::class.java))
    background = Sprite(assets.manager.get("bg_background.png", Texture::class.java))

    ground.setOriginCenter()
    cloudsFG.setRegion(0,0,256,256)
    cloudsBG.setRegion(0,0,256,256)
    background.setRegion(0,0,256,256)
  }

  private fun drawSceneToFBO() {
    this.fbo?.begin()
    Gdx.gl20.glClearColor(.21f, .35f, 0.65f, 1f)
    Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT)
    spriteBatch?.begin()
    spriteBatch?.projectionMatrix?.setToOrtho2D(0f,0f, fbo?.width!!.toFloat(), fbo?.height!!.toFloat())
    /** Draw Background (ground, cloudsFG, background) */
    val w = width +10
    val h = height + 10
    val scrlspd = 0.15f
    cloudsFG.setRegion(bgCloudScroll.toInt(),0,256,256)
    cloudsBG.setRegion(bgCloud2Scroll.toInt(),0,256,256)
    background.setRegion(bgStarsScroll.toInt(),0,256,256)
    spriteBatch?.draw(background,ground.x-w/2,-h/2, w, h)
    spriteBatch?.draw(cloudsBG,ground.x-w/2,-h/2, w, h)
    spriteBatch?.draw(cloudsFG,ground.x-w/2,-h/2, w, h)
    spriteBatch?.draw(ground,ground.x-w/2, (ground.y-h/2)-(height*0.85f)+3f, w, h)

    if (gameState == GameState.RUNNING) {
      bgCloudScroll += scrlspd
      bgCloud2Scroll += scrlspd / 4f
      bgStarsScroll -= scrlspd / 2f
    }
    if (bgCloudScroll > 768) bgCloudScroll = 0f
    if (bgCloud2Scroll > 768) bgCloud2Scroll = 0f
    if (bgStarsScroll < 0) bgStarsScroll = 768f

    /** Draw Score / text stuff behind chickens*/
    displayUIText()

    /** Draw Chickens */
    for (c in chickens) {
      c.didChickenCross()
      if (gameState == GameState.RUNNING) {
        c.update()
      }
      val sb = spriteBatch
      if (sb != null && !c.isDead ) {
        c.draw(sb)
      }
    }
    /** Draw Particles */
    for (i in dustEffects.size - 1 downTo 0) {
      val effect = dustEffects.get(i)
      effect.draw(spriteBatch, Gdx.graphics.deltaTime)
      if (effect.isComplete) {
        effect.free()
        dustEffects.removeIndex(i)
      }
    }
    for (i in popEffects.size - 1 downTo 0) {
      val effect = popEffects.get(i)
      effect.draw(spriteBatch, Gdx.graphics.deltaTime)
      if (effect.isComplete) {
        effect.free()
        popEffects.removeIndex(i)
      }
    }
    spriteBatch?.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA)

    /** Draw "return to title" on top of chickens */
    if (gameData.isGameOver) {
      displayReturnToTitleText()
    }

    /** Draw player's cursor if playing on Desktop */
    if (gameData.showCursor) {
      val mpos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
      cam.unproject(mpos)
      cursor.setOriginCenter()
      cursor.setOriginBasedPosition(mpos.x, mpos.y)
      spriteBatch?.draw(cursor, cursor.x, cursor.y)
    }
    spriteBatch?.end()

    /** Draw player's shot*/
    val m = mouseAttack
    if (m != null) {
      sr.begin(ShapeRenderer.ShapeType.Filled)
      sr.setColor(1f,1f,1f,1f)
      sr.circle(m.position.x, m.position.y, 5f)
      sr.end()
    }
    fbo?.end()
  }

  private fun removeDeadChickens() {
    if (gameData.didChickenDie) {
      dedChickens = ArrayList()
      livingChickens = ArrayList()
      for (c in chickens) {
        if (c.isDead) {
          dedChickens.add(c)
          world.destroyBody(c.body)
        } else {
          livingChickens.add(c)
        }
      }
      chickens.clear()
      chickens = livingChickens
      dedChickens.removeAll({it.isDead})
      gameData.didChickenDie = false
    }
  }

  /** Scores, "click to reset", etc... */
  private fun displayUIText() {
    if (gameState == GameState.PAUSED) {
      glyph.setText(font,gameData.pausedText)
      font.data.setScale(0.3f,0.3f)
      font.color = Color.valueOf("#44891A")
      font.draw(spriteBatch, gameData.pausedText, -glyph.width/2, 0f)
      font.color = Color.WHITE
      displayGameText()
    } else if (gameData.isGameOver) {
      if (isNewHighScore()) {
        prefs.putInteger("HiScore", gameData.currentScore)
        prefs.flush()
      }
      displayGameOverText()

    } else {
      displayGameText()
    }
  }

  private fun displayGameOverText() {
    val sinY = Math.sin(System.currentTimeMillis() * 0.005).toFloat()
    val y = (height/2*0.75f)+sinY
    val gameoverText = "Game Over"
    val killCountText = "${gameData.currentScore}"

    font.data.setScale(0.3f,0.3f)
    glyph.setText(font, killCountText)
    font.draw(spriteBatch, killCountText, -glyph.width/2, y)

    if (isNewHighScore()) {
      spriteBatch?.draw(newHighScore, -newHighScore.width/2, y-glyph.height-7)
      prefs.putInteger("HiScore", gameData.currentScore)
    }

    font.color = Color.valueOf("#44891A")
    glyph.setText(font, gameoverText)

    if (System.currentTimeMillis() - gameData.gameOverTimer > 1500L) {
      // reset game and flush new high score to prefs file
      if (Gdx.input.isTouched) {
        if (isNewHighScore()) {
          highScore = gameData.currentScore
        }
        prefs.flush()
      }
      glyph.setText(font, gameData.tryAgainText)
      font.draw(spriteBatch, gameData.tryAgainText,-glyph.width/2, sinY)
    } else {
      font.draw(spriteBatch, gameoverText, -glyph.width/2, sinY)
    }
    font.setColor(1f,1f,1f,1f)
  }

  private fun displayReturnToTitleText() {
    val sinY = Math.sin(System.currentTimeMillis() * 0.005).toFloat()
//    val y = (height/2*0.75f)+sinY

    if (System.currentTimeMillis() - gameData.gameOverTimer > 1500L) {
      // reset game and flush new high score to prefs file
      if (Gdx.input.isTouched) {
        if (isNewHighScore()) {
          highScore = gameData.currentScore
        }
        prefs.flush()
      }
      font.data.setScale(0.15f,0.15f)
      glyph.setText(font,gameData.returnToTitleText)
      font.color = Color.WHITE
      font.draw(spriteBatch, gameData.returnToTitleText,-glyph.width/2, -21f+(sinY*0.5f))
      font.data.setScale(0.3f,0.3f)
    }
    font.setColor(1f,1f,1f,1f)
  }

  private fun displayGameText() {
    val sinY = Math.sin(System.currentTimeMillis() * 0.005).toFloat() // animates score
    val posY = (height/2*0.75f)+sinY
    var scoreText = "${gameData.currentScore}"
    font.data.setScale(0.3f,0.3f)
    glyph.setText(font, scoreText)

    if (isNewHighScore()) {
      spriteBatch?.draw(newHighScore, -newHighScore.width/2, posY-glyph.height-7)
    }
    handleRedChickenScoreColor()
    font.draw(spriteBatch, scoreText ,-glyph.width/2, posY)
    font.color = Color(Color.WHITE)
  }

  /** If a red chicken is hit, score turns red, and then gradually shifts to white again */
  fun handleRedChickenScoreColor() {
    if (gameData.didHitRedChicken) {
      /** No longer than 1 second */
      if (System.currentTimeMillis() - gameData.hitRedChickenTimer < 1000) {
        font.setColor(gameData.redChickenAlertColor)
        if (gameData.redChickenAlertColor.g > gameData.redChickenAlertColor.r){
          font.setColor(Color.WHITE)
        } else if (gameData.redChickenAlertColor.g < gameData.redChickenAlertColor.r) {
          gameData.redChickenAlertColor.g += 10 / 255f
          gameData.redChickenAlertColor.b += 10 / 255f
        }
      }
    } else {
      font.setColor(Color.WHITE)
    }
  }
  fun isNewHighScore(): Boolean {
    return gameData.currentScore > highScore
  }

  /** Reset game variables and destroy all physics bodies and chickens */
  fun resetGame() {
    gameData.resetGameData()
    var bodies = Array<Body>()
    world.getBodies(bodies)
    for (i in 0..bodies.size-1) {
      world.destroyBody(bodies[i])
    }
    chickens.clear()
    createGround()
    music.position = 0f
    if (!music.isPlaying) {
      if (assets.isMusicOn) music.play()
    }
    if (gameOverMusic.isPlaying) {
      gameOverMusic.stop()
      gameOverMusic.position = 0f
    }
  }

  fun createGround() {
    bodyDef = BodyDef()
    bodyDef.type = BodyDef.BodyType.StaticBody
    bodyDef.position.set(0f, -height/2)
    staticBody = world.createBody(bodyDef)
    val boxShape = PolygonShape()
    boxShape.setAsBox(270f,10f)
    val fixtureDef = FixtureDef()
    fixtureDef.shape = boxShape
    fixtureDef.friction = 0.02f
    fixtureDef.restitution = 0.2f
    fixtureDef.filter.categoryBits = 2
    fixtureDef.filter.maskBits = 1.or(4).or(0x0008).or(0x0010).or(0x0020)
    staticBody.createFixture(fixtureDef)
    world.setContactListener(CollisionListener(this))
    staticBody.userData = "ground"
    boxShape.dispose()
  }

  override fun resize(w: Int, h: Int) {
    height = width * (Gdx.graphics.height.toFloat()/Gdx.graphics.width.toFloat())
    initializeFBO()
    cam = OrthographicCamera(width,height)
  }

  fun initCursor() {
    val clearCursor = Pixmap(16,16, Pixmap.Format.RGBA8888)
    clearCursor.setColor(1f,1f,1f,0f)
    clearCursor.fill()
    cursor = Sprite(assets.manager.get("ui_crosshair.png", Texture::class.java))
    newHighScore = Sprite(assets.manager.get("text_newhighscore.png", Texture::class.java))
    val customCursor = Gdx.graphics.newCursor(clearCursor, 0, 0)
    Gdx.graphics.setCursor(customCursor)
    initBGSprites()
    clearCursor.dispose()
  }
  override fun pause() {
    if (gameState == GameState.RUNNING) {
      gameData.timePaused = System.currentTimeMillis()
      gameState = GameState.PAUSED
    }
  }
  override fun resume() { }

  override fun hide() { }

  override fun dispose() {
    assets.dispose()
    prefs.flush()
  }
}