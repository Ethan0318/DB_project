package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.entity.OpLog;
import com.example.collab.mapper.OpLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OpLogService {

    private final OpLogMapper opLogMapper;

    public OpLogService(OpLogMapper opLogMapper) {
        this.opLogMapper = opLogMapper;
    }

    public void log(Long userId, String action, String detail, String ip) {
        OpLog log = new OpLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDetail(detail);
        log.setIp(ip);
        log.setCreatedAt(LocalDateTime.now());
        opLogMapper.insert(log);
    }

    public long countByUser(Long userId) {
        return opLogMapper.selectCount(new QueryWrapper<OpLog>().eq("user_id", userId));
    }
}
