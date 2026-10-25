package com.forum.service;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class UserLevelTest {
    @Test
    void levelBoundaries() {
        assertThat(UserService.levelName(0)).contains("新人");
        assertThat(UserService.levelName(49)).contains("新人");
        assertThat(UserService.levelName(50)).contains("常客");
        assertThat(UserService.levelName(149)).contains("常客");
        assertThat(UserService.levelName(150)).contains("老船长");
        assertThat(UserService.levelName(399)).contains("老船长");
        assertThat(UserService.levelName(400)).contains("传奇");
    }
}
