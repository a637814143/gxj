package com.gxj.cropyield.modules.system.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
/**
 * 系统设置模块的实体类，映射系统设置领域对应的数据表结构。
 */

@Entity
@Table(name = "sys_log")
public class SystemLog extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String action;

    @Column(length = 512)
    private String detail;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
