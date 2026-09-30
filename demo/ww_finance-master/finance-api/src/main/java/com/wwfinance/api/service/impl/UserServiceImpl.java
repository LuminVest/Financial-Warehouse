package com.wwfinance.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.UserAccount;
import com.wwfinance.api.entity.UserLoginRecord;
import com.wwfinance.api.entity.dto.AdminUserQuery;
import com.wwfinance.api.entity.dto.UserDto;
import com.wwfinance.common.exception.BusinessException;
import com.wwfinance.api.mapper.UserLoginRecordMapper;
import com.wwfinance.api.mapper.UserMapper;
import com.wwfinance.api.service.UserAccountService;
import com.wwfinance.api.service.UserService;
import com.wwfinance.api.utils.TokenUtil;
import com.wwfinance.common.utils.MD5;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /** 注册验证码 Redis key 前缀（与短信服务约定保持一致） */
    private static final String SMS_CODE_PREFIX = "xx:code:";

    /** 验证码有效期：5 分钟 */
    private static final long SMS_CODE_EXPIRE_MINUTES = 5;

    @Autowired
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private UserLoginRecordMapper userLoginRecordMapper;

    @Override
    public User getUserById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public IPage<User> listPage(Page<User> pageParam, AdminUserQuery adminUserQuery) {
        if (adminUserQuery == null) {
            return baseMapper.selectPage(pageParam, null);
        }
        String mobile = adminUserQuery.getMobile();
        Integer status = adminUserQuery.getStatus();
        Integer userType = adminUserQuery.getUserType();

        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq(StringUtils.isNotBlank(mobile), "mobile", mobile)
                .eq(status != null, "status", status)
                .eq(userType != null, "user_type", userType);
        return baseMapper.selectPage(pageParam, userQueryWrapper);
    }

    @Override
    public UserDto getUserDto(Long id) {
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserDto userDto = new UserDto();
        userDto.setMobile(user.getMobile());
        userDto.setUserType(user.getUserType());
        return userDto;
    }

    @Override
    public boolean checkMobile(String mobile) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getMobile, mobile);
        return this.getOne(queryWrapper) == null;
    }

    @Override
    public String sendCode(String mobile) {
        // 1. 手机号非空 + 11 位手机号格式校验
        if (mobile == null || !mobile.matches("^1\\d{10}$")) {
            throw new BusinessException("手机号格式不正确");
        }
        // 2. 已注册的手机号不允许再发送注册验证码
        if (!checkMobile(mobile)) {
            throw new BusinessException("手机号已被注册");
        }
        // 3. 生成 4 位随机验证码并存入 Redis，有效期 5 分钟
        String code = String.valueOf(new Random().nextInt(9000) + 1000);
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + mobile, code, SMS_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("注册验证码已生成：mobile={}, code={}", mobile, code);
        // demo 阶段：直接返回验证码方便前后端联调；生产环境应通过短信网关下发，禁止返回
        return code;
    }

    @Override
    public void register(UserDto userDTO) {
        // 1. 校验验证码：注册前必须通过 /api/core/user/sendCode 获取验证码
        Object cacheCode = redisTemplate.opsForValue().get(SMS_CODE_PREFIX + userDTO.getMobile());
        if (userDTO.getCode() == null || cacheCode == null || !userDTO.getCode().equals(cacheCode.toString())) {
            throw new BusinessException("验证码错误或已过期，请重新获取");
        }
        // 2. 判断手机号是否已存在
        if (!checkMobile(userDTO.getMobile())) {
            throw new BusinessException("手机号已被注册");
        }
        // 3. 判断两次密码是否一致
        if (!userDTO.getPassword().equals(userDTO.getPasswordto())) {
            throw new BusinessException("两次输入的密码不正确");
        }
        // 4. 保存用户（密码 MD5 加密）
        User user = new User();
        user.setMobile(userDTO.getMobile());
        user.setUserType(userDTO.getUserType());
        user.setName(userDTO.getMobile());
        user.setNickName(userDTO.getMobile());
        user.setPassword(MD5.encrypt(userDTO.getPassword()));
        user.setStatus(1);
        this.save(user);
        // 5. 同步创建托管账户
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(user.getId());
        userAccountService.save(userAccount);
        // 6. 注册成功，删除已使用的验证码，防止被重复使用
        redisTemplate.delete(SMS_CODE_PREFIX + userDTO.getMobile());
    }

    @Override
    public Map<String, Object> login(UserDto userDTO) {
        // 1. 按手机号查用户
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getMobile, userDTO.getMobile()));
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }
        // 2. 校验密码（数据库存的是 MD5 加密后的值）
        if (!MD5.encrypt(userDTO.getPassword()).equals(user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        // 3. 校验账号状态（1 正常 / 0 禁用）
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        // 4. 生成 token（claims 中存 userId，避免手机号超出 int 范围）
        String token = TokenUtil.generateMerchantToken(String.valueOf(user.getId()));
        // 4.1 记录登录日志（写入 user_login_record，含登录 IP）
        saveLoginRecord(user);
        // 5. 组装返回：token + 用户信息（password 字段已被 @JsonIgnore，不会返回）
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);
        return data;
    }

    /** 登录成功后写一条登录日志（失败不影响登录主流程） */
    private void saveLoginRecord(User user) {
        try {
            UserLoginRecord record = new UserLoginRecord();
            record.setUserId(user.getId());
            record.setIp(getClientIp());
            record.setCreateTime(LocalDateTime.now());
            record.setIsDeleted(false);
            userLoginRecordMapper.insert(record);
        } catch (Exception e) {
            log.warn("记录登录日志失败: userId={}, err={}", user.getId(), e.getMessage());
        }
    }

    /** 从请求头/请求中取客户端 IP，取不到返回空串 */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "";
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip == null ? "" : ip;
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void updateProfile(Long userId, String nickName, Integer gender) {
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (nickName == null || nickName.trim().isEmpty()) {
            throw new BusinessException("昵称不能为空");
        }
        nickName = nickName.trim();
        if (nickName.length() > 20) {
            throw new BusinessException("昵称最长 20 个字符");
        }
        if (gender != null && (gender < 0 || gender > 2)) {
            throw new BusinessException("性别参数不合法");
        }
        User update = new User();
        update.setId(userId);
        update.setNickName(nickName);
        if (gender != null) {
            update.setGender(gender);
        }
        this.updateById(update);
        log.info("用户更新个人资料: userId={}, nickName={}, gender={}", userId, nickName, gender);
    }
}
