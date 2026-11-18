package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表。三种角色共用一张表，用 role 字段区分。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 登录账号：学生/教师用学号工号，管理员用 admin */
    private String username;

    /** BCrypt 加密后的密码，JSON 序列化时排除，避免泄露 */
    @JsonIgnore
    private String password;

    private String realName;

    /** STUDENT / ORGANIZER / ADMIN */
    private String role;

    private String studentNo;

    private String phone;

    private String email;

    private String avatar;

    /** 1 正常 0 禁用 */
    private Integer status;

    /** 累计爽约次数 */
    private Integer violationCount;

    /** 违约封禁截止时间，为空表示未被封禁 */
    private LocalDateTime banUntil;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
