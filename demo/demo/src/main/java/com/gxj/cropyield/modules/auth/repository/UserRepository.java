package com.gxj.cropyield.modules.auth.repository;

import com.gxj.cropyield.modules.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
/**
 * 认证与账号模块的数据访问接口（接口），封装了对认证与账号相关数据表的持久化操作。
 */

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByUsername(String username);

    @Query("select distinct u from User u join u.roles r where r.code = :roleCode and u.departmentCode = :departmentCode")
    List<User> findByRoleAndDepartment(@Param("roleCode") String roleCode, @Param("departmentCode") String departmentCode);

    @Query("""
        select distinct u from User u
        join u.roles r
        where r.code = :roleCode
        order by
            case when u.departmentCode is null or trim(u.departmentCode) = '' then 1 else 0 end,
            u.departmentCode asc,
            u.fullName asc,
            u.username asc,
            u.id asc
        """)
    List<User> findDepartmentUsers(@Param("roleCode") String roleCode);
}
