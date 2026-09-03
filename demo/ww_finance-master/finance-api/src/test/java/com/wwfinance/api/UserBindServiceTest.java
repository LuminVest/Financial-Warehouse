package com.wwfinance.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wwfinance.api.entity.User;
import com.wwfinance.api.entity.UserBind;
import com.wwfinance.api.entity.dto.UserBindDTO;
import com.wwfinance.api.mapper.UserBindMapper;
import com.wwfinance.api.service.UserBindService;
import com.wwfinance.api.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserBindService 绑定流程测试：
 * 1. commitBindUser：保存待绑定记录(status=0) + 构建托管平台表单
 * 2. notify：异步回调更新绑定状态(status=1) + 同步 user 表 bind_code/bind_status
 * <p>
 * 注意：测试连接的是本地真实库，使用测试专用手机号，用例结束后自动清理数据。
 */
@SpringBootTest(classes = ApiApplication.class)
class UserBindServiceTest {

    /** 测试专用手机号（不与正式用户冲突） */
    private static final String TEST_MOBILE = "19900009999";

    @Resource
    private UserBindService userBindService;

    @Resource
    private UserBindMapper userBindMapper;

    @Resource
    private UserService userService;

    /** 测试用户 id */
    private Long userId;

    @BeforeEach
    void setUp() {
        // 每个用例前创建独立测试用户，保证用例之间互不干扰
        User user = new User()
                .setMobile(TEST_MOBILE)
                .setUserType(1)
                .setName(TEST_MOBILE)
                .setNickName(TEST_MOBILE)
                .setStatus(1);
        userService.save(user);
        userId = user.getId();
    }

    @AfterEach
    void tearDown() {
        // 清理：删除测试产生的绑定记录 + 测试用户
        if (userId != null) {
            userBindMapper.delete(new LambdaQueryWrapper<UserBind>().eq(UserBind::getUserId, userId));
            userService.removeById(userId);
        }
    }

    /** 测试1：提交绑定 -> 落库待绑定记录(status=0) + 返回托管平台表单 */
    @Test
    void testCommitBindUser() {
        UserBindDTO dto = buildUserBindDTO();

        String formStr = userBindService.commitBindUser(dto, userId);

        // 1. 返回的是自动提交表单
        assertNotNull(formStr);
        assertTrue(formStr.contains("autoForm"), "应返回包含 autoForm 的 HTML 表单");
        assertTrue(formStr.contains("method='post'"), "表单应为 POST 提交");

        // 2. 数据库已落库待绑定记录：status=0、姓名/卡号已保存
        UserBind bind = userBindService.getBindByUserId(userId);
        assertNotNull(bind, "应存在绑定记录");
        assertEquals(0, bind.getStatus().intValue(), "提交后应为待绑定状态(0)");
        assertEquals(dto.getName(), bind.getName());
        assertEquals(dto.getBankNo(), bind.getBankNo());
        assertEquals(TEST_MOBILE, bind.getMobile());
    }

    /** 测试2：重复提交绑定 -> 更新原记录而不是新增（user_id 唯一） */
    @Test
    void testCommitBindUserTwice() {
        UserBindDTO dto = buildUserBindDTO();

        userBindService.commitBindUser(dto, userId);
        // 第二次提交：修改姓名，验证是更新而非新增
        dto.setName("测试用户-改");
        userBindService.commitBindUser(dto, userId);

        UserBind bind = userBindService.getBindByUserId(userId);
        assertNotNull(bind);
        assertEquals("测试用户-改", bind.getName(), "重复提交应同步最新数据");
    }

    /** 测试3：异步回调 -> 绑定状态置为成功(1) + 同步 user 表 bind_code/bind_status */
    @Test
    void testNotify() {
        // 先提交绑定，产生一条 status=0 记录
        userBindService.commitBindUser(buildUserBindDTO(), userId);

        // 模拟托管平台回调
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentUserId", userId);
        paramMap.put("bindCode", "BIND_TEST001");
        userBindService.notify(paramMap);

        // 1. user_bind 状态更新为成功
        UserBind bind = userBindService.getBindByUserId(userId);
        assertNotNull(bind);
        assertEquals(1, bind.getStatus().intValue(), "回调后应为绑定成功(1)");
        assertEquals("BIND_TEST001", bind.getBindCode(), "应写入绑定协议号");

        // 2. user 表同步绑定状态与协议号
        User user = userService.getById(userId);
        assertNotNull(user);
        assertEquals(1, user.getBindStatus().intValue(), "user.bind_status 应为1");
        assertEquals("BIND_TEST001", user.getBindCode(), "user.bind_code 应同步");
    }

    /** 构造绑定提交参数 */
    private UserBindDTO buildUserBindDTO() {
        UserBindDTO dto = new UserBindDTO();
        dto.setName("测试用户");
        dto.setIdCard("410123199901011234");
        dto.setBankNo("6222021234567890123");
        dto.setBankType("工商银行");
        dto.setMobile(TEST_MOBILE);
        return dto;
    }
}
