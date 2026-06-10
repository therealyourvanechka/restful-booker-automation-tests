package com.booker.controllers.booking;

import com.booker.model.BookingDates;
import com.booker.model.request.BookingRequest;
import com.booker.model.response.BookingIdResponse;
import com.booker.util.BookingDataFactory;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Restful Booker API")
@Feature("Booking")
class GetBookingControllerTest extends BaseBookingControllerTest {

    @Test
    @Tag("positive")
    @Story("Получение списка броней")
    @DisplayName("GET /booking — без фильтров возвращает непустой список")
    void shouldGetAllBookings() {
        Response response = bookingClient.getBookingsRaw();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
        response.then().body(matchesJsonSchemaInClasspath("schemas/booking-ids-schema.json"));

        List<BookingIdResponse> bookings = response.jsonPath().getList(".", BookingIdResponse.class);
        assertThat(bookings).as("Список броней не должен быть пустым").isNotEmpty();
    }

    @Test
    @Tag("positive")
    @Story("Получение списка броней")
    @DisplayName("GET /booking — фильтр по firstname и lastname возвращает созданную бронь")
    void shouldGetBookingsFilteredByName() {
        BookingRequest request = createTestBooking();

        Map<String, Object> params = new HashMap<>();
        params.put("firstname", request.getFirstname());
        params.put("lastname", request.getLastname());

        List<BookingIdResponse> bookings = bookingClient.getBookingsWithFilters(params);

        assertThat(bookings).as("Список не должен быть пустым").isNotEmpty();
        assertThat(bookings)
                .extracting(BookingIdResponse::getBookingid)
                .as("Созданная бронь должна быть в списке")
                .contains(createdBookingId);
    }

    @Test
    @Tag("negative")
    @Story("Получение списка броней")
    @DisplayName("GET /booking — несуществующий firstname возвращает пустой список")
    void shouldReturnEmptyListForNonExistentFirstname() {
        Map<String, Object> params = new HashMap<>();
        params.put("firstname", "ИмяКотороеТочноНеСуществует12345");

        List<BookingIdResponse> bookings = bookingClient.getBookingsWithFilters(params);
        assertThat(bookings).as("Список должен быть пустым").isEmpty();
    }

    @Disabled("Баг API: возвращает 500 вместо 400 при невалидном формате checkin")
    @ParameterizedTest
    @ValueSource(strings = {"abc", "2024-13-01", "2024-01-78", "", "01-06-2024"})
    @Tag("negative")
    @Story("Фильтрация по checkin")
    @DisplayName("GET /booking — невалидный формат checkin, ожидается 400")
    void checkinInvalidFormat(String checkin) {
        Map<String, Object> params = new HashMap<>();
        params.put("checkin", checkin);

        Response response = bookingClient.getBookingsRaw(params);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Disabled("Баг API: возвращает 500 вместо 400 при невалидном формате checkout")
    @ParameterizedTest
    @ValueSource(strings = {"abc", "2024-13-01", "2024-01-78", "", "01-06-2024"})
    @Tag("negative")
    @Story("Фильтрация по checkout")
    @DisplayName("GET /booking — невалидный формат checkout, ожидается 400")
    void checkoutInvalidFormat(String checkout) {
        Map<String, Object> params = new HashMap<>();
        params.put("checkout", checkout);

        Response response = bookingClient.getBookingsRaw(params);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @Disabled("Документация: checkin >= param, API: checkin > param — checkin=15 (граница) не проходит из-за строгого сравнения")
    @ParameterizedTest
    @ValueSource(strings = {"2024-06-14", "2024-06-15"})
    @Tag("positive")
    @Story("Фильтрация по checkin")
    @DisplayName("GET /booking — checkin >= param, найдёт")
    void shouldFindByCheckin(String checkin) {
        BookingRequest request = BookingDataFactory.defaultBooking()
                .bookingdates(new BookingDates(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 22)))
                .build();
        createdBookingId = bookingClient.createBooking(request).getBookingid();

        Map<String, Object> params = new HashMap<>();
        params.put("checkin", checkin);

        List<BookingIdResponse> bookings = bookingClient.getBookingsWithFilters(params);
        assertThat(bookings)
                .extracting(BookingIdResponse::getBookingid)
                .as("Бронь должна быть найдена при checkin=%s", checkin)
                .contains(createdBookingId);
    }

    @Test
    @Tag("negative")
    @Story("Фильтрация по checkin")
    @DisplayName("GET /booking — checkin вне границы, не найдёт")
    void shouldNotFindByCheckin() {
        BookingRequest request = BookingDataFactory.defaultBooking()
                .bookingdates(new BookingDates(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 22)))
                .build();
        createdBookingId = bookingClient.createBooking(request).getBookingid();

        Map<String, Object> params = new HashMap<>();
        params.put("checkin", "2024-06-16");

        List<BookingIdResponse> bookings = bookingClient.getBookingsWithFilters(params);
        assertThat(bookings)
                .extracting(BookingIdResponse::getBookingid)
                .as("Бронь не должна быть найдена при checkin=16")
                .doesNotContain(createdBookingId);
    }

    @Disabled("Документация: checkout >= param, API: checkout <= param — checkout=21 не проходит")
    @ParameterizedTest
    @ValueSource(strings = {"2024-06-21", "2024-06-22"})
    @Tag("positive")
    @Story("Фильтрация по checkout")
    @DisplayName("GET /booking — checkout >= param, найдёт")
    void shouldFindByCheckout(String checkout) {
        BookingRequest request = BookingDataFactory.defaultBooking()
                .bookingdates(new BookingDates(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 22)))
                .build();
        createdBookingId = bookingClient.createBooking(request).getBookingid();

        Map<String, Object> params = new HashMap<>();
        params.put("checkout", checkout);

        List<BookingIdResponse> bookings = bookingClient.getBookingsWithFilters(params);
        assertThat(bookings)
                .extracting(BookingIdResponse::getBookingid)
                .as("Бронь должна быть найдена при checkout=%s", checkout)
                .contains(createdBookingId);
    }

    @Disabled("Документация: checkout >= param, API: checkout <= param — бронь находится при checkout=23")
    @Test
    @Tag("negative")
    @Story("Фильтрация по checkout")
    @DisplayName("GET /booking — checkout вне границы, не найдёт")
    void shouldNotFindByCheckout() {
        BookingRequest request = BookingDataFactory.defaultBooking()
                .bookingdates(new BookingDates(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 22)))
                .build();
        createdBookingId = bookingClient.createBooking(request).getBookingid();

        Map<String, Object> params = new HashMap<>();
        params.put("checkout", "2024-06-23");

        List<BookingIdResponse> bookings = bookingClient.getBookingsWithFilters(params);
        assertThat(bookings)
                .extracting(BookingIdResponse::getBookingid)
                .as("Бронь не должна быть найдена при checkout=23")
                .doesNotContain(createdBookingId);
    }

    @Test
    @Tag("positive")
    @Story("Получение списка броней")
    @DisplayName("GET /booking — комбинированный фильтр firstname + checkin")
    void shouldGetBookingsByFirstnameAndCheckin() {
        BookingRequest request = createTestBooking();

        Map<String, Object> params = new HashMap<>();
        params.put("firstname", request.getFirstname());
        params.put("checkin", LocalDate.now());

        List<BookingIdResponse> bookings = bookingClient.getBookingsWithFilters(params);
        assertThat(bookings)
                .extracting(BookingIdResponse::getBookingid)
                .as("Созданная бронь должна быть в списке")
                .contains(createdBookingId);
    }
}
