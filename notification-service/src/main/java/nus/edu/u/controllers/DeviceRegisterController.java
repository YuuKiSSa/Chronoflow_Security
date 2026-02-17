package nus.edu.u.controllers;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import nus.edu.u.domain.dto.common.DeviceRegisterDTO;
import nus.edu.u.enums.push.PushPlatform;
import nus.edu.u.services.push.DeviceRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications/push/devices")
public class DeviceRegisterController {

    private final DeviceRegistryService deviceRegistryService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerDevice(@RequestBody DeviceRegisterDTO dto) {
        // Ensures request is authenticated; throws if not logged in
        StpUtil.checkLogin();

        String userId = String.valueOf(StpUtil.getLoginIdAsLong());

        if (dto.getPlatform() == null) dto.setPlatform(PushPlatform.WEB);
        deviceRegistryService.register(userId, dto);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeByToken(@RequestParam("token") String token) {
        String userId = String.valueOf(StpUtil.getLoginIdAsLong());
        deviceRegistryService.revokeByTokenForUser(userId, token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke-all")
    public ResponseEntity<Void> revokeAll() {
        StpUtil.checkLogin();

        String userId = String.valueOf(StpUtil.getLoginIdAsLong());
        deviceRegistryService.revokeAllForUser(userId);

        return ResponseEntity.ok().build();
    }
}
