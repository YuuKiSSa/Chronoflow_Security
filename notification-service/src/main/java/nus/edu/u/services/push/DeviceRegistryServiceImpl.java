package nus.edu.u.services.push;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import nus.edu.u.domain.dataObject.common.NotificationDeviceDO;
import nus.edu.u.domain.dto.common.DeviceRegisterDTO;
import nus.edu.u.domain.dto.common.NotificationDeviceViewDTO;
import nus.edu.u.enums.common.DeviceStatus;
import nus.edu.u.enums.push.PushPlatform;
import nus.edu.u.repositories.common.NotificationDeviceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceRegistryServiceImpl implements DeviceRegistryService {

    private static final String CACHE_NAME = "devices:activeByUser";

    private final NotificationDeviceRepository repo;

    @Override
    @Transactional
    public void register(String userId, DeviceRegisterDTO dto) {
        if (userId == null || userId.isBlank())
            throw new IllegalArgumentException("userId is required");
        if (dto == null || dto.getToken() == null || dto.getToken().isBlank())
            throw new IllegalArgumentException("token is required");

        final String token = dto.getToken().trim();
        final PushPlatform platform =
                Optional.ofNullable(dto.getPlatform()).orElse(PushPlatform.WEB);

        var existing = repo.findByToken(token).orElse(null);
        if (existing == null) {
            try {
                repo.save(
                        NotificationDeviceDO.builder()
                                .userId(userId)
                                .platform(platform)
                                .token(token)
                                .status(DeviceStatus.ACTIVE)
                                .build());
                return;
            } catch (DataIntegrityViolationException race) {
                existing = repo.findByToken(token).orElse(null);
            }
        }

        if (existing != null) {
            if (existing.getUserId().equals(userId)
                    && existing.getPlatform() == platform
                    && existing.getStatus() == DeviceStatus.ACTIVE) {
                return; // no change
            }
            existing.setUserId(userId);
            existing.setPlatform(platform);
            existing.setStatus(DeviceStatus.ACTIVE);
            repo.save(existing);
        }
    }

    @Override
    @Transactional
    public void revokeByToken(String token) {
        if (token == null || token.isBlank()) return;
        repo.findByToken(token.trim())
                .ifPresent(
                        d -> {
                            d.setStatus(DeviceStatus.REVOKED);
                            repo.save(d);
                        });
    }

    @Override
    @Transactional
    public void revokeByTokenForUser(String userId, String token) {
        if (userId == null || userId.isBlank())
            throw new IllegalArgumentException("userId is required");
        if (token == null || token.isBlank()) return;

        repo.findByUserIdAndToken(userId, token.trim())
                .ifPresent(d -> {
                    d.setStatus(DeviceStatus.REVOKED);
                    repo.save(d);
                });

        // Intentionally do nothing if not found:
        // - idempotent
        // - avoids leaking whether a token exists / belongs to someone else
    }

    /** Keep for internal use (no caching) */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDeviceDO> activeDevices(String userId) {
        if (userId == null || userId.isBlank())
            throw new IllegalArgumentException("userId is required");
        return repo.findByUserIdAndStatus(userId, DeviceStatus.ACTIVE);
    }

    /** Cache the thin DTOs */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDeviceViewDTO> activeDeviceViews(String userId) {
        if (userId == null || userId.isBlank())
            throw new IllegalArgumentException("userId is required");
        return repo.findByUserIdAndStatus(userId, DeviceStatus.ACTIVE).stream()
                .map(
                        d ->
                                NotificationDeviceViewDTO.builder()
                                        .id(d.getId())
                                        .token(d.getToken())
                                        .platform(d.getPlatform())
                                        .status(d.getStatus())
                                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void revokeAllForUser(String userId) {
        var active = repo.findByUserIdAndStatus(userId, DeviceStatus.ACTIVE);
        for (var d : active) {
            d.setStatus(DeviceStatus.REVOKED);
        }
        repo.saveAll(active);
    }
}
