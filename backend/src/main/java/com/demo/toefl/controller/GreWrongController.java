package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.GreWrong;
import com.demo.toefl.mapper.GreWrongMapper;
import com.demo.toefl.util.AuthHelper;
import com.demo.toefl.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GRE 错题本接口（数据表 gre_wrong，与托福错题本 wrong_book 完全分开）
 *
 *   GET    /api/greWrong/list        我的 GRE 错题（含各模块统计）
 *   POST   /api/greWrong/master      {id, mastered} 标记掌握 / 撤销掌握
 *   DELETE /api/greWrong/{id}        从错题本移除
 *   DELETE /api/greWrong/all?userId= 清空（只清 mastered=0 的）
 */
@RestController
@RequestMapping("/api/greWrong")
public class GreWrongController {

    @Autowired
    private GreWrongMapper wrongMapper;

    @Autowired
    private AuthHelper authHelper;

    private static final String[] MODULE_ORDER = {"verbal", "quant", "writing"};

    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) Long userId,
                                            @RequestParam(required = false) String moduleType,
                                            @RequestParam(required = false, defaultValue = "false")
                                            boolean includeMastered,
                                            HttpServletRequest request) {
        Long uid = resolveUserId(userId, request);

        LambdaQueryWrapper<GreWrong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GreWrong::getUserId, uid);
        if (!includeMastered) {
            wrapper.eq(GreWrong::getMastered, 0);
        }
        if (moduleType != null && !moduleType.isEmpty()) {
            wrapper.eq(GreWrong::getModuleType, moduleType);
        }
        wrapper.orderByDesc(GreWrong::getUpdateTime).orderByDesc(GreWrong::getId);

        List<GreWrong> rows = wrongMapper.selectList(wrapper);

        // 分模块统计（不受 moduleType 过滤影响，方便页面显示三个角标）
        Map<String, Integer> stats = new LinkedHashMap<>();
        for (String module : MODULE_ORDER) {
            int count = (int) (long) wrongMapper.selectCount(new LambdaQueryWrapper<GreWrong>()
                    .eq(GreWrong::getUserId, uid)
                    .eq(GreWrong::getMastered, 0)
                    .eq(GreWrong::getModuleType, module));
            stats.put(module, count);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", rows);
        data.put("stats", stats);
        data.put("total", rows.size());
        return Result.success(data);
    }

    @PostMapping("/master")
    public Result<String> master(@RequestBody Map<String, Object> body,
                                 HttpServletRequest request) {
        if (body == null || body.get("id") == null) {
            return Result.fail("缺少错题id（id）");
        }
        Long id = Long.valueOf(body.get("id").toString());
        GreWrong row = wrongMapper.selectById(id);
        if (row == null) {
            return Result.fail("错题不存在，id=" + id);
        }
        boolean mastered = body.get("mastered") == null
                || Boolean.parseBoolean(body.get("mastered").toString());
        row.setMastered(mastered ? 1 : 0);
        row.setUpdateTime(java.time.LocalDateTime.now());
        wrongMapper.updateById(row);
        return Result.success(mastered ? "已标记掌握" : "已恢复为待复习");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        GreWrong row = wrongMapper.selectById(id);
        if (row == null) {
            return Result.fail("错题不存在，id=" + id);
        }
        wrongMapper.deleteById(id);
        return Result.success("已移出错题本");
    }

    @DeleteMapping("/all")
    public Result<Map<String, Object>> clearAll(@RequestParam(required = false) Long userId,
                                                HttpServletRequest request) {
        Long uid = resolveUserId(userId, request);
        int removed = wrongMapper.delete(new LambdaQueryWrapper<GreWrong>()
                .eq(GreWrong::getUserId, uid)
                .eq(GreWrong::getMastered, 0));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("removed", removed);
        return Result.success(data);
    }

    private Long resolveUserId(Long userId, HttpServletRequest request) {
        if (userId != null) {
            return userId;
        }
        return authHelper.currentUserId(request);
    }
}
