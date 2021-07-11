package com.fittonia.chigginblazzder

import com.badlogic.gdx.graphics.Color

/**
 * This class holds all the little timers and counters and
 * booleans that handle the "logic" of the game */

class GameDataMachine(var showCursor: Boolean) {

  val INIT_CHICKEN_SPAWN_RATE = 0.98f
  var chickenSpawnRate = INIT_CHICKEN_SPAWN_RATE
  var multiplier = 1
  var currentScore = 0

  /** Handle screenshake */
  var scrShakeTimer = 0L

  /** adds little delay after game over */
  var gameOverTimer = 0L

  var spawnTimer = 0L
  /** spawntimer for chickens*/
  var timeToSpawn = 600L  // RESET THIS EVERY GAME
  val initBigChickenLimit = 3
  var bigChickenLimit = initBigChickenLimit // RESET THIS EVERY GAME
  var numBigChickens = 0         // RESET THIS EVERY GAME

  var isGameOver = false     // RESET THIS EVERY GAME
  var didChickenDie = false
  var spawnBigChicken = false     // RESET THIS EVERY GAME
  var canShootSound = false
  var didHitRedChicken = false
  var hitRedChickenTimer = 0L
  var timePaused = 0L
  var gameOverMusicTimesPlayed = 0

  // Color of text '!' when you hit a red chicken.
  var redChickenAlertColor: Color = Color.valueOf("#BE2633")
  var spawnBigChickenAtScore = 15
  var numOfChigginsBlazzted = 0

  val tryAgainText = "Tap to try again!"
  val returnToTitleText = "or return to title screen"
  val pausedText = "Tap to unpause"

  var canSpawnRedChicken = generateNextRedChickenTime()
  var spawnRedChickenTimer = System.currentTimeMillis()

  fun generateNextRedChickenTime(): Long {
    val num = 8_000L + (Math.random() * 4000.0).toLong()
//    println("Next red chicken: $num")
    return num
  }

  fun resetGameData() {
    isGameOver = false
    gameOverTimer = 0L
    currentScore = 0
    numBigChickens = 0
    timeToSpawn = 600L
    multiplier = 1
    numOfChigginsBlazzted = 0
    chickenSpawnRate = INIT_CHICKEN_SPAWN_RATE
    bigChickenLimit = initBigChickenLimit
    spawnBigChickenAtScore = 15
    spawnRedChickenTimer = System.currentTimeMillis()
    canSpawnRedChicken = generateNextRedChickenTime()
    gameOverMusicTimesPlayed = 0
  }

  fun penalizePlayerForHittingChicken() {
    currentScore /= 2
    multiplier -= when {
      multiplier > 10 -> 3
      multiplier > 5 -> 2
      else -> 1
    }
    if (multiplier < 1) multiplier = 1
  }

  fun chickenShot(chicken: Chicken) {
    if (chicken is RedChicken) { // RedChickens Multiply score
      penalizePlayerForHittingChicken()
    } else {
      numOfChigginsBlazzted++
      if (chicken is BigChicken) {
        currentScore += (5 * multiplier)
      } else {
        currentScore += (1 * multiplier)
      }

      if (numOfChigginsBlazzted > 249) {
        if (numOfChigginsBlazzted % 25 == 0) {
          spawnBigChicken = true
          timeToSpawn -= 26L
          if (timeToSpawn < 130L) {
            timeToSpawn = 130L
          }
        }
      } else if (numOfChigginsBlazzted > 199) {
        if (numOfChigginsBlazzted % 25 == 0) {
          spawnBigChicken = true
          timeToSpawn -= 28L
          if (timeToSpawn < 135L) {
            timeToSpawn = 135L
          }
        }
      } else if (numOfChigginsBlazzted > 99) {
        if (numOfChigginsBlazzted % 20 == 0) {
          spawnBigChicken = true
          timeToSpawn -= 30L
          if (timeToSpawn < 140L) {
            timeToSpawn = 140L
          }
        }
      } else {
        if (numOfChigginsBlazzted % 15 == 0) {
          spawnBigChicken = true
          timeToSpawn -= 35L
          if (timeToSpawn < 155L) {
            timeToSpawn = 155L
          }
        }
      }


      if (numOfChigginsBlazzted > 0 && numOfChigginsBlazzted % 100 == 0) {
        bigChickenLimit++
        spawnBigChicken = true
      }
    }
  }

}