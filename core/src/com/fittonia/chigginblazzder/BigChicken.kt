package com.fittonia.chigginblazzder

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import java.util.*

class BigChicken(world: World, screen: PlayScreen) : Chicken(world,screen) {

  var isBigHit = false

  override fun initChicken() {
    //super.initChicken()
    DEATH_ANIM_TIME = ((Math.random()*(1500-1000+1))+1500).toLong()
    health = ((Math.random()*(5-3+1))+3).toInt()
    max = 18
    min = 12
    w = ((Math.random()*(max-min+1))+min).toInt()
    h = w

    contacting = false
    isDead = false
    isDying = false
    direction = Random().nextInt(2)
    isMovingleft = (direction == LEFT)

    var pixmap = Pixmap(w,h, Pixmap.Format.RGBA8888)
    box_sprite = Sprite(Texture(pixmap))
    box_sprite.setOriginBasedPosition(if (direction == LEFT) SPAWN_RIGHT+10f else SPAWN_LEFT-10f, SPAWN_HEIGHT+w*3)

    /** Create a shadow */
    shadow = Sprite(Texture( createShadow() ))

    /** Create a Chicken Sprite */
    when {
      w > 14 -> {
        chicken_sprite = Sprite(ps.assets.getBigChickenTexture(64, if (isMovingleft) "R" else "L"))
        chicken_sprite.regionHeight = 60
        chicken_sprite.regionWidth = 60
      }
      else -> {
        chicken_sprite = Sprite(ps.assets.getBigChickenTexture(32, if (isMovingleft) "R" else "L"))
        chicken_sprite.regionHeight = 30
        chicken_sprite.regionWidth = 30
      }
    }

    chicken_sprite.scale(box_sprite.width/w)
    chicken_sprite.color = color
    if (!isMovingleft) {
      chicken_sprite.flip(true,false)
    }

    initBox2D()
    pixmap.dispose()
  }

  override fun initBox2D() {
    val bodyDef = BodyDef()
    bodyDef.type = BodyDef.BodyType.DynamicBody
    bodyDef.angle = Math.toRadians(Math.random()*360.0).toFloat()
    bodyDef.position.set(box_sprite.x,box_sprite.y)
    body = world.createBody(bodyDef)


    val circshape = CircleShape()
    circshape.radius = w.toFloat()
    val boxshape = PolygonShape()
    boxshape.setAsBox(w.toFloat(),h.toFloat())

    /** Set collision filter */
    fixtureDef.filter.categoryBits = if (isMovingleft) 0x0008 else 0x0010
    if (isMovingleft)
      fixtureDef.filter.maskBits = 1.or(2).or(4)
    else
      fixtureDef.filter.maskBits = 1.or(2).or(4)
    fixtureDef.shape = circshape
    fixtureDef.density = 0.15f
    fixtureDef.friction = 0.2f
    fixtureDef.restitution = 0.85f
    body.userData = this
    body.createFixture(fixtureDef)
    if (isMovingleft) {
      body.applyLinearImpulse(-60f, 10f,body.position.x, body.position.y,true)
    } else {
      body.applyLinearImpulse(60f, 10f,body.position.x, body.position.y, true)
    }
    boxshape.dispose()
    circshape.dispose()
  }

  override fun draw(batch: SpriteBatch) {
    box_sprite.setOriginBasedPosition(body.position.x, body.position.y)
    shadow.setOriginCenter()
    shadow.setPosition(body.position.x-shadow.width/2, ps.GROUND_HEIGHT+5f-artOffset)
    batch.draw(shadow, shadow.x, shadow.y, shadow.width, 0.5f)
    box_sprite.rotation = Math.toDegrees(body.angle.toDouble()).toFloat()
    if (isBigHit) {
      batch.color = Color.ORANGE
      isBigHit = false
    }

    batch.draw(chicken_sprite,
        box_sprite.x,
        box_sprite.y,
        box_sprite.originX,
        box_sprite.originY,
        w.toFloat(),             //  width
        h.toFloat(),             //  height
        chicken_sprite.scaleX,   //  scaleX
        chicken_sprite.scaleY,   //  scaleY
        box_sprite.rotation
    )
    batch.color = Color.WHITE
    if (ps.gameState == PlayScreen.GameState.RUNNING) displayDeathText(batch)
  }

  override fun displayDeathText(batch: SpriteBatch) {
    if (isDying) {
      ps.font.data.setScale(0.28f,0.28f)
      ps.font.setColor(deathTextColor)
      if (deathTextColor.g < .99f) {
        deathTextColor.r += 2f / 255f
        deathTextColor.g += 2f / 255f
      }
      if (deathTextColor.r > deathTextColor.b || deathTextColor.g > deathTextColor.b) {
        deathTextColor = Color.WHITE
      }
      val multiplierNum = "${multiplier}"
      glyph.setText(ps.font, multiplierNum)
      ps.font.draw(batch, multiplierNum,deathCoords.x-glyph.width/2, deathCoords.y+h+10)
      deathCoords.y += 0.25f
      ps.font.setColor(Color.WHITE)
      ps.font.data.setScale(0.3f,0.3f)
    }
  }

  override fun update() {
    if (isDying) {
      var filter = Filter()
      filter.categoryBits = 4
      filter.maskBits = 2
      var mass = MassData()
      mass.mass = 2f
      body.massData = mass
      body.fixtureList.first().filterData = filter
      if (System.currentTimeMillis() - deathTime > DEATH_ANIM_TIME ) {
        isDead = true
        ps.gameData.didChickenDie = true
      }
    } else {
      val mlef    = Vector2(-20f, 0f)
      val mright  = Vector2( 20f, 0f)
      body.applyLinearImpulse(
          if (isMovingleft) mlef else mright,
          body.position,
          true
      )
      val vel = body.linearVelocity
      if (vel.x >  15f)  vel.x =  15f
      if (vel.x < -15f)  vel.x = -15f
      body.linearVelocity = vel
    }
  }

  override fun die() {
    super.die()
    ps.gameData.numBigChickens--
    body.angularVelocity = body.angularVelocity * 7f
    body.linearVelocity = body.linearVelocity.scl(10f,25f)
    deathCoords = body.position.cpy()
    multiplier = ps.gameData.multiplier * 5
  }

  override fun hit() {
    health--
    if (health <= 0) {
      die()
    }
    ps.assets.playRandomBigChickenHitSound()
    ps.gameData.scrShakeTimer = System.currentTimeMillis()
    isBigHit = true
  }



}