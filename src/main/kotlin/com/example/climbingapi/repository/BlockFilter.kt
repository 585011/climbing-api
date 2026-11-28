package com.example.climbingapi.repository

/**
 * SQL condition that hides users who have blocked the viewer (ADR-0003), so a blocked climber never sees the blocker.
 * [userIdColumn] is the column holding the listed user's id. Takes the viewer's user id once; null hides no one.
 */
internal fun notBlockingViewer(userIdColumn: String): String = """
    NOT EXISTS (
        SELECT 1 FROM follows blk
        WHERE blk.follower_id = ?::int AND blk.followee_id = $userIdColumn AND blk.status = 'blocked'
    )
""".trimIndent()
