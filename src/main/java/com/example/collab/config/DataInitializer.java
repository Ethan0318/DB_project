package com.example.collab.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.collab.entity.Role;
import com.example.collab.entity.User;
import com.example.collab.entity.UserProfile;
import com.example.collab.entity.UserRole;
import com.example.collab.mapper.RoleMapper;
import com.example.collab.mapper.UserMapper;
import com.example.collab.mapper.UserProfileMapper;
import com.example.collab.mapper.UserRoleMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleMapper roleMapper,
                           UserMapper userMapper,
                           UserProfileMapper userProfileMapper,
                           UserRoleMapper userRoleMapper,
                           PasswordEncoder passwordEncoder) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initRoles();
        initDefaultUsers();
    }

    private void initRoles() {
        long count = roleMapper.selectCount(null);
        if (count > 0) {
            return;
        }
        Role admin = new Role();
        admin.setCode("ADMIN");
        admin.setName("Administrator");
        admin.setCreatedAt(LocalDateTime.now());
        roleMapper.insert(admin);

        Role editor = new Role();
        editor.setCode("EDITOR");
        editor.setName("Editor");
        editor.setCreatedAt(LocalDateTime.now());
        roleMapper.insert(editor);

        Role viewer = new Role();
        viewer.setCode("VIEWER");
        viewer.setName("Viewer");
        viewer.setCreatedAt(LocalDateTime.now());
        roleMapper.insert(viewer);
    }

    private void initDefaultUsers() {
        long count = userMapper.selectCount(null);
        if (count > 0) {
            return;
        }
        List<Role> roles = roleMapper.selectList(null);
        Long adminRoleId = roles.stream().filter(r -> "ADMIN".equals(r.getCode())).findFirst().map(Role::getId).orElse(null);
        Long editorRoleId = roles.stream().filter(r -> "EDITOR".equals(r.getCode())).findFirst().map(Role::getId).orElse(null);
        Long viewerRoleId = roles.stream().filter(r -> "VIEWER".equals(r.getCode())).findFirst().map(Role::getId).orElse(null);

        createUser("admin@example.com", "Admin", adminRoleId);
        createUser("editor@example.com", "Editor", editorRoleId);
        createUser("viewer@example.com", "Viewer", viewerRoleId);
    }

    private void createUser(String email, String nickname, Long roleId) {
        if (userMapper.selectCount(new QueryWrapper<User>().eq("email", email)) > 0) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(nickname);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.insert(profile);

        if (roleId != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }
}
