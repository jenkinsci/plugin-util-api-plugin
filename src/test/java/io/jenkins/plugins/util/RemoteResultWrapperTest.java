package io.jenkins.plugins.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RemoteResultWrapperTest {
    @Test
    void shouldCreateWrapper() {
        var result = "result";

        var wrapper = new RemoteResultWrapper<>(result, "title");

        assertThat(wrapper.getResult()).isEqualTo(result);

        wrapper.logInfo("Hello %s", "World");
        assertThat(wrapper.getInfoMessages()).containsExactly("Hello World");
    }
}
