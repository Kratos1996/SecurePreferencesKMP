package com.ishant.securepreferences.ios

import com.ishant.securepreferences.internal.AeadCipher
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.coroutines.sync.Mutex
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecMissingEntitlement
import platform.Security.errSecNotAvailable
import platform.Security.errSecSuccess
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
@OptIn(ExperimentalForeignApi::class)
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
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
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
        if (isSimulatorOrHeadless && status != errSecSuccess) {
            return readSimulatorFallback()
        }
        check(status == errSecSuccess) {
            "Unable to read the AES key from iOS Keychain (OSStatus=$status)"
        }
        val data = (CFBridgingRelease(result.value) as? NSData)
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
        if (isSimulatorOrHeadless && status != errSecSuccess && status != errSecDuplicateItem) {
            writeSimulatorFallback(key)
            return
        }
        check(status == errSecSuccess || status == errSecDuplicateItem) {
            "Unable to store the AES key in iOS Keychain (OSStatus=$status)"
        }
    }

    private fun fallbackKey(): String = "com.ishant.securepreferences.sim_key.$service.$account"

    private fun readSimulatorFallback(): ByteArray? {
        inMemoryFallback[service to account]?.let { return it.copyOf() }
        val base64 = NSUserDefaults.standardUserDefaults.stringForKey(fallbackKey()) ?: return null
        val data = NSData.create(base64EncodedString = base64, options = 0u) ?: return null
        val size = data.length.toInt()
        val pointer = data.bytes?.reinterpret<kotlinx.cinterop.ByteVar>() ?: return null
        val bytes = pointer.readBytes(size)
        inMemoryFallback[service to account] = bytes.copyOf()
        return bytes
    }

    private fun writeSimulatorFallback(key: ByteArray) {
        inMemoryFallback[service to account] = key.copyOf()
        val data = key.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = key.size.toULong())
        }
        val base64 = data.base64EncodedStringWithOptions(0u)
        NSUserDefaults.standardUserDefaults.setObject(base64, forKey = fallbackKey())
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    private fun newQuery(): CFMutableDictionaryRef {
        val dictionary = CFDictionaryCreateMutable(
            null,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        ) ?: error("Unable to create Keychain query")
        val serviceRef = CFBridgingRetain(NSString.create(string = service))
        val accountRef = CFBridgingRetain(NSString.create(string = account))
        CFDictionaryAddValue(dictionary, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(dictionary, kSecAttrService, serviceRef)
        CFDictionaryAddValue(dictionary, kSecAttrAccount, accountRef)
        serviceRef?.let { CFRelease(it) }
        accountRef?.let { CFRelease(it) }
        return dictionary
    }

    private companion object {
        const val AES_256_KEY_BYTES = 32
        val inMemoryFallback = mutableMapOf<Pair<String, String>, ByteArray>()
        val isSimulatorOrHeadless: Boolean
            get() = platform.Foundation.NSBundle.mainBundle.bundleIdentifier == null ||
                    NSProcessInfo.processInfo.environment["SIMULATOR_ROOT"] != null ||
                    NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null ||
                    platform.posix.getenv("SIMULATOR_ROOT") != null
    }
}
