package com.tetraploid.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComposeKnMavenCacheTest {
    @Test
    fun findsAssetIdRegardlessOfFieldOrder() {
        val json = """
            {
              "id": 1,
              "name": "v0.5.67",
              "assets": [
                {
                  "url": "https://api.github.com/...",
                  "name": "other.zip",
                  "id": 11
                },
                {
                  "id": 42,
                  "node_id": "x",
                  "name": "composekn-maven-0.5.67.zip",
                  "content_type": "application/zip"
                }
              ]
            }
        """.trimIndent()
        assertEquals(
            "42",
            ComposeKnMavenCache.findReleaseAssetId(json, "composekn-maven-0.5.67.zip"),
        )
    }

    @Test
    fun ignoresReleaseNameOutsideAssets() {
        val json = """
            {
              "id": 99,
              "name": "composekn-maven-0.5.67.zip",
              "assets": []
            }
        """.trimIndent()
        assertNull(
            ComposeKnMavenCache.findReleaseAssetId(json, "composekn-maven-0.5.67.zip"),
        )
    }
}
