package com.example.library.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.entity.User;
import com.example.library.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用户业务服务
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户注册
     */
    public boolean register(User user) {
        // 检查用户名是否已存在
        User existing = userMapper.findByUsername(user.getUsername());
        if (existing != null) {
            return false;
        }
        // 密码MD5加密（课程项目简单处理；生产环境应使用BCrypt）
        user.setPassword(DigestUtils.md5DigestAsHex(
                user.getPassword().getBytes(StandardCharsets.UTF_8)));
        user.setRole(0);  // 默认注册为读者
        user.setStatus(1); // 默认正常状态
        return userMapper.insert(user) > 0;
    }

    /**
     * 用户登录
     * @return null表示登录失败，否则返回用户对象
     */
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return null;
        }
        if (user.getStatus() == 0) {
            return null; // 账号已禁用
        }
        String encryptedPwd = DigestUtils.md5DigestAsHex(
                password.getBytes(StandardCharsets.UTF_8));
        if (encryptedPwd.equals(user.getPassword())) {
            // 登录成功，清除密码后返回
            user.setPassword(null);
            return user;
        }
        return null;
    }

    /**
     * 根据ID查询用户
     */
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 查询所有读者
     */
    public List<User> listReaders() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("role", 0);
        return userMapper.selectList(wrapper);
    }

    /**
     * 更新用户信息
     */
    public boolean update(User user) {
        // 不更新密码
        user.setPassword(null);
        return userMapper.updateById(user) > 0;
    }

    /**
     * 修改密码
     */
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;

        String oldEncrypted = DigestUtils.md5DigestAsHex(
                oldPassword.getBytes(StandardCharsets.UTF_8));
        if (!oldEncrypted.equals(user.getPassword())) {
            return false; // 旧密码不正确
        }

        user.setPassword(DigestUtils.md5DigestAsHex(
                newPassword.getBytes(StandardCharsets.UTF_8)));
        return userMapper.updateById(user) > 0;
    }

    /**
     * 禁用/启用用户
     */
    public boolean toggleStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        return userMapper.updateById(user) > 0;
    }

    /**
     * 分页查询读者（管理员用）
     */
    public IPage<User> listReadersByPage(int pageNum, int pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("role", 0);
        return userMapper.selectPage(page, wrapper);
    }

    /**
     * 获取统计信息
     */
    public int countReaders() {
        return userMapper.countActiveReaders();
    }
}
