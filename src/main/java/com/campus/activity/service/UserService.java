package com.campus.activity.service;

import com.campus.activity.dto.LoginDTO;
import com.campus.activity.dto.RegisterDTO;
import com.campus.activity.vo.LoginVO;
import com.campus.activity.vo.UserVO;

public interface UserService {

    /** 登录，返回 token 与基础信息 */
    LoginVO login(LoginDTO dto);

    /** 注册（只允许学生和组织者自助注册） */
    void register(RegisterDTO dto);

    /** 查询当前登录用户信息 */
    UserVO getInfo(Integer userId);
}
