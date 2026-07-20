package com.gold.safefam.domain.notification.controller;

import com.gold.safefam.domain.notification.entity.Device;
import com.gold.safefam.domain.notification.enums.DevicePlatform;
import com.gold.safefam.domain.notification.repository.DeviceRepository;
import com.gold.safefam.domain.user.entity.User;
import com.gold.safefam.domain.user.repository.UserRepository;
import com.gold.safefam.global.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class DeviceControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private String accessToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        deviceRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(new User(
                "01012345678",
                passwordEncoder.encode("safefam12"),
                "김안전"
        ));
        accessToken = "Bearer " + jwtUtil.generateAccessToken(testUser.getId());
    }

    @Test
    void registerDeviceReturns201() throws Exception {
        mockMvc.perform(post("/api/v1/devices")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceToken": "test-fcm-token-123",
                                  "platform": "ANDROID"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.platform").value("ANDROID"));
    }

    @Test
    void registerDuplicateTokenReturnsSameDevice() throws Exception {
        mockMvc.perform(post("/api/v1/devices")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceToken": "test-fcm-token-123",
                                  "platform": "ANDROID"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/devices")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceToken": "test-fcm-token-123",
                                  "platform": "ANDROID"
                                }
                                """))
                .andExpect(status().isCreated());

        assertEquals(1, deviceRepository.findByUserId(testUser.getId()).size());
    }

    @Test
    void unregisterDeviceReturns204() throws Exception {
        Device device = deviceRepository.save(
                new Device(testUser.getId(), "test-fcm-token-123", DevicePlatform.ANDROID)
        );

        mockMvc.perform(delete("/api/v1/devices/" + device.getId())
                        .header("Authorization", accessToken))
                .andExpect(status().isNoContent());

        assertFalse(deviceRepository.existsById(device.getId()));
    }

    @Test
    void unregisterOtherUsersDeviceReturns403() throws Exception {
        User otherUser = userRepository.save(new User(
                "01099999999",
                passwordEncoder.encode("safefam12"),
                "다른유저"
        ));
        Device device = deviceRepository.save(
                new Device(otherUser.getId(), "other-fcm-token", DevicePlatform.ANDROID)
        );

        mockMvc.perform(delete("/api/v1/devices/" + device.getId())
                        .header("Authorization", accessToken))
                .andExpect(status().isForbidden());
    }
}