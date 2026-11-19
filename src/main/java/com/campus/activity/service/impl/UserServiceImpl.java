package com.campus.activity.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.BusinessException;
import com.campus.activity.common.Constants;
import com.campus.activity.common.PageResult;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.LoginDTO;
import com.campus.activity.dto.PasswordDTO;
import com.campus.activity.dto.ProfileDTO;
import com.campus.activity.dto.RegisterDTO;
import com.campus.activity.dto.UserQuery;
import com.campus.activity.entity.SysUser;
import com.campus.activity.mapper.SysUserMapper;
import com.campus.activity.service.UserService;
import com.campus.activity.util.JwtUtil;
import com.campus.activity.vo.LoginVO;
import com.campus.activity.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final List<String> AVATAR_EXT = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Value("${file.upload-path:./upload}")
    private String uploadPath;

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));

        // 账号不存在和密码错误返回同一句提示，避免被用来枚举学号
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("学号/工号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == Constants.USER_DISABLED) {
            throw new BusinessException("该账号已被禁用，请联系管理员");
        }

        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        return vo;
    }

    @Override
    public void register(RegisterDTO dto) {
        String role = dto.getRole();
        if (role == null || role.trim().isEmpty()) {
            role = Constants.ROLE_STUDENT;
        }
        if (!Constants.ROLE_STUDENT.equals(role) && !Constants.ROLE_ORGANIZER.equals(role)) {
            throw new BusinessException("角色不合法，只能注册为学生或组织者");
        }

        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该学号/工号已注册");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setRealName(dto.getRealName().trim());
        user.setRole(role);
        user.setStudentNo(dto.getStudentNo());
        user.setPhone(dto.getPhone());
        user.setStatus(Constants.USER_ENABLED);
        user.setViolationCount(0);

        userMapper.insert(user);
        log.info("新用户注册成功: {} ({})", user.getUsername(), role);
    }

    @Override
    public UserVO getInfo(Integer userId) {
        return toVO(getExist(userId));
    }

    @Override
    public void updateProfile(Integer userId, ProfileDTO dto) {
        getExist(userId);
        SysUser update = new SysUser();
        update.setId(userId);
        update.setRealName(dto.getRealName().trim());
        update.setPhone(emptyToNull(dto.getPhone()));
        update.setEmail(emptyToNull(dto.getEmail()));
        update.setAvatar(emptyToNull(dto.getAvatar()));
        userMapper.updateById(update);
    }

    @Override
    public void changePassword(Integer userId, PasswordDTO dto) {
        SysUser user = getExist(userId);
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (BCrypt.checkpw(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userMapper.updateById(update);
        log.info("用户 {} 修改了密码", user.getUsername());
    }

    @Override
    public String uploadAvatar(Integer userId, MultipartFile file) {
        getExist(userId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!AVATAR_EXT.contains(ext)) {
            throw new BusinessException("只支持 jpg/jpeg/png/gif/webp 格式的图片");
        }

        // transferTo 遇到相对路径会落到 Tomcat 临时目录下，这里一律转成绝对路径
        File dir = new File(uploadPath, "avatar").getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("创建头像目录失败");
        }
        String filename = userId + "_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            file.transferTo(new File(dir, filename));
        } catch (IOException e) {
            log.error("头像保存失败", e);
            throw new BusinessException("头像保存失败，请稍后重试");
        }

        String url = "/upload/avatar/" + filename;
        SysUser update = new SysUser();
        update.setId(userId);
        update.setAvatar(url);
        userMapper.updateById(update);
        return url;
    }

    /* ---------------- 管理员 ---------------- */

    @Override
    public PageResult<UserVO> adminPage(UserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (query.getRole() != null && !query.getRole().trim().isEmpty()) {
            wrapper.eq(SysUser::getRole, query.getRole().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        String keyword = query.getKeyword() == null ? null : query.getKeyword().trim();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getStudentNo, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        wrapper.orderByDesc(SysUser::getId);

        Page<SysUser> page = new Page<>(
                query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum(),
                query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 50));
        userMapper.selectPage(page, wrapper);

        List<UserVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id, Integer status, Integer operatorId) {
        if (status == null || (status != Constants.USER_ENABLED && status != Constants.USER_DISABLED)) {
            throw new BusinessException("状态只能是 0（禁用）或 1（正常）");
        }
        SysUser user = getExist(id);
        if (id.equals(operatorId)) {
            throw new BusinessException("不能修改自己的账号状态");
        }
        if (status == Constants.USER_DISABLED && Constants.ROLE_ADMIN.equals(user.getRole())) {
            ensureNotLastAdmin("系统至少需要保留一个可用的管理员");
        }

        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
        log.info("管理员 {} 将用户 {} 状态改为 {}", operatorId, id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Integer id, String role, Integer operatorId) {
        if (!Constants.ROLE_STUDENT.equals(role) && !Constants.ROLE_ORGANIZER.equals(role)
                && !Constants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException("角色只能是 STUDENT / ORGANIZER / ADMIN");
        }
        SysUser user = getExist(id);
        if (id.equals(operatorId)) {
            throw new BusinessException("不能修改自己的角色");
        }
        if (Constants.ROLE_ADMIN.equals(user.getRole()) && !Constants.ROLE_ADMIN.equals(role)) {
            ensureNotLastAdmin("系统至少需要保留一个管理员");
        }

        SysUser update = new SysUser();
        update.setId(id);
        update.setRole(role);
        userMapper.updateById(update);
        log.info("管理员 {} 将用户 {} 角色改为 {}", operatorId, id, role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Integer id, Integer operatorId) {
        SysUser user = getExist(id);
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(BCrypt.hashpw(Constants.DEFAULT_PASSWORD));
        userMapper.updateById(update);
        log.info("管理员 {} 重置了用户 {} 的密码", operatorId, user.getUsername());
    }

    /* ---------------- 私有方法 ---------------- */

    private SysUser getExist(Integer id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void ensureNotLastAdmin(String message) {
        Long adminCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, Constants.ROLE_ADMIN)
                .eq(SysUser::getStatus, Constants.USER_ENABLED));
        if (adminCount != null && adminCount <= 1) {
            throw new BusinessException(message);
        }
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
