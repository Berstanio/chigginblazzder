package com.fittonia.chigginblazzder

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import java.util.*

import com.fittonia.chigginblazzder.PlayScreen.GameState.*;

class RedChicken(world: World, screen: PlayScreen) : Chicken(world,screen) {

  init {
    initChicken()
  }

  override fun initChicken() {
    health = 1
    max = 4
    min = 2
    w = ((Math.random()*(max-min+1))+min).toInt()
    h = w
    contacting = false
    isDead = false
    isDying = false
    direction = Random().nextInt(2)
    isMovingleft = (direction == LEFT)
    var pixmap = Pixmap(w,h, Pixmap.Format.RGBA8888)

    /** box sprite mostly used for debugging, and used as the */
    box_sprite = Sprite(Texture(pixmap))
    box_sprite.setOriginBasedPosition(if (isMovingleft) SPAWN_RIGHT else SPAWN_LEFT, SPAWN_HEIGHT)

    /** Create a shadow */
    shadow = Sprite(Texture( createShadow() ))

    chicken_sprite = Sprite(ps.assets.manager.get("chicken_body.png", Texture::class.java))
    chicken_sprite.setOriginBasedPosition(if (isMovingleft) SPAWN_RIGHT else SPAWN_LEFT, SPAWN_HEIGHT)
    chicken_sprite.scale((box_sprite.width/w))
    chicken_sprite.color = color

    if (!isMovingleft) {
      chicken_sprite.flip(true,false)
    }

    initBox2D()
    var filter = Filter()
    filter.categoryBits = 0x0020
    filter.maskBits = 1.or(2)
    body.fixtureList.first().filterData = filter
    loadAnimation()
    pixmap.dispose()
  }

  override fun update() {
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
      if (Math.random() > 0.998f) hop()
      if (Math.random() > 0.94f) {
        val vel = body.linearVelocity
        if (isMovingleft && vel.x < -5f)
          body.applyLinearImpulse(Vector2(-6f,0f), body.position, true)
        if (!isMovingleft && vel.x > 5f)
          body.applyLinearImpulse(Vector2(6f,0f), body.position, true)
      }
    }
  }

  override fun draw(batch: SpriteBatch) {
    box_sprite.setOriginBasedPosition(body.position.x, body.position.y)
    chicken_sprite.setOriginBasedPosition(body.position.x, body.position.y)

    shadow.setOriginCenter()
    shadow.setPosition(body.position.x-shadow.width/2, ps.GROUND_HEIGHT+5f-artOffset)
    batch.draw(shadow, shadow.x, shadow.y, shadow.width, 0.50f)

    chicken_sprite.rotation = Math.toDegrees(body.angle.toDouble()).toFloat()

    var posY = body.position.y

    if (body.position.y-h < ps.GROUND_HEIGHT) {
      posY = ps.GROUND_HEIGHT+h
    }

    if (ps.gameState == RUNNING) {
      stateTime += Gdx.graphics.deltaTime
    }
    val currentFrame = walkAnim.getKeyFrame(stateTime, true)

    if (!isMovingleft && !currentFrame.isFlipX) currentFrame.flip(true, false)
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
    if (ps.gameState == RUNNING) displayDeathText(batch)
  }

  override fun didChickenCross() {
    if (isMovingleft) {
      if (body.position.x < -(ps.width/2)-w && !isDying) { madeItAcrossBonus() }
    } else {
      if (body.position.x > (ps.width/2)+w && !isDying) { madeItAcrossBonus() }
    }
  }

  /** If red chicken crosses screen, score multiplier goes up!*/
  fun madeItAcrossBonus() {
    if (!isDead) {
      ps.gameData.multiplier++
      isDead = true
      ps.assets.playMultiplierIncreaseSound()
    }
  }

  override fun die() {
    super.die()
    ps.assets.playHitRedChickenSound()
    val effect = ps.popPool.obtain()
    effect.scaleEffect(2f)
    val glyph = GlyphLayout()
    glyph.setText(ps.font,"${ps.gameData.currentScore}")
    effect.setPosition(0f, (ps.height/2*0.75f)-glyph.height/2)
    ps.popEffects.add(effect)
  }

  override fun displayDeathText(batch: SpriteBatch) {
    if (isDying) {
      ps.font.data.setScale(0.5f, 0.5f)
      ps.font.setColor(Color.valueOf("#BE2633"))
      val multiplierNum = "!"
      glyph.setText(ps.font, multiplierNum)
      ps.font.draw(batch, multiplierNum, deathCoords.x - glyph.width / 2, deathCoords.y + h + 10)
      deathCoords.y += 0.25f
      ps.font.setColor(Color.WHITE)

      ps.gameData.didHitRedChicken = true
      ps.gameData.hitRedChickenTimer = System.currentTimeMillis()
      ps.gameData.redChickenAlertColor = Color.valueOf("#BE2633")
      ps.font.data.setScale(0.3f,0.3f)
    }
  }

  override fun loadAnimation() {
    /** Trying out some animation stuff here */
    val FRAME_ROWS = 2
    val FRAME_COLS = 4
    // Load the sprite sheet as a Texture
    walkSheet = ps.assets.manager.get("redChicken_frames.png", Texture::class.java)

    val tmp = TextureRegion.split(walkSheet,
        walkSheet.getWidth() / FRAME_COLS,
        walkSheet.getHeight()/2 / FRAME_ROWS)

    val walkFrames = arrayOfNulls<TextureRegion>(FRAME_COLS * FRAME_ROWS)
    var index = 0
    for (i in 0 until FRAME_ROWS) {
      for (j in 0 until FRAME_COLS) {
        // i+2 to get the art from the last two rows
        walkFrames[index++] = tmp[i][j]
      }
    }

    walkAnim = Animation<TextureRegion>(0.075f, *walkFrames)
  }

}