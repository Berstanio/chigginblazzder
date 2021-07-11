package com.fittonia.chigginblazzder

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.fittonia.chigginblazzder.PlayScreen.GameState.*;

class InputMachine(val ps: PlayScreen): InputAdapter() {
  val gd = ps.gameData


  override fun keyDown(keycode: Int): Boolean {
    if (ps.gameState == RUNNING) {
      ps.gameState = PAUSED;
      return true;
    } else {
      ps.gameState = RUNNING;
      return true;
    }

    return false
  }

  override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
    if (!gd.isGameOver && ps.gameState == RUNNING) shoot(x.toFloat(), y.toFloat())
    return true
  }

  override fun touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean {
    if (ps.gameState == PAUSED) {
      ps.gameState = RUNNING
      gd.timePaused = System.currentTimeMillis() - gd.timePaused
      Gdx.app.debug("Total time paused", gd.timePaused.toString())
    }
    else if (gd.isGameOver && System.currentTimeMillis() - gd.gameOverTimer > 1500L) {
      val touchPos = Vector3(x.toFloat(), y.toFloat(), 0f)
      ps.cam.unproject(touchPos)
      if (touchPos.y > -25f) {
        ps.resetGame()
      } else {
        ps.game.setScreen(TitleScreen(ps.game, ps.assets))
      }
    }
    return true
  }

  /**
   * Shoot
   * Basically create a temporary box2d body at the touch coord.
   * Any chickens it collides with will die.
   * */
  fun shoot(x: Float, y: Float) {
    ps.mouseAttack = null // "reset" the body
    val mpos = Vector3(x, y, 0f)
    ps.cam.unproject(mpos)

    ps.bodyDef.type = BodyDef.BodyType.DynamicBody
    ps.bodyDef.position.set(mpos.x,mpos.y)
    ps.bodyDef.angle = Math.toRadians(90.0).toFloat()
    ps.mouseAttack = ps.world.createBody(ps.bodyDef)
    ps.mouseAttack?.userData = "shoot"

    val circle = CircleShape()
    circle.radius = 5f

    val fixtureDef = FixtureDef()
    fixtureDef.shape = circle
    fixtureDef.isSensor = true
    fixtureDef.filter.categoryBits = 2
    fixtureDef.filter.maskBits = 1.or(0x0010).or(0x0008).or(0x0020)

    ps.mouseAttack?.createFixture(fixtureDef)
    gd.canShootSound = true
    ps.assets.playShootSound()
  }
}