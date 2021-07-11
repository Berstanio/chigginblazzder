package com.fittonia.chigginblazzder

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.fittonia.chigginblazzder.PlayScreen.GameState.RUNNING
import java.util.*
import java.util.concurrent.ThreadLocalRandom

open class Chicken(var world: World, val ps: PlayScreen) {

  val artOffset = 1.5f
  val LEFT  = 0
  val SPAWN_RIGHT  = (ps.width/2)+20f
  val SPAWN_LEFT   = (-ps.width/2)-20f
  val SPAWN_HEIGHT = ps.GROUND_HEIGHT+15f
  val TINY_CHICKEN_THRESH = 2
  val glyph = GlyphLayout()
  val fixtureDef = FixtureDef()

  var direction = 0
  var max = 0
  var min = 0
  var w = 0
  var h = 0
  var contacting = false
  var isDead  = false
  var isDying = false
  var isMovingleft = false
  var color = Color.WHITE
  var deathCoords = Vector2()
  var multiplier = 0
  var DEATH_ANIM_TIME = ((Math.random()*(1000-500+1))+500).toLong()
  var stateTime = 0f
  var deathTime = 0L
  var health = 0
  val superHopHeight = ThreadLocalRandom.current().nextDouble(40.0,60.0).toFloat()
  var deathTextColor = Color.WHITE

  lateinit var shadow: Sprite
  lateinit var box_sprite: Sprite
  lateinit var chicken_sprite: Sprite
  lateinit var body: Body
  lateinit var walkAnim: Animation<TextureRegion>
  lateinit var walkSheet: Texture

  var isSuperHopping = false
  var superHopTimer = 0L


  open fun initChicken() {
    health = 1
    max = 4
    min = TINY_CHICKEN_THRESH
    w = ((Math.random()*(max-min+1))+min).toInt()
    h = w
    contacting = false
    isDead = false
    isDying = false
    direction = Random().nextInt(2)
    isMovingleft = (direction == LEFT)

    /** If w is less than two, this chicken will be a tiny sprite chicken */
    if (w < 2) {
      w = TINY_CHICKEN_THRESH
      h = w
    }

    var pixmap = Pixmap(w,h, Pixmap.Format.RGBA8888)
    box_sprite = Sprite(Texture(pixmap)) // <- mostly for debug stuff
    box_sprite.setOriginBasedPosition(if (isMovingleft) SPAWN_RIGHT else SPAWN_LEFT, SPAWN_HEIGHT)

    /** Create a shadow */
    shadow = Sprite(Texture( createShadow() ))

    /** Create a Chicken Sprite */
    if (w < TINY_CHICKEN_THRESH) {
      chicken_sprite = Sprite(ps.assets.manager.get("chicken_body.png", Texture::class.java))
    } else {
      if (isMovingleft)
        chicken_sprite = Sprite(ps.assets.manager.get("chicken_tiny_R.png", Texture::class.java))
      else
        chicken_sprite = Sprite(ps.assets.manager.get("chicken_tiny_L.png", Texture::class.java))
    }

    chicken_sprite.scale(box_sprite.width/w)
    chicken_sprite.setOriginBasedPosition(if (isMovingleft) SPAWN_RIGHT else SPAWN_LEFT, SPAWN_HEIGHT)
    chicken_sprite.color = color

    if (!isMovingleft) {
      chicken_sprite.flip(true,false)
    }

    loadAnimation()
    initBox2D()
    pixmap.dispose()

  }

  open fun initBox2D() {
    val bodyDef = BodyDef()
    bodyDef.type = BodyDef.BodyType.DynamicBody
    bodyDef.position.set(chicken_sprite.x,chicken_sprite.y)
    body = world.createBody(bodyDef)

    val shape = PolygonShape()
    shape.setAsBox(w.toFloat()/2f,h.toFloat()/2f)

    /** Set collision filter */
    fixtureDef.filter.categoryBits = 1
    if (isMovingleft) {
      fixtureDef.filter.maskBits = 2.or(4).or(0x0010)
    } else {
      fixtureDef.filter.maskBits = 2.or(4).or(0x0008)
    }

    fixtureDef.shape = shape
    body.linearDamping = 0.3f

    if (isMovingleft) {
      body.applyLinearImpulse(-15f, 10f,body.position.x, body.position.y,true)
    } else {
      body.applyLinearImpulse(15f, 10f,body.position.x, body.position.y, true)
    }
    val angSpeed = ThreadLocalRandom.current().nextDouble(3.0,7.0).toFloat()
    body.angularVelocity = if (isMovingleft) angSpeed else -angSpeed
    body.createFixture(fixtureDef)
    body.userData = this
    shape.dispose()
  }

  open fun loadAnimation() {
    /** Trying out some animation stuff here */
    val rows = 2
    val cols = 4
    // Load the sprite sheet as a Texture
    walkSheet = if (isMovingleft) {
      ps.assets.manager.get("chicken_frames_R.png", Texture::class.java)
    } else {
      ps.assets.manager.get("chicken_frames_L.png", Texture::class.java)
    }

    // Use the split utility method to create a 2D array of TextureRegions. This is
    // possible because this sprite sheet contains frames of equal size and they are
    // all aligned.
    val tmp = TextureRegion.split(walkSheet,
        walkSheet.width / cols,
        (walkSheet.height/2) / rows)

    // Place the regions into a 1D array in the correct order, starting from the top
    // left, going across first. The Animation constructor requires a 1D array.
    val walkFrames = arrayOfNulls<TextureRegion>(cols * rows)
    var index = 0
    for (i in 0 until rows) {
      for (j in 0 until cols) {
        walkFrames[index++] = tmp[i+2][j]
      }
    }

    // Initialize the Animation with the frame interval and array of frames
    walkAnim = Animation<TextureRegion>(0.065f, *walkFrames)
  }

  fun createShadow(): Pixmap {
    var pixmap = Pixmap(w*2, 1, Pixmap.Format.RGBA8888)
    pixmap.setColor(Color.valueOf("#44891A"))
    pixmap.fillRectangle(0,0,pixmap.width,pixmap.height)
    return pixmap
  }

  open fun update() {
    if (isDying) {
      var filter = Filter()
      filter.categoryBits = 4
      filter.maskBits = 1.or(2)
      var mass = MassData()
      mass.mass = 0.5f
      body.massData = mass
      body.fixtureList.first().filterData = filter
      body.fixtureList.first().density = 0.001f
      if(System.currentTimeMillis() - deathTime > DEATH_ANIM_TIME) {
        isDead = true
        ps.gameData.didChickenDie = true
      }
    } else {
      if (Math.random() > 0.9995f && w > TINY_CHICKEN_THRESH) superHop()
      else if (Math.random() > 0.997f) hop()
      if (Math.random() > 0.925f) {
        val vel = body.linearVelocity
        if (isMovingleft && vel.x < -6f)
          body.applyLinearImpulse(Vector2(-3.0f,0f), body.position, true)
        if (!isMovingleft && vel.x > 6f)
          body.applyLinearImpulse(Vector2(3.0f,0f), body.position, true)
      }

    }
  }

  fun hop() {
    val linSpeed = ThreadLocalRandom.current().nextDouble(4.0,7.0).toFloat()
    body.applyLinearImpulse(if (direction == LEFT) -linSpeed else linSpeed, 20f,body.position.x, body.position.y,true)
    if (body.position.x < ps.width/2 && body.position.x > -ps.width/2) {
      ps.assets.playRandomChirp()
    }
  }

  fun superHop() {
    isSuperHopping = System.currentTimeMillis() - superHopTimer < 3000L
    if (!isSuperHopping) {
      superHopTimer = System.currentTimeMillis()
      val linSpeed = ThreadLocalRandom.current().nextDouble(4.0, 7.0).toFloat()
      body.applyLinearImpulse(if (direction == LEFT) -linSpeed else linSpeed, superHopHeight, body.position.x, body.position.y, true)
      if (body.position.x < ps.width / 2 && body.position.x > -ps.width / 2) {
        ps.assets.playRandomChirp()
      }
    }
  }

  open fun draw(batch: SpriteBatch) {
    box_sprite.setOriginBasedPosition(body.position.x, body.position.y)
    chicken_sprite.setOriginBasedPosition(body.position.x, body.position.y)

    shadow.setOriginCenter()
    shadow.setPosition(body.position.x-shadow.width/2, ps.GROUND_HEIGHT+5f-artOffset)
    batch.draw(shadow, shadow.x, shadow.y, shadow.width, 0.50f)

    box_sprite.rotation = Math.toDegrees(body.angle.toDouble()).toFloat()

    /** This ensures our sprite does get drawn lower than its shadow*/
    var posY = body.position.y
    if (body.position.y-h < ps.GROUND_HEIGHT) {
      posY = ps.GROUND_HEIGHT+h
    }
    ColorChickenIfCloseToOtherSide()
    if (w > TINY_CHICKEN_THRESH) {

      if (ps.gameState == RUNNING) {
        stateTime += Gdx.graphics.deltaTime
      }
      val currentFrame = walkAnim.getKeyFrame(stateTime, true)
      if (!isMovingleft && !currentFrame.isFlipX) currentFrame.flip(true, false)
      batch.color = color
      batch.draw(currentFrame,
          box_sprite.x,
          posY-artOffset,
          box_sprite.originX,
          box_sprite.originY,
          w.toFloat(),             //  width
          h.toFloat(),             //  height
          chicken_sprite.scaleX,   //  scaleX
          chicken_sprite.scaleY,   //  scaleY
          if (!isDying) 0f else box_sprite.rotation)
      batch.color = Color.WHITE
    } else {
      batch.color = color
      batch.draw(chicken_sprite,
          box_sprite.x,
          posY-artOffset,
          box_sprite.originX,
          box_sprite.originY,
          w.toFloat(),             //  width
          h.toFloat(),             //  height
          chicken_sprite.scaleX,   //  scaleX
          chicken_sprite.scaleY,   //  scaleY
          if (!isDying) 0f else box_sprite.rotation)
      batch.color = Color.WHITE
    }
    if (ps.gameState == RUNNING) displayDeathText(batch)
  }

  open fun displayDeathText(batch: SpriteBatch) {
    if (isDying && !ps.gameData.isGameOver) {
      ps.font.color = deathTextColor
      if (deathTextColor.g < .99f) {
        deathTextColor.r += 2f / 255f
        deathTextColor.g += 2f / 255f
      }
      if (deathTextColor.r > deathTextColor.b || deathTextColor.g > deathTextColor.b) {
        deathTextColor = Color.WHITE
      }

      ps.font.data.setScale(0.125f,0.125f)
      val multiplierNum = "$multiplier"
      glyph.setText(ps.font, multiplierNum)
      ps.font.draw(batch, multiplierNum,deathCoords.x-glyph.width/2, deathCoords.y+h+10)
      deathCoords.y += 0.25f
      ps.font.color = Color.WHITE
      ps.font.data.setScale(0.3f,0.3f)
    }
  }

  open fun ColorChickenIfCloseToOtherSide() {

    // TODO
    // Add flag if chicken is close to edge. Result will be arrow drawn above chicken?
//    val width = (ps.width/2)
//    val distanceFromEdge = ps.width*0.2
//    if (isMovingleft) {
//      when {
//        (body.position.x < (-width-w)+distanceFromEdge && !isDying) -> {
//          color = Color.RED
//        }
//      }
//    } else {
//      when {
//        (body.position.x > (width)+w-distanceFromEdge && !isDying) -> {
//          color = Color.RED
//        }
//      }
//    }
  }

  open fun didChickenCross() {
    if (isMovingleft) {
      when {
        (body.position.x < -(ps.width/2)-w && !isDying) -> {
          if (!ps.gameData.isGameOver) {
            ps.gameData.isGameOver = true
            ps.gameData.gameOverTimer = System.currentTimeMillis()
          }
        }
      }
    } else {
      when {
        (body.position.x > (ps.width/2)+w && !isDying) -> {
          if (!ps.gameData.isGameOver) {
            ps.gameData.isGameOver = true
            ps.gameData.gameOverTimer = System.currentTimeMillis()
          }
        }
      }
    }
  }

  open fun die() {
    deathTime = System.currentTimeMillis()
    isDying = true
    contacting = true
    body.applyLinearImpulse(0f, 25.5f, body.position.x, body.position.y, false)
    ps.assets.playRandomExplosion()

    if (!ps.gameData.isGameOver)
      ps.gameData.chickenShot(this)

    deathCoords = body.position.cpy()
    multiplier = ps.gameData.multiplier
    deathTextColor = Color.valueOf("#31A2F2")
  }

  open fun hit() {
    health--
    if (health <= 0) { die() }
    ps.gameData.scrShakeTimer = System.currentTimeMillis()
    val effect = ps.popPool.obtain()
    val scalefctr = w * 0.5f
    if (this is BigChicken) {
      effect.scaleEffect(scalefctr*0.5f)
    } else {
      effect.scaleEffect(scalefctr)
    }
    effect.setPosition(body.position.x, body.position.y)
    ps.popEffects.add(effect)
  }

}