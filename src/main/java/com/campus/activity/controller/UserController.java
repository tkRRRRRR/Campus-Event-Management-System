package com.campus.activity.controller;

import com.campus.activity.annotation.RequireRole;
import com.campus.activity.common.Constants;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.Result;
import com.campus.activity.dto.PasswordDTO;
import com.campus.activity.dto.ProfileDTO;
import com.campus.activity.dto.UserQuery;
import com.campus.activity.service.UserService;
import com.campus.activity.util.UserContext;
import com.campus.activity.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 当前登录用户信息 */
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.ok(userService.getInfo(UserContext.getUserId()));
    }

    /** 修改个人资料 */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileDTO dto) {
        userService.updateProfile(UserContext.getUserId(), dto);
        return Result.ok("资料已保存", null);
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordDTO dto) {
        userService.changePassword(UserContext.getUserId(), dto);
        return Result.ok("密码修改成功，请重新登录", null);
    }

    /** 上传头像 */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Result.ok(userService.uploadAvatar(UserContext.getUserId(), file));
    }

    /* ---------------- 管理员 ---------------- */

    /** 用户列表 */
    @GetMapping("/page")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<PageResult<UserVO>> page(UserQuery query) {
        return Result.ok(userService.adminPage(query));
    }

    /** 启用/禁用账号 */
    @PutMapping("/{id}/status")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> updateStatus(@PathVariable Integer id, @RequestParam Integer status) {
        userService.updateStatus(id, status, UserContext.getUserId());
        return Result.ok("操作成功", null);
    }

    /** 修改角色 */
    @PutMapping("/{id}/role")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> updateRole(@PathVariable Integer id, @RequestParam String role) {
        userService.updateRole(id, role, UserContext.getUserId());
        return Result.ok("角色已修改", null);
    }

    /** 重置密码为 123456 */
    @PutMapping("/{id}/reset-password")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> resetPassword(@PathVariable Integer id) {
        userService.resetPassword(id, UserContext.getUserId());
        return Result.ok("密码已重置为 " + Constants.DEFAULT_PASSWORD, null);
    }
}
