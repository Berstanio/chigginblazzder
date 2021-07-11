package com.fittonia.chigginblazzder

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.BitmapFontLoader
import com.badlogic.gdx.assets.loaders.ParticleEffectLoader
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.ParticleEffect
import com.badlogic.gdx.utils.Disposable
import org.jetbrains.annotations.Nullable
import java.util.concurrent.ThreadLocalRandom

class Assets : Disposable {

  val manager = AssetManager()
  var isMusicOn = true
  val masterVol = 2f/5f

  fun loadAssets() {
    loadTextures()
    loadFonts()
    loadParticleEffects()
    loadSounds()
    loadMusic()
  }

  fun loadTextures() {
    // TITLE SCREEN
    manager.load("title_sprite.png", Texture::class.java)
    manager.load("text_loading.png", Texture::class.java)
    manager.load("text_taptostart.png", Texture::class.java)
    manager.load("text_highscore.png", Texture::class.java)
    manager.load("text_music.png", Texture::class.java)
    manager.load("text_nomusic.png", Texture::class.java)
    manager.load("text_version.png", Texture::class.java)


    // MAIN GAME
    manager.load("big_chicken_32_L.png", Texture::class.java)
    manager.load("big_chicken_64_L.png", Texture::class.java)
    manager.load("big_chicken_32_R.png", Texture::class.java)
    manager.load("big_chicken_64_R.png", Texture::class.java)
    manager.load("chicken_tiny_R.png", Texture::class.java)
    manager.load("chicken_tiny_L.png", Texture::class.java)
    manager.load("chicken_body.png", Texture::class.java)
    manager.load("redChicken_frames.png", Texture::class.java)
    manager.load("chicken_frames_L.png", Texture::class.java)
    manager.load("chicken_frames_R.png", Texture::class.java)
    manager.load("text_newhighscore.png", Texture::class.java)
    manager.load("bg_ground.png", Texture::class.java)
    manager.load("bg_clouds_BG.png", Texture::class.java)
    manager.load("bg_clouds_FG.png", Texture::class.java)
    manager.load("bg_background.png", Texture::class.java)
    manager.load("ui_crosshair.png", Texture::class.java)
  }

  fun getBigChickenTexture(size: Int, dir:String): Texture {
    return manager.get("big_chicken_${size}_${dir}.png", Texture::class.java)
  }


  fun loadFonts() {
    manager.load("font_rounded.fnt", BitmapFont::class.java)
  }

  fun getFont(): BitmapFont {
    return manager.get("font_rounded.fnt", BitmapFont::class.java)
  }

  fun loadParticleEffects() {
    manager.load("particleEffects/pop.p", ParticleEffect::class.java)
    manager.load("particleEffects/dustcloud.particle", ParticleEffect::class.java)
  }

  fun loadSounds() {
    manager.load("sounds/hit0.wav", Sound::class.java)
    manager.load("sounds/Laser_Shoot4.wav", Sound::class.java)
    manager.load("sounds/hit_redChicken0.wav", Sound::class.java)
    manager.load("sounds/multiplier_increase.wav", Sound::class.java)

    for (i in 0..12) {
      manager.load("sounds/Chirp${i}.wav", Sound::class.java)
    }
    for (i in 0..2) {
      manager.load("sounds/Explosion${i}.wav", Sound::class.java)
    }
    for (i in 0..7) {
      manager.load("sounds/hit${i}.wav", Sound::class.java)
    }
    for (i in 0..6) {
      manager.load("sounds/big_chicken_hit${i}.wav", Sound::class.java)
    }

    for (i in 0..2) {
      manager.load("sounds/ground_boom${i}.wav", Sound::class.java)
    }
  }


  fun playShootSound() {
    manager.get("sounds/Laser_Shoot4.wav", Sound::class.java).play(masterVol)
  }

  fun playRandomGroundBoom() {
    val num = ThreadLocalRandom.current().nextInt(0,3)
    manager.get("sounds/ground_boom${num}.wav", Sound::class.java).play(masterVol)
  }

  fun playRandomBigChickenHitSound() {
    val num = ThreadLocalRandom.current().nextInt(0,7)
    manager.get("sounds/big_chicken_hit${num}.wav", Sound::class.java).play(masterVol)
  }

  fun playHitRedChickenSound() {
    manager.get("sounds/hit_redChicken0.wav", Sound::class.java).play(masterVol)
  }

  fun playMultiplierIncreaseSound() {
    manager.get("sounds/multiplier_increase.wav", Sound::class.java).play(masterVol*1.5f)
  }

  fun playRandomChirp() {
    val num = ThreadLocalRandom.current().nextInt(0,13)
    manager.get("sounds/Chirp${num}.wav", Sound::class.java).play(masterVol)
  }

  fun playRandomExplosion() {
    val num = ThreadLocalRandom.current().nextInt(0,3)
    manager.get("sounds/Explosion${num}.wav", Sound::class.java).play(masterVol)
  }

  fun loadMusic() {
    manager.load("music/music_title.wav", Music::class.java)
    manager.load("music/music_gameplay.mp3", Music::class.java)
    manager.load("music/music_gameover.wav", Music::class.java)
    manager.load("music/music_pause.mp3", Music::class.java)
  }

  fun getTitleMusic(): Music {
    return manager.get("music/music_title.wav", Music::class.java)
  }

  fun getGamePlayMusic(): Music {
    return manager.get("music/music_gameplay.mp3", Music::class.java)
  }

  fun getGamePauseMusic(): Music {
    return manager.get("music/music_pause.mp3", Music::class.java)
  }

  fun getGameOverMusic(): Music {
    return manager.get("music/music_gameover.wav", Music::class.java)
  }
  override fun dispose() {
    manager.clear()
  }
}