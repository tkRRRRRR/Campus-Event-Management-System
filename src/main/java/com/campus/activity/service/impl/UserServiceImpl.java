package com.campus.activity.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.common.BusinessException;
import com.campus.activity.common.Constants;
import com.campus.activity.common.ResultCode;
import com.campus.activity.dto.LoginDTO;
import com.campus.activity.dto.RegisterDTO;
import com.campus.activity.entity.SysUser;
import com.campus.activity.mapper.SysUserMapper;
import com.campus.activity.service.UserService;
import com.campus.activity.util.JwtUtil;
import com.campus.activity.vo.LoginVO;
import com.campus.activity.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;

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
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return BeanUtil.copyProperties(user, UserVO.class);
    }
}
