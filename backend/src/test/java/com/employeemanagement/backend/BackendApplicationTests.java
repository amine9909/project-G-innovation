package com.employeemanagement.backend;

import com.employeemanagement.backend.dto.user.UpdateUserRequest;
import com.employeemanagement.backend.dto.user.UserResponse;
import com.employeemanagement.backend.entity.User;
import com.employeemanagement.backend.enums.Permission;
import com.employeemanagement.backend.enums.UserType;
import com.employeemanagement.backend.repository.UserRepository;
import com.employeemanagement.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class BackendApplicationTests {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
	}

	@Test
	void normalUserWithUpdateCannotUpdateSuperAdmin() {
		User actor = saveUser(
				"Normal Updater",
				"normal-updater",
				"normal-updater@example.local",
				UserType.NORMAL_USER,
				Set.of(Permission.UPDATE),
				true
		);
		User target = saveUser(
				"Protected Admin",
				"protected-admin",
				"protected-admin@example.local",
				UserType.SUPER_ADMIN,
				EnumSet.allOf(Permission.class),
				true
		);
		UpdateUserRequest request = updateRequest(
				"Changed Admin",
				"changed-admin",
				"changed-admin@example.local",
				false
		);

		AccessDeniedException exception = assertThrows(
				AccessDeniedException.class,
				() -> userService.updateUser(target.getId(), request, actor.getUsername())
		);

		assertEquals("You are not allowed to modify a Super Admin account.", exception.getMessage());
	}

	@Test
	void superAdminCanUpdateSuperAdmin() {
		User actor = saveUser(
				"Super Admin Actor",
				"super-admin-actor",
				"super-admin-actor@example.local",
				UserType.SUPER_ADMIN,
				EnumSet.allOf(Permission.class),
				true
		);
		User target = saveUser(
				"Super Admin Target",
				"super-admin-target",
				"super-admin-target@example.local",
				UserType.SUPER_ADMIN,
				EnumSet.allOf(Permission.class),
				true
		);
		UpdateUserRequest request = updateRequest(
				"Updated Super Admin",
				"updated-super-admin",
				"updated-super-admin@example.local",
				false
		);

		UserResponse response = userService.updateUser(target.getId(), request, actor.getUsername());

		assertEquals("Updated Super Admin", response.getName());
		assertEquals("updated-super-admin", response.getUsername());
		assertEquals("updated-super-admin@example.local", response.getEmail());
		assertFalse(response.isActive());
	}

	@Test
	void normalUserWithUpdateCanUpdateNormalUser() {
		User actor = saveUser(
				"Normal Updater Two",
				"normal-updater-two",
				"normal-updater-two@example.local",
				UserType.NORMAL_USER,
				Set.of(Permission.UPDATE),
				true
		);
		User target = saveUser(
				"Normal Target",
				"normal-target",
				"normal-target@example.local",
				UserType.NORMAL_USER,
				Set.of(),
				true
		);
		UpdateUserRequest request = updateRequest(
				"Updated Normal",
				"updated-normal",
				"updated-normal@example.local",
				false
		);

		UserResponse response = userService.updateUser(target.getId(), request, actor.getUsername());

		assertEquals("Updated Normal", response.getName());
		assertEquals("updated-normal", response.getUsername());
		assertEquals("updated-normal@example.local", response.getEmail());
		assertFalse(response.isActive());
	}

	private User saveUser(String name, String username, String email, UserType userType,
						  Set<Permission> permissions, boolean active) {
		User user = new User();
		user.setName(name);
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode("Password123"));
		user.setUserType(userType);
		user.setPermissions(new HashSet<>(permissions));
		user.setActive(active);

		return userRepository.saveAndFlush(user);
	}

	private UpdateUserRequest updateRequest(String name, String username, String email, boolean active) {
		UpdateUserRequest request = new UpdateUserRequest();
		request.setName(name);
		request.setUsername(username);
		request.setEmail(email);
		request.setActive(active);
		return request;
	}

}
