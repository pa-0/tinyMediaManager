/*
 * Copyright 2012 - 2025 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.core;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.xml.bind.DatatypeConverter;

public class AesUtil {
  public static final String  DEFAULT_VECTOR   = "727DEC2725991751BDFE3DBD0C6BF137";
  public static final String  DEFAULT_SALT     = "19CAD45282FBC7627B91A57F201B69E0359AFEB15DE328A88C0A2E05585F84C9";
  public static final AesUtil DEFAULT_INSTANCE = new AesUtil(128, 10);

  private final int           keySize;
  private final int           iterationCount;
  private final Cipher        cipher;

  public AesUtil(int keySize, int iterationCount) {
    this.keySize = keySize;
    this.iterationCount = iterationCount;
    try {
      cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // NOSONAR
    }
    catch (Exception e) {
      throw fail(e);
    }
  }

  public String encrypt(String salt, String iv, String passphrase, String plaintext) {
    SecretKey key = generateKey(salt, passphrase);
    byte[] encrypted = doFinal(Cipher.ENCRYPT_MODE, key, iv, plaintext.getBytes(StandardCharsets.UTF_8));
    return base64(encrypted);
  }

  public String decrypt(String salt, String iv, String passphrase, String ciphertext) {
    SecretKey key = generateKey(salt, passphrase);
    byte[] decrypted = doFinal(Cipher.DECRYPT_MODE, key, iv, base64(ciphertext));
    return new String(decrypted, StandardCharsets.UTF_8);
  }

  private byte[] doFinal(int encryptMode, SecretKey key, String iv, byte[] bytes) {
    try {
      cipher.init(encryptMode, key, new IvParameterSpec(hex(iv))); // NOSONAR
      return cipher.doFinal(bytes);
    }
    catch (Exception e) {
      throw fail(e);
    }
  }

  private SecretKey generateKey(String salt, String passphrase) {
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), hex(salt), iterationCount, keySize);
      return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
    catch (Exception e) {
      throw fail(e);
    }
  }

  protected static String random(int length) {
    byte[] salt = new byte[length];
    new SecureRandom().nextBytes(salt);
    return hex(salt);
  }

  protected static String base64(byte[] bytes) {
    return DatatypeConverter.printBase64Binary(bytes);
  }

  protected static byte[] base64(String str) {
    return DatatypeConverter.parseBase64Binary(str);
  }

  protected static String hex(byte[] bytes) {
    return DatatypeConverter.printHexBinary(bytes);
  }

  protected static byte[] hex(String str) {
    return DatatypeConverter.parseHexBinary(str);
  }

  private IllegalStateException fail(Exception e) {
    return new IllegalStateException(e);
  }
}
