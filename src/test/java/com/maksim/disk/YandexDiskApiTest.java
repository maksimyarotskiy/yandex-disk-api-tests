package com.maksim.disk;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class YandexDiskApiTest {
    // Вставьте OAuth-токен тестового аккаунта перед запуском.
    private static final String TOKEN = "";
    private static final String API_BASE = "https://cloud-api.yandex.net/v1/disk";
    private static String testFolder;
    private static String copiedFolder;

    @BeforeAll
    static void setUp() {
        if (TOKEN.isBlank()) {
            throw new IllegalStateException("Укажите OAuth-токен в константе TOKEN в начале класса.");
        }

        RestAssured.baseURI = API_BASE;
        RestAssured.requestSpecification = given()
                .header("Authorization", "OAuth " + TOKEN)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");

        String suffix = UUID.randomUUID().toString();
        testFolder = "app:/api-tests-" + suffix;
        copiedFolder = "app:/api-tests-copy-" + suffix;
    }

    @Test
    void folderLifecycleUsesGetPostPutAndDelete() {
        // Создаём папку.
        given()
                .queryParam("path", testFolder)
        .when()
                .put("/resources")
        .then()
                .statusCode(201);

        // Проверяем, что папка создана.
        given()
                .queryParam("path", testFolder)
        .when()
                .get("/resources")
        .then()
                .statusCode(200)
                .body("type", equalTo("dir"));

        // Копируем папку.
        given()
                .queryParam("from", testFolder)
                .queryParam("path", copiedFolder)
                .queryParam("overwrite", false)
        .when()
                .post("/resources/copy")
        .then()
                .statusCode(201);

        given()
                .queryParam("path", copiedFolder)
        .when()
                .get("/resources")
        .then()
                .statusCode(200)
                .body("type", equalTo("dir"));
    }

    @AfterAll
    static void cleanUp() {
        if (testFolder == null) {
            return;
        }
        deleteIfPresent(copiedFolder);
        deleteIfPresent(testFolder);
    }

    private static void deleteIfPresent(String path) {
        if (path == null) {
            return;
        }
        given()
                .queryParam("path", path)
                .queryParam("permanently", true)
        .when()
                .delete("/resources")
        .then()
                .statusCode(org.hamcrest.Matchers.anyOf(
                        equalTo(202), equalTo(204), equalTo(404)));
    }
}
