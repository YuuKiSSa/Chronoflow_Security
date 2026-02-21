package nus.edu.u.controllers;

import static nus.edu.u.common.core.domain.CommonResult.success;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.domain.dto.common.NotificationDetailRespDTO;
import nus.edu.u.services.common.NotificationQueryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationQueryController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping("/{notifId}")
    public CommonResult<NotificationDetailRespDTO> getNotificationDetail(
            @PathVariable("notifId") String notifId) {
        StpUtil.checkLogin();
        String currentUserId = String.valueOf(StpUtil.getLoginIdAsLong());

        return success(notificationQueryService.getDetail(notifId, currentUserId));
    }
}
