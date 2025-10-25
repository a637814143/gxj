package com.gxj.cropyield.modules.auth.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
/**
 * 认证与账号模块的实体类，映射认证与账号领域对应的数据表结构。
 */

@Entity
@Table(name = "sys_role")
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sys_role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
