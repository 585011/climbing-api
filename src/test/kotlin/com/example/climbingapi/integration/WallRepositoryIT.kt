package com.example.climbingapi.integration

import com.example.climbingapi.model.Wall
import com.example.climbingapi.repository.WallRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(WallControllerIT.FakeStorageConfig::class)
class WallRepositoryIT : IntegrationTestBase() {

    @Autowired lateinit var wallRepository: WallRepository

    private var areaId = 0

    @BeforeEach
    fun createArea() {
        // resetDatabase() in IntegrationTestBase truncates everything first (runs before this).
        areaId = extractId(postJson("/api/climbing-areas", """{"name":"Test Area"}"""))
    }

    @Test
    fun `create and read round-trips all three image keys`() {
        val created = wallRepository.create(
            Wall(
                id = null, areaId = areaId, name = "Variant Wall", description = null,
                latitude = null, longitude = null, approachInfo = null,
                imageKey = "walls/orig.png", createdAt = null,
                optimizedKey = "walls/opt.jpg", thumbnailKey = "walls/thumb.jpg"
            )
        )
        val fetched = wallRepository.getById(created.id!!)
        assertNotNull(fetched)
        assertEquals("walls/orig.png", fetched!!.imageKey)
        assertEquals("walls/opt.jpg", fetched.optimizedKey)
        assertEquals("walls/thumb.jpg", fetched.thumbnailKey)
    }

    @Test
    fun `updateImageKeys replaces all three keys`() {
        val created = wallRepository.create(
            Wall(
                id = null, areaId = areaId, name = "Update Wall", description = null,
                latitude = null, longitude = null, approachInfo = null,
                imageKey = "walls/old.png", createdAt = null
            )
        )
        val updated = wallRepository.updateImageKeys(created.id!!, "walls/o2.png", "walls/opt2.jpg", "walls/th2.jpg")
        assertNotNull(updated)
        assertEquals("walls/o2.png", updated!!.imageKey)
        assertEquals("walls/opt2.jpg", updated.optimizedKey)
        assertEquals("walls/th2.jpg", updated.thumbnailKey)
    }

    @Test
    fun `findWallsNeedingBackfill returns walls with an original but missing variants`() {
        val needs = wallRepository.create(
            Wall(
                id = null, areaId = areaId, name = "Needs Backfill", description = null,
                latitude = null, longitude = null, approachInfo = null,
                imageKey = "walls/needs.png", createdAt = null
            )
        )
        val done = wallRepository.create(
            Wall(
                id = null, areaId = areaId, name = "Already Done", description = null,
                latitude = null, longitude = null, approachInfo = null,
                imageKey = "walls/done.png", createdAt = null,
                optimizedKey = "walls/done-opt.jpg", thumbnailKey = "walls/done-th.jpg"
            )
        )
        val result = wallRepository.findWallsNeedingBackfill()
        assertTrue(result.any { it.id == needs.id })
        assertTrue(result.none { it.id == done.id })
    }
}
