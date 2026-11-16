package com.ishant.securepreferences.desktop

import com.ishant.securepreferences.internal.EncryptedSnapshotStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission

/**
 * Persists only the encrypted envelope. Writes use a sibling temporary file
 * and atomic replacement where supported. POSIX systems receive owner-only
 * directory and file permissions.
 */
internal class DesktopEncryptedSnapshotStorage(
    private val snapshotFile: Path,
) : EncryptedSnapshotStorage {
    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        if (!Files.exists(snapshotFile)) return@withContext null
        Files.readString(snapshotFile)
    }

    override suspend fun write(value: String) = withContext(Dispatchers.IO) {
        val directory = snapshotFile.parent
        Files.createDirectories(directory)
        restrictPermissions(directory, DIRECTORY_PERMISSIONS)

        val temporary = Files.createTempFile(directory, ".secure-preferences-", ".tmp")
        try {
            restrictPermissions(temporary, FILE_PERMISSIONS)
            Files.writeString(
                temporary,
                value,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
            )
            try {
                Files.move(
                    temporary,
                    snapshotFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary, snapshotFile, StandardCopyOption.REPLACE_EXISTING)
            }
            restrictPermissions(snapshotFile, FILE_PERMISSIONS)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    override suspend fun delete() = withContext(Dispatchers.IO) {
        Files.deleteIfExists(snapshotFile)
    }

    private fun restrictPermissions(path: Path, permissions: Set<PosixFilePermission>) {
        try {
            Files.setPosixFilePermissions(path, permissions)
        } catch (_: UnsupportedOperationException) {
            // Windows uses the user's profile ACL. Never weaken it with a fallback file.
        }
    }

    private companion object {
        val DIRECTORY_PERMISSIONS = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
        )
        val FILE_PERMISSIONS = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
        )
    }
}
