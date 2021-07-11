package com.fittonia.chigginblazzder

import com.badlogic.gdx.physics.box2d.*


class CollisionListener(val ps: PlayScreen) : ContactListener {

  val gd: GameDataMachine = ps.gameData // GameDataMachine

  override fun endContact(contact: Contact) {
    /**
     * if collision is GROUND vs BIG CHICKEN
     **/
    var ground: Body? = null
    var bigChicken: Body? = null
    if (contact.fixtureA.body.userData is BigChicken && contact.fixtureB.body.userData == "ground") {
      ground      = contact.fixtureB.body
      bigChicken  = contact.fixtureA.body
    }
    if (contact.fixtureB.body.userData is BigChicken && contact.fixtureA.body.userData == "ground") {
      ground      = contact.fixtureA.body
      bigChicken  = contact.fixtureB.body
    }

    if (ground != null && bigChicken != null) {
      val bc = bigChicken.userData as BigChicken
      /** Draw dust particle */
      val effect = ps.dustPool.obtain()
      effect.scaleEffect(bc.w*0.125f)
      effect.setPosition(bigChicken.position.x, ps.GROUND_HEIGHT+5-bc.artOffset)
      ps.dustEffects.add(effect)

      /** shake ground */
      gd.scrShakeTimer = System.currentTimeMillis()
      if (!gd.isGameOver) {
        ps.assets.playRandomGroundBoom()
      }
    }
  }

  override fun beginContact(contact: Contact) {
    handleShooting(contact)
    handleBigChickensKillingLittleChickens(contact)
  }

  override fun preSolve(contact: Contact?, oldManifold: Manifold?) { }

  override fun postSolve(contact: Contact?, impulse: ContactImpulse?) { }

  private fun assessChickenType(contact: Contact): Chicken {
    if (contact.fixtureA.body.userData is BigChicken) {
      return contact.fixtureA.body.userData as BigChicken
    } else if (contact.fixtureB.body.userData is BigChicken) {
      return contact.fixtureB.body.userData as BigChicken
    } else if (contact.fixtureA.body.userData is Chicken) {
      return contact.fixtureA.body.userData as Chicken
    } else {
      return contact.fixtureB.body.userData as Chicken
    }
  }

  private fun handleBigChickensKillingLittleChickens(contact: Contact) {
    /** Handle Big Chickens killing regular chickens*/
    var regularChicken: Chicken? = null
    var bigChicken: BigChicken? = null

    if (contact.fixtureA.body.userData is BigChicken
        && contact.fixtureB.body.userData is Chicken) {

      regularChicken = contact.fixtureB.body.userData as Chicken
      bigChicken = contact.fixtureA.body.userData as BigChicken

    } else if  (contact.fixtureA.body.userData is Chicken
        && contact.fixtureB.body.userData is BigChicken)  {

      regularChicken = contact.fixtureA.body.userData as Chicken
      bigChicken = contact.fixtureB.body.userData as BigChicken

    }

    // BIG CHICKENS COLLIDE AND KILL EACH OTHER. BUT NOT SURE IF IT'S BEST FOR GAME
//        if ((contact.fixtureA.body.userData is BigChicken
//                        && contact.fixtureB.body.userData is BigChicken) ||
//                (contact.fixtureB.body.userData is BigChicken
//                        && contact.fixtureA.body.userData is BigChicken) ) {
//            var bigchicken1 = contact.fixtureA.body.userData as BigChicken
//            var bigchicken2 = contact.fixtureB.body.userData as BigChicken
//            bigchicken1.die()
//            bigchicken2.die()
//            bigchicken1.isDying = true
//            bigchicken1.isDying = true
//            bigchicken2.isDying = true
//        } else

    if (regularChicken != null && bigChicken != null && !bigChicken.isDead) {
      regularChicken.die()
      regularChicken.body.linearVelocity.scl(-100f, 100f)

      val effect = ps.popPool.obtain()
      var scalefctr = regularChicken.w * 0.5f
      effect.scaleEffect(scalefctr)
      effect.setPosition(regularChicken.body.position.x, regularChicken.body.position.y)
      ps.popEffects.add(effect)
    }
  }

  /** This is where we handle the player's shots. In short, each time the player
   * shoots (taps/clicks), a box2d body is created at that location for a frame.
   * If it's colliding with a chicken, do the thing ... */
  private fun handleShooting(contact: Contact) {
    if (checkShotVsChicken(contact)) {
      /** In case we have a big chicken */
      val chicken: Chicken = assessChickenType(contact)
      if (!chicken.isDying) {
        val sensorFixture = when {
          contact.fixtureB.body.userData == "shoot" -> contact.fixtureB
          else -> contact.fixtureA
        }
        val sensor = sensorFixture.body


        /* Get angle between bodies colliding (chicken + playershot)
           and fire chicken in that direction! */
        val collisionAngle = sensor.position.sub(chicken.body.position).nor()

        if (chicken !is BigChicken) {
          if (collisionAngle.x > 0f) {
            chicken.body.angularVelocity = 12f
          } else {
            chicken.body.angularVelocity = -12f
          }
          chicken.body.applyLinearImpulse(
              -collisionAngle.x * 10f,
              10f,
              chicken.body.position.x,
              chicken.body.position.y,
              true)
        } else {
          chicken.body.applyLinearImpulse(
              -collisionAngle.x * 10f,
              chicken.w * 25f,
              chicken.body.position.x,
              chicken.body.position.y,
              true)
        }

        if (contact.isTouching) {
          gd.canShootSound = false
          chicken.hit()
        }
      }
    }
  }

  /** Checks if the collision is between player shot and a chicken  */
  fun checkShotVsChicken(contact: Contact): Boolean {
    return (contact.fixtureB.body.userData == "shoot" && contact.fixtureA.body.userData is Chicken) ||
        (contact.fixtureA.body.userData == "shoot" && contact.fixtureB.body.userData is Chicken)
  }
}