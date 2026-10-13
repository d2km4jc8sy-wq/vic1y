package com.example.s1client.util

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 敏感配置的本地加密存储（基于 Android Keystore 的 AES-GCM）。
 *
 * 用途：把登录 Cookie 这类"能直接顶替账号"的凭据从明文 SharedPreferences 挪走。
 *
 * 设计取舍：
 * - **不引入 androidx.security**（夜间新增依赖有拉取失败风险），直接用平台 Keystore API，
 *   离线可编译、无额外依赖。
 * - 密钥存在系统 Keystore（TEE/硬件-backed），**应用私有目录里拿不到密钥本身**，
 *   拿到 root 之外的数据也解不开。
 * - GCM 带认证标签，能发现密文被篡改。
 * - 任何一步失败（锁屏导致密钥不可用、迁移时数据损坏）都**回退到明文读写**，
 *   宁可安全性差一点也不能让用户莫名其妙掉登录。
 */
object SecureStore {

    private const val TAG = "SecureStore"
    private const val KEY_ALIAS = "s1_secure_store_v1"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_LEN = 12
    private const val TAG_LEN_BITS = 128

    /** Keystore 在 API 23 以下不可用；本项目 minSdk 24，保留判断以便万一下调 */
    private val supported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return gen.generateKey()
    }

    /** 加密并以 Base64 存回 SharedPreferences 的 [key]；失败返回 false（调用方应回退明文） */
    fun putEncrypted(prefs: android.content.SharedPreferences, key: String, plain: String): Boolean {
        if (!supported) return false
        return try {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val iv = cipher.iv
            val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val packed = iv.size + ct.size
            val buf = ByteArray(packed)
            System.arraycopy(iv, 0, buf, 0, iv.size)
            System.arraycopy(ct, 0, buf, iv.size, ct.size)
            prefs.edit()
                .putString(key, Base64.encodeToString(buf, Base64.NO_WRAP))
                .putBoolean("$key.enc", true)
                .apply()
            true
        } catch (e: Exception) {
            Log.w(TAG, "putEncrypted failed, fallback to plain", e)
            prefs.edit().putBoolean("$key.enc", false).apply()
            false
        }
    }

    /** 读取；优先尝试解密，标记/格式不符时自动降级读明文（兼容迁移前的老数据） */
    fun getString(prefs: android.content.SharedPreferences, key: String): String? {
        val raw = prefs.getString(key, null) ?: return null
        val isEnc = prefs.getBoolean("$key.enc", false)
        if (!isEnc) return raw
        if (!supported) return null
        return try {
            val buf = Base64.decode(raw, Base64.NO_WRAP)
            if (buf.size <= IV_LEN) return null
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(
                Cipher.DECRYPT_MODE, secretKey(),
                GCMParameterSpec(TAG_LEN_BITS, buf, 0, IV_LEN)
            )
            String(cipher.doFinal(buf, IV_LEN, buf.size - IV_LEN), Charsets.UTF_8)
        } catch (e: Exception) {
            // 密钥不可用（常见于用户改锁屏密码后 Keystore 失效）或数据损坏：
            // 删除损坏项并当未登录处理，下一次登录会重新写入
            Log.w(TAG, "getString decrypt failed, dropping entry", e)
            prefs.edit().remove(key).remove("$key.enc").apply()
            null
        }
    }
}
