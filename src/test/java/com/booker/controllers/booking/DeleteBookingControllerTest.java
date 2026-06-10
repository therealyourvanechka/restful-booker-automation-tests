package com.booker.controllers.booking;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Restful Booker API")
@Feature("Booking")
class DeleteBookingControllerTest extends BaseBookingControllerTest {

    @Test
    @Tag("positive")
    @Story("Удаление брони")
    @DisplayName("DELETE /booking/{id} — бронь удалена, GET возвращает 404")
    void shouldDeleteBooking() {
        createTestBooking();

        Response deleteResponse = authBookingClient.deleteBooking(createdBookingId);
        assertThat(deleteResponse.statusCode())
                .as("DELETE /booking возвращает 201 согласно документации API")
                .isEqualTo(HttpStatus.SC_CREATED);

        Response getResponse = bookingClient.getBookingRaw(createdBookingId);
        assertThat(getResponse.statusCode()).as("После удаления GET должен вернуть 404").isEqualTo(HttpStatus.SC_NOT_FOUND);

        createdBookingId = 0;
    }

    @Test
    @Tag("negative")
    @Story("Удаление брони")
    @DisplayName("DELETE /booking/{id} — без авторизации возвращает 403")
    void shouldNotDeleteBookingWithoutToken() {
        createTestBooking();

        Response response = bookingClient.deleteBooking(createdBookingId);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }

    // @Disabled("Баг API: вместо 404 возвращает 405")
    @Test
    @Tag("negative")
    @Story("Удаление брони")
    @DisplayName("DELETE /booking/{id} — несуществующий ID, ожидается 404")
    void shouldNotDeleteNonExistentBooking() {
        Response response = authBookingClient.deleteBooking(NON_EXISTENT_ID);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
    }
}
