package com.fairfold.mfa;

import com.fairfold.common.audit.AuditService;
import com.fairfold.common.errors.ApiException;
import com.fairfold.config.AppProperties;
import com.fairfold.user.User;
import com.fairfold.user.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/** FR-005/FR-006: TOTP MFA with encrypted secrets and replay protection. */
@Service
public class MfaService {

    private static final int MAX_FAILED = 5;
    private static final Duration LOCKOUT = Duration.ofMinutes(15);
    private static final long STEP_SECONDS = 30;
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AppProperties properties;
    private final Clock clock;
    private final byte[] encryptionKey;

    public MfaService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      AuditService auditService, AppProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.properties = properties;
        this.clock = clock;
        String configured = properties.mfa() == null ? null : properties.mfa().encryptionKey();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("MFA_ENCRYPTION_KEY is required");
        }
        this.encryptionKey = Base64.getDecoder().decode(configured);
        if (this.encryptionKey.length != 32) {
            throw new IllegalStateException("MFA_ENCRYPTION_KEY must decode to 32 bytes");
        }
    }

    public record SetupResponse(String secret, String otpauthUri, String qrDataUri) {}

    @Transactional
    public SetupResponse setup(UUID userId) {
        User user = requireUser(userId);
        if (user.isMfaEnabled()) {
            throw ApiException.conflict("MFA_ALREADY_ENABLED", "Two-step login is already on.");
        }
        String secret = generateSecret();
        user.setMfaPendingSecretEnc(encrypt(secret));
        userRepository.save(user);
        String uri = "otpauth://totp/FairFold:" + urlEncode(user.getEmail())
                + "?secret=" + secret + "&issuer=FairFold";
        return new SetupResponse(secret, uri, qrDataUri(uri));
    }

    @Transactional
    public void enable(UUID userId, String code) {
        User user = requireUser(userId);
        if (user.isMfaEnabled()) {
            throw ApiException.conflict("MFA_ALREADY_ENABLED", "Two-step login is already on.");
        }
        if (user.getMfaPendingSecretEnc() == null) {
            throw ApiException.badRequest("MFA_SETUP_REQUIRED", "Start two-step setup first.");
        }
        String secret = decrypt(user.getMfaPendingSecretEnc());
        Long step = matchingStep(secret, code);
        if (step == null) {
            throw ApiException.badRequest("MFA_CODE_INVALID", "Enter a valid 6-digit code.");
        }
        user.setMfaSecretEnc(user.getMfaPendingSecretEnc());
        user.setMfaPendingSecretEnc(null);
        user.setMfaEnabled(true);
        user.setMfaLastStep(null);
        userRepository.save(user);
        auditService.record(userId, "mfa_enabled", "user", userId.toString(), "{}");
    }

    @Transactional
    public void disable(UUID userId, String password, String code) {
        User user = requireUser(userId);
        if (!user.isMfaEnabled()) {
            throw ApiException.conflict("MFA_NOT_ENABLED", "Two-step login is not on.");
        }
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.badRequest("INVALID_PASSWORD", "Password is incorrect.");
        }
        if (matchedStep(user.getMfaSecretEnc(), code, user.getMfaLastStep()) == null) {
            throw ApiException.badRequest("MFA_CODE_INVALID", "Enter a valid 6-digit code.");
        }
        clear(user);
        userRepository.save(user);
        auditService.record(userId, "mfa_disabled", "user", userId.toString(), "{}");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void verifyLogin(User user, String code) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(clock.instant())) {
            throw ApiException.locked(Math.max(1,
                    Duration.between(clock.instant(), user.getLockedUntil()).toMinutes() + 1));
        }
        Long matched = matchedStep(user.getMfaSecretEnc(), code, user.getMfaLastStep());
        if (matched == null) {
            int count = user.getFailedLoginCount() + 1;
            int triesLeft;
            if (count >= MAX_FAILED) {
                user.setFailedLoginCount(0);
                user.setLockedUntil(clock.instant().plus(LOCKOUT));
                triesLeft = 0;
            } else {
                user.setFailedLoginCount(count);
                triesLeft = MAX_FAILED - count;
            }
            userRepository.save(user);
            throw ApiException.unauthorized("MFA_CODE_INVALID", "Invalid two-step code.",
                    Map.of("triesLeft", triesLeft));
        }
        user.setMfaLastStep(matched);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void adminReset(UUID actorId, UUID targetId) {
        User user = requireUser(targetId);
        clear(user);
        userRepository.save(user);
        auditService.record(actorId, "mfa_reset", "user", targetId.toString(), "{}");
    }

    private Long matchedStep(String encryptedSecret, String code, Long lastStep) {
        if (encryptedSecret == null || code == null || !code.trim().matches("\\d{6}")) {
            return null;
        }
        String secret = decrypt(encryptedSecret);
        long nowStep = currentStep();
        for (int offset = -1; offset <= 1; offset++) {
            long step = nowStep + offset;
            if (lastStep != null && step <= lastStep) {
                continue;
            }
            if (MessageDigest.isEqual(totp(secret, step).getBytes(StandardCharsets.UTF_8),
                    code.trim().getBytes(StandardCharsets.UTF_8))) {
                return step;
            }
        }
        return null;
    }

    private Long matchingStep(String secret, String code) {
        if (code == null || !code.trim().matches("\\d{6}")) {
            return null;
        }
        long nowStep = currentStep();
        for (int offset = -1; offset <= 1; offset++) {
            long step = nowStep + offset;
            if (MessageDigest.isEqual(totp(secret, step).getBytes(StandardCharsets.UTF_8),
                    code.trim().getBytes(StandardCharsets.UTF_8))) {
                return step;
            }
        }
        return null;
    }

    long currentStep() {
        return clock.instant().getEpochSecond() / STEP_SECONDS;
    }

    String totp(String secret, long step) {
        try {
            byte[] key = base32Decode(secret);
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (step & 0xff);
                step >>>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception e) {
            throw new IllegalStateException("Could not calculate TOTP", e);
        }
    }

    private void clear(User user) {
        user.setMfaEnabled(false);
        user.setMfaSecretEnc(null);
        user.setMfaPendingSecretEnc(null);
        user.setMfaLastStep(null);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED", "Authentication required."));
    }

    private String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    static String base32Encode(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32.charAt((buffer >> (bits - 5)) & 0x1f));
                bits -= 5;
            }
        }
        if (bits > 0) {
            out.append(BASE32.charAt((buffer << (5 - bits)) & 0x1f));
        }
        return out.toString();
    }

    static byte[] base32Decode(String value) {
        String clean = value == null ? "" : value.replace("=", "").replace(" ", "").toUpperCase();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : clean.toCharArray()) {
            int index = BASE32.indexOf(c);
            if (index < 0) {
                throw new IllegalArgumentException("Invalid base32 secret");
            }
            buffer = (buffer << 5) | index;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }

    private String encrypt(String plain) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"),
                    new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt MFA secret", e);
        }
    }

    private String decrypt(String stored) {
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            byte[] iv = new byte[12];
            System.arraycopy(combined, 0, iv, 0, 12);
            byte[] encrypted = new byte[combined.length - 12];
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"),
                    new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt MFA secret", e);
        }
    }

    private String qrDataUri(String uri) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, 240, 240);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate MFA QR code", e);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
