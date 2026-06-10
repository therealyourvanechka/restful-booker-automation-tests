package com.booker.controllers.booking;

import com.booker.model.request.BookingRequest;
import com.booker.model.response.BookingResponse;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Epic("Restful Booker API")
@Feature("Booking")
class GetBookingByIdControllerTest extends BaseBookingControllerTest {

    @Test
    @Tag("positive")
    @Story("Получение брони по ID")
    @DisplayName("GET /booking/{id} — данные совпадают с созданной бронью")
    void shouldGetBookingById() {
        BookingRequest request = createTestBooking();

        Response rawResponse = bookingClient.getBookingRaw(createdBookingId);
        assertThat(rawResponse.statusCode()).isEqualTo(HttpStatus.SC_OK);
        rawResponse.then().body(matchesJsonSchemaInClasspath("schemas/booking-schema.json"));

        BookingResponse booking = rawResponse.as(BookingResponse.class);
        assertSoftly(softly -> {
            softly.assertThat(booking.getFirstname()).as("Имя").isEqualTo(request.getFirstname());
            softly.assertThat(booking.getLastname()).as("Фамилия").isEqualTo(request.getLastname());
            softly.assertThat(booking.getTotalprice()).as("Цена").isEqualTo(request.getTotalprice());
            softly.assertThat(booking.getDepositpaid()).as("Депозит").isEqualTo(request.getDepositpaid());
            softly.assertThat(booking.getBookingdates().getCheckin()).as("Дата заезда").isEqualTo(request.getBookingdates().getCheckin());
            softly.assertThat(booking.getBookingdates().getCheckout()).as("Дата выезда").isEqualTo(request.getBookingdates().getCheckout());
            softly.assertThat(booking.getAdditionalneeds()).as("Доп. пожелания").isEqualTo(request.getAdditionalneeds());
        });
    }

    @Test
    @Tag("negative")
    @Story("Получение брони по ID")
    @DisplayName("GET /booking/{id} — несуществующий ID возвращает 404")
    void shouldReturn404ForNonExistentBooking() {
        Response response = bookingClient.getBookingRaw(NON_EXISTENT_ID);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
    }

    // @Disabled("Баг API: возвращает 404 вместо 400 при невалидном формате ID")
    @Test
    @Tag("negative")
    @Story("Получение брони по ID")
    @DisplayName("GET /booking/{id} — невалидный формат ID (строка), ожидается 400")
    void shouldReturn400ForInvalidIdFormat() {
        Response response = bookingClient.getBookingRaw("abc");
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }
}
