package com.iot.qa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iot.qa.entity.SysConfig;
import com.iot.qa.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统参数服务。
 *
 * <p>参数存放在 sys_config 表中，支持管理员在后台运行时修改而无需重启服务。
 * 为避免每次问答都查库，这里做了一层 30 秒有效期的本地缓存。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {

    /** 缓存有效期（毫秒） */
    private static final long TTL_MS = 30_000L;

    private final SysConfigMapper sysConfigMapper;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    private volatile long loadedAt = 0L;

    /** 读取参数，不存在时返回默认值 */
    public String get(String key, String defaultValue) {
        loadIfExpired();
        String value = cache.get(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 查询全部参数（后台配置页展示） */
    public List<SysConfig> listAll() {
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getId));
    }

    /** 新增或更新参数 */
    @Transactional(rollbackFor = Exception.class)
    public void save(String key, String value) {
        SysConfig exists = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key)
                .last("LIMIT 1"));
        if (exists == null) {
            SysConfig config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            sysConfigMapper.insert(config);
        } else {
            exists.setConfigValue(value);
            sysConfigMapper.updateById(exists);
        }
        refresh();
    }

    /** 强制刷新缓存 */
    public void refresh() {
        loadedAt = 0L;
        loadIfExpired();
    }

    private void loadIfExpired() {
        if (System.currentTimeMillis() - loadedAt < TTL_MS) {
            return;
        }
        synchronized (this) {
            if (System.currentTimeMillis() - loadedAt < TTL_MS) {
                return;
            }
            try {
                List<SysConfig> configs = sysConfigMapper.selectList(null);
                Map<String, String> fresh = new ConcurrentHashMap<>();
                for (SysConfig config : configs) {
                    if (config.getConfigKey() != null) {
                        fresh.put(config.getConfigKey(), config.getConfigValue());
                    }
                }
                cache.clear();
                cache.putAll(fresh);
                loadedAt = System.currentTimeMillis();
            } catch (Exception e) {
                // 数据库暂不可用时保持旧缓存，并延长一个周期后重试
                log.warn("加载系统参数失败，继续使用缓存值: {}", e.getMessage());
                loadedAt = System.currentTimeMillis();
            }
        }
    }
}