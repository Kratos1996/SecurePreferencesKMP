package com.ishant.securepreferences.desktop

import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopEncryptedSnapshotStorageTest {

    @Test
    fun writesReadsReplacesAndDeletesSnapshot() = runTest {
        withTemporaryDirectory { directory ->
            val file = directory.resolve("preferences.snapshot")
            val storage = DesktopEncryptedSnapshotStorage(file)

            assertEquals(null, storage.read())

            storage.write("encrypted-envelope-v1")
            assertEquals("encrypted-envelope-v1", storage.read())

            storage.write("encrypted-envelope-v2")
            assertEquals("encrypted-envelope-v2", storage.read())

            storage.delete()
            assertEquals(null, storage.read())

            // Deleting an already absent snapshot is intentionally idempotent.
            storage.delete()
            assertFalse(Files.exists(file))
        }
    }

    @Test
    fun appliesOwnerOnlyPosixPermissionsWhenSupported() = runTest {
        withTemporaryDirectory { directory ->
            val file = directory.resolve("preferences.snapshot")
            val storage = DesktopEncryptedSnapshotStorage(file)

            storage.write("ciphertext")

            if (Files.getFileAttributeView(file, PosixFileAttributeView::class.java) != null) {
                assertEquals(
                    setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                    Files.getPosixFilePermissions(file),
                )
                assertEquals(
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    ),
                    Files.getPosixFilePermissions(directory),
                )
            }
        }
    }

    private suspend fun withTemporaryDirectory(block: suspend (java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("secure-preferences-test-")
        try {
            block(directory)
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }
}
