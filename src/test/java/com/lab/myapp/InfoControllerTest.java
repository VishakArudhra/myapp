package com.lab.myapp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InfoController.class)
@TestPropertySource(properties = {
        "app.environment=dev",
        "app.greeting=Hello test",
        "app.api-key=secret"
})
class InfoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void reportsEnvironmentAndGreeting() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app").value("myapp"))
                .andExpect(jsonPath("$.environment").value("dev"))
                .andExpect(jsonPath("$.greeting").value("Hello test"));
    }

    @Test
    void neverLeaksTheApiKey() throws Exception {
        mvc.perform(get("/"))
                .andExpect(jsonPath("$.apiKeyConfigured").value(true))
                .andExpect(jsonPath("$.apiKey").doesNotExist());
    }
}
