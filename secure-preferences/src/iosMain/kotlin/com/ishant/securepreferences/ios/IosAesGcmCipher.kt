package com.ishant.securepreferences.ios

import com.ishant.securepreferences.internal.AeadCipher
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.CryptographyProviderApi
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.coroutines.sync.Mutex
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.cinterop.readBytes
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.NSData
import platform.Foundation.CFBridgingRetain
import platform.Foundation.CFBridgingRelease
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.errSecSuccess
import platform.Security.errSecItemNotFound
import platform.Security.errSecDuplicateItem
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * AES-256-GCM using cryptography-kotlin's optimal Apple CryptoKit provider.
 * The raw key is persisted in iOS Keychain and cached only for this instance.
 * Ciphertext format is IV || ciphertext || 128-bit authentication tag.
 */
@OptIn(ExperimentalForeignApi::class, CryptographyProviderApi::class)
internal class IosAesGcmCipher(
    service: String,
    account: String,
) : AeadCipher {
    private val keychain = KeychainAesKeyStore(service, account)
    private val mutex = Mutex()
    private val aes = CryptographyProvider.Default.get(AES.GCM)
    private var cachedKey: AES.GCM.Key? = null

    override suspend fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        getKey().cipher().encrypt(plaintext, associatedData)

    override suspend fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
        getKey().cipher().decrypt(ciphertext, associatedData)

    private suspend fun getKey(): AES.GCM.Key {
        mutex.lock()
        try {
            cachedKey?.let { return it }
            val rawKey = keychain.readOrCreate {
                val generated = aes.keyGenerator(AES.Key.Size.B256).generateKey()
                generated.encodeToByteArray(AES.Key.Format.RAW)
            }
            return try {
                aes.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, rawKey).also {
                    cachedKey = it
                }
            } finally {
                rawKey.fill(0)
            }
        } finally {
            mutex.unlock()
        }
    }
}

/** Stores only the 32-byte AES key in Keychain; never stores it in preferences. */
@OptIn(ExperimentalForeignApi::class)
private class KeychainAesKeyStore(
    private val service: String,
    private val account: String,
) {
    suspend fun readOrCreate(generate: suspend () -> ByteArray): ByteArray {
        read()?.let { return it }
        val generated = generate()
        try {
            require(generated.size == AES_256_KEY_BYTES) { "AES-256 key must be 32 bytes" }
            add(generated)
        } finally {
            generated.fill(0)
        }
        // A second process may win the insert race; always return the persisted key.
        return read() ?: throw IllegalStateException("Unable to persist or retrieve the iOS Keychain key")
    }

    private fun read(): ByteArray? = memScoped {
        val query = newQuery()
        CFDictionaryAddValue(query, kSecReturnData, platform.CoreFoundation.kCFBooleanTrue)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)
        if (status == errSecItemNotFound) return null
        check(status == errSecSuccess) {
            "Unable to read the AES key from iOS Keychain (OSStatus=$status)"
        }
        val data = CFBridgingRelease(result.value) as? NSData
            ?: throw IllegalStateException("iOS Keychain returned no key data")
        val size = data.length.toInt()
        val pointer = data.bytes?.reinterpret<kotlinx.cinterop.ByteVar>() ?: return null
        pointer.readBytes(size)
    }

    private fun add(key: ByteArray) {
        val data = key.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = key.size.toULong())
        }
        val query = newQuery()
        val retainedData = CFBridgingRetain(data)
        CFDictionaryAddValue(query, kSecValueData, retainedData)
        CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        val status = SecItemAdd(query, null)
        retainedData?.let { CFRelease(it) }
        CFRelease(query)
        check(status == errSecSuccess || status == errSecDuplicateItem) {
            "Unable to store the AES key in iOS Keychain (OSStatus=$status)"
        }
    }

    private fun newQuery(): CFMutableDictionaryRef {
        val dictionary = CFDictionaryCreateMutable(
            null,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        ) ?: error("Unable to create Keychain query")
        val serviceRef = CFBridgingRetain(service as platform.Foundation.NSString)
        val accountRef = CFBridgingRetain(account as platform.Foundation.NSString)
        CFDictionaryAddValue(dictionary, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(dictionary, kSecAttrService, serviceRef)
        CFDictionaryAddValue(dictionary, kSecAttrAccount, accountRef)
        serviceRef?.let { CFRelease(it) }
        accountRef?.let { CFRelease(it) }
        return dictionary
    }

    private companion object {
        const val AES_256_KEY_BYTES = 32
    }
}
