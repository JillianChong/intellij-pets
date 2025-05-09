package dev.gabrielchl.intellijPets.toolWindow

import dev.gabrielchl.intellijPets.settings.PetsSettings
import dev.gabrielchl.intellijPets.utils.Constants
import java.awt.Image
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.max

import java.io.File;


// TODO: rewrite all this to allow more pet-specific behaviours
class Pet(val variant: String, val container: JPanel) {
    private enum class State {
        ATTACK,
        IDLE,
        JUMP,
        LIEDOWN,
        RUN,
        SIT,
        WALK,
    }

    private var xIndex = 0
    private var state: State
    var currentX = Random().nextInt(300)
    private var targetX = currentX
    private val sprites: HashMap<State, ArrayList<BufferedImage>> = HashMap<State, ArrayList<BufferedImage>>()
    val SPRITE_WIDTH: Int
    val SPRITE_HEIGHT: Int
    private val SPEED: Int
    var image: Image

    init {
        state = State.SIT

        val settings = PetsSettings.instance.state

        val spriteSize: Int = Constants.PET_TO_SPRITE_SIZE[variant]!!
        val displayScale = 1.6
        val spriteWidthBase = Math.round(spriteSize * displayScale).toDouble()
        val spriteHeightBase = Math.round(spriteSize * displayScale).toDouble()
        val speedBase = 7.0 / 40 * spriteSize

        SPRITE_WIDTH = Math.round(spriteWidthBase * settings.petScale).toInt()
        SPRITE_HEIGHT = Math.round(spriteHeightBase * settings.petScale).toInt()
        SPEED = Math.round(speedBase * settings.petScale).toInt()

        for (state in State.values()) {
            val resourcePath = String.format(
                "/spritesheets/%s/%s.png", variant, state.toString().lowercase(
                    Locale.getDefault()
                )
            )

            val resourceUrl = javaClass.getResource(resourcePath)
            if (resourceUrl == null) {
                throw IllegalArgumentException("⚠️ Resource not found at: $resourcePath")
            }

            val spriteImg = ImageIO.read(resourceUrl)

            val spriteRow = ArrayList<BufferedImage>()
            var x = 0
            while (x < spriteImg.getWidth(null) / spriteSize) {
//                val croppedImg = spriteImg.getSubimage(spriteSize * x, 0, spriteSize, spriteSize)

//                spriteRow.add(croppedImg)
                spriteRow.add(spriteImg)
                x += 1
            }
            sprites[state] = spriteRow
        }
        image = sprites[state]!![xIndex]
    }

    fun onMouseClicked(@Suppress("UNUSED_PARAMETER") e: MouseEvent) {
        if (state == Pet.State.SIT) {
            xIndex = 0
            state = if (Math.round(Math.random().toFloat()) == 0) Pet.State.JUMP else Pet.State.ATTACK
        }
    }

    fun onMouseMoved(e: MouseEvent) {
        if (abs((e.x - (currentX + (SPRITE_WIDTH / 2))).toDouble()) > 30) {
            targetX = max(0.0, (e.x - (SPRITE_WIDTH / 2)).toDouble()).toInt()
        }
    }

    fun onMouseExited(@Suppress("UNUSED_PARAMETER") e: MouseEvent) {
        if (state == Pet.State.SIT) {
            targetX = currentX
        }
    }

    fun tick() {
        if (currentX != targetX && state != Pet.State.WALK) {
            state = Pet.State.WALK
            xIndex = 0
        }
        if (currentX == targetX && state == Pet.State.WALK) {
            state = Pet.State.SIT
            xIndex = 0
        }
        if (xIndex >= sprites[state]!!.size) {
            xIndex = 0
            if (state == Pet.State.JUMP || state == Pet.State.ATTACK) {
                state = Pet.State.SIT
            } else if (state == Pet.State.SIT) {
                val rand = Random().nextInt(10)
                if (rand == 0) {
                    state = Pet.State.IDLE
                }
                if (rand == 1) {
                    state = Pet.State.LIEDOWN
                }
                if (rand == 2) {
                    if (container.width > 0) {
                        targetX = Random().nextInt(container.width - SPRITE_WIDTH)
                    }
                }
            } else if (state == Pet.State.IDLE) {
                val rand = Random().nextInt(10)
                if (rand == 0) {
                    state = Pet.State.SIT
                }
                if (rand == 1) {
                    if (container.width > 0) {
                        targetX = Random().nextInt(container.width - SPRITE_WIDTH)
                    }
                }
            } else if (state == Pet.State.LIEDOWN) {
                if (Random().nextInt(15) == 0) {
                    state = Pet.State.SIT
                }
            }
        }
        var toRight = true
        if (currentX != targetX) {
            var change = abs((targetX - currentX).toDouble()).toInt()
            if (change > SPEED) {
                change = SPEED
            }
            if (targetX < currentX) {
                change *= -1
                toRight = false
            }
            currentX += change
        }
        val catImg = sprites[state]!![xIndex]
        if (!toRight) {
            val tx = AffineTransform.getScaleInstance(-1.0, 1.0)
            tx.translate(-catImg.getWidth(null).toDouble(), 0.0)
            val op = AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
            val flippedImage = op.filter(catImg, null)
            image = flippedImage.getScaledInstance(SPRITE_WIDTH, SPRITE_HEIGHT, Image.SCALE_DEFAULT)
        } else {
            image = catImg
        }
        xIndex += 1
    }
}