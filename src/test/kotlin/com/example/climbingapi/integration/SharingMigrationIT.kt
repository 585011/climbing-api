package com.example.climbingapi.integration

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.sql.DataSource

/** V11 must leave Sharing off for users who existed before it, and on for users created after (ADR-0002). */
class SharingMigrationIT : IntegrationTestBase() {

    @Autowired lateinit var dataSource: DataSource

    private val schema = "sharing_migration_test"

    private fun migrateTo(version: String) =
        Flyway.configure().dataSource(dataSource).schemas(schema).target(version).load().migrate()

    @AfterEach
    fun dropSchema() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS $schema CASCADE")
    }

    @Test
    fun `existing users get Sharing off and new users get it on`() {
        migrateTo("10")
        jdbcTemplate.update("INSERT INTO $schema.users (email, display_name) VALUES ('old@example.com', 'Old')")

        migrateTo("11")
        jdbcTemplate.update("INSERT INTO $schema.users (email, display_name) VALUES ('new@example.com', 'New')")

        val sharing = jdbcTemplate.query("SELECT email, share_activity FROM $schema.users") { rs, _ ->
            rs.getString("email") to rs.getBoolean("share_activity")
        }.toMap()
        assertEquals(false, sharing["old@example.com"])
        assertEquals(true, sharing["new@example.com"])
        // Seed users from V2/V3 also existed before V11.
        assertEquals(setOf(false), sharing.filterKeys { it != "new@example.com" }.values.toSet())
    }
}
