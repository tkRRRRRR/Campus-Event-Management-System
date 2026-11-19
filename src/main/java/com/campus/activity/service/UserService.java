package com.campus.activity.service;

import com.campus.activity.common.PageResult;
import com.campus.activity.dto.LoginDTO;
import com.campus.activity.dto.PasswordDTO;
import com.campus.activity.dto.ProfileDTO;
import com.campus.activity.dto.RegisterDTO;
import com.campus.activity.dto.UserQuery;
import com.campus.activity.vo.LoginVO;
import com.campus.activity.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    /** 登录，返回 token 与基础信息 */
    LoginVO login(LoginDTO dto);

    /** 注册（只允许学生和组织者自助注册） */
    void register(RegisterDTO dto);

    /** 查询当前登录用户信息 */
    UserVO getInfo(Integer userId);

    /** 修改个人资料 */
    void updateProfile(Integer userId, ProfileDTO dto);

    /** 修改密码，需要校验原密码 */
    void changePassword(Integer userId, PasswordDTO dto);

    /** 上传头像，返回可访问的相对地址 */
    String uploadAvatar(Integer userId, MultipartFile file);

    /* ---------------- 管理员 ---------------- */

    PageResult<UserVO> adminPage(UserQuery query);

    void updateStatus(Integer id, Integer status, Integer operatorId);

    void updateRole(Integer id, String role, Integer operatorId);

    /** 重置密码为默认密码 */
    void resetPassword(Integer id, Integer operatorId);
}
