package com.booker.controllers;

import com.booker.BaseTest;
import com.booker.model.BookingDates;
import com.booker.model.request.BookingRequest;
import com.booker.model.response.BookingIdResponse;
import com.booker.model.response.BookingResponse;
import com.booker.model.response.CreateBookingResponse;
import com.booker.util.BookingDataFactory;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
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
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Epic("Restful Booker API")
@Feature("Booking")
class BookingControllerTest extends BaseTest {

    private static final int NON_EXISTENT_ID = (int) (System.currentTimeMillis() / 1000);

    private int createdBookingId = 0;

    private BookingRequest createTestBooking() {
        BookingRequest request = BookingDataFactory.defaultBooking().build();
        createdBookingId = bookingClient.createBooking(request).getBookingid();
        return request;
    }

    @AfterEach
    void tearDown() {
        if (createdBookingId != 0) {
            authBookingClient.deleteBooking(createdBookingId);
            createdBookingId = 0;
        }
    }


    // POST /booking
    @Test
    @Tag("positive")
    @Story("Создание брони")
    @DisplayName("POST /booking — успешное создание, данные в ответе совпадают с запросом")
    void shouldCreateBookingWithAllFields() {
        BookingRequest request = BookingDataFactory.defaultBooking().build();

        Response rawResponse = bookingClient.createBookingRaw(request);
        assertThat(rawResponse.statusCode()).isEqualTo(HttpStatus.SC_OK);
        rawResponse.then().body(matchesJsonSchemaInClasspath("schemas/create-booking-schema.json"));

        CreateBookingResponse response = rawResponse.as(CreateBookingResponse.class);
        createdBookingId = response.getBookingid();

        assertSoftly(softly -> {
            softly.assertThat(response.getBooking().getFirstname()).as("Имя").isEqualTo(request.getFirstname());
            softly.assertThat(response.getBooking().getLastname()).as("Фамилия").isEqualTo(request.getLastname());
            softly.assertThat(response.getBooking().getTotalprice()).as("Цена").isEqualTo(request.getTotalprice());
            softly.assertThat(response.getBooking().getDepositpaid()).as("Депозит").isEqualTo(request.getDepositpaid());
            softly.assertThat(response.getBooking().getBookingdates().getCheckin()).as("Дата заезда").isEqualTo(request.getBookingdates().getCheckin());
            softly.assertThat(response.getBooking().getBookingdates().getCheckout()).as("Дата выезда").isEqualTo(request.getBookingdates().getCheckout());
            softly.assertThat(response.getBooking().getAdditionalneeds()).as("Доп. пожелания").isEqualTo(request.getAdditionalneeds());
        });

        Response getResponse = bookingClient.getBookingRaw(createdBookingId);
        assertThat(getResponse.statusCode())
                .as("Бронь доступна по GET после создания")
                .isEqualTo(HttpStatus.SC_OK);
    }

    @Disabled("Баг API: возвращает 500 вместо 400 при отсутствии обязательного поля firstname")
    @Test
    @Tag("negative")
    @Story("Создание брони")
    @DisplayName("POST /booking — отсутствует обязательное поле firstname, ожидается 400")
    void shouldReturn400WhenFirstnameMissing() {
        BookingRequest request = BookingDataFactory.defaultBooking()
                .firstname(null)
                .build();

        Response response = bookingClient.createBookingRaw(request);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }


    // GET /booking
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


    // GET /booking/{id}
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

    @Disabled("Баг API: возвращает 404 вместо 400 при невалидном формате ID")
    @Test
    @Tag("negative")
    @Story("Получение брони по ID")
    @DisplayName("GET /booking/{id} — невалидный формат ID (строка), ожидается 400")
    void shouldReturn400ForInvalidIdFormat() {
        Response response = bookingClient.getBookingRaw("abc");
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }


    // PUT /booking/{id}
    @Test
    @Tag("positive")
    @Story("Обновление брони")
    @DisplayName("PUT /booking/{id} — полное обновление с токеном")
    void shouldUpdateBookingWithToken() {
        createTestBooking();

        BookingRequest updateRequest = BookingDataFactory.defaultBooking()
                .firstname(BookingDataFactory.randomFirstName())
                .totalprice(BookingDataFactory.randomPrice())
                .build();

        Response response = authBookingClient.updateBooking(createdBookingId, updateRequest);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
        response.then().body(matchesJsonSchemaInClasspath("schemas/booking-schema.json"));

        BookingResponse updated = response.as(BookingResponse.class);
        assertSoftly(softly -> {
            softly.assertThat(updated.getFirstname()).as("Имя после обновления").isEqualTo(updateRequest.getFirstname());
            softly.assertThat(updated.getTotalprice()).as("Цена после обновления").isEqualTo(updateRequest.getTotalprice());
        });
    }

    @Test
    @Tag("positive")
    @Story("Обновление брони")
    @DisplayName("PUT /booking/{id} — полное обновление с Basic Auth")
    void shouldUpdateBookingWithBasicAuth() {
        createTestBooking();

        BookingRequest updateRequest = BookingDataFactory.defaultBooking()
                .firstname(BookingDataFactory.randomFirstName())
                .totalprice(BookingDataFactory.randomPrice())
                .build();

        Response response = basicAuthBookingClient.updateBooking(createdBookingId, updateRequest);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
        response.then().body(matchesJsonSchemaInClasspath("schemas/booking-schema.json"));

        BookingResponse updated = response.as(BookingResponse.class);
        assertSoftly(softly -> {
            softly.assertThat(updated.getFirstname()).as("Имя").isEqualTo(updateRequest.getFirstname());
            softly.assertThat(updated.getTotalprice()).as("Цена").isEqualTo(updateRequest.getTotalprice());
        });
    }

    @Test
    @Tag("negative")
    @Story("Обновление брони")
    @DisplayName("PUT /booking/{id} — без авторизации возвращает 403")
    void shouldNotUpdateBookingWithoutToken() {
        BookingRequest request = createTestBooking();

        Response response = bookingClient.updateBooking(createdBookingId, request);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @Tag("negative")
    @Story("Обновление брони")
    @DisplayName("PUT /booking/{id} — неполный payload, ожидается 400")
    void shouldNotUpdateBookingWithPartialPayload() {
        createTestBooking();

        Map<String, Object> partialPayload = new HashMap<>();
        partialPayload.put("firstname", BookingDataFactory.randomFirstName());

        Response response = authBookingClient.updateBooking(createdBookingId, partialPayload);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }


    // PATCH /booking/{id}
    @Test
    @Tag("positive")
    @Story("Частичное обновление")
    @DisplayName("PATCH /booking/{id} — обновлённое поле изменилось, остальные не тронуты")
    void shouldPartiallyUpdateBooking() {
        BookingRequest request = createTestBooking();

        int newPrice = BookingDataFactory.randomPrice();
        Map<String, Object> partialUpdate = new HashMap<>();
        partialUpdate.put("totalprice", newPrice);

        Response response = authBookingClient.partialUpdateBooking(createdBookingId, partialUpdate);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
        response.then().body(matchesJsonSchemaInClasspath("schemas/booking-schema.json"));

        BookingResponse updated = response.as(BookingResponse.class);
        assertSoftly(softly -> {
            softly.assertThat(updated.getTotalprice()).as("Цена обновилась").isEqualTo(newPrice);
            softly.assertThat(updated.getFirstname()).as("Имя не изменилось").isEqualTo(request.getFirstname());
            softly.assertThat(updated.getLastname()).as("Фамилия не изменилась").isEqualTo(request.getLastname());
        });
    }

    @Test
    @Tag("negative")
    @Story("Частичное обновление")
    @DisplayName("PATCH /booking/{id} — без авторизации возвращает 403")
    void shouldNotPartiallyUpdateBookingWithoutToken() {
        createTestBooking();

        Map<String, Object> partialUpdate = new HashMap<>();
        partialUpdate.put("totalprice", BookingDataFactory.randomPrice());

        Response response = bookingClient.partialUpdateBooking(createdBookingId, partialUpdate);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
    }


    // DELETE /booking/{id}
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

    @Disabled("Баг API: вместо 404 возвращает 405")
    @Test
    @Tag("negative")
    @Story("Удаление брони")
    @DisplayName("DELETE /booking/{id} — несуществующий ID, ожидается 404")
    void shouldNotDeleteNonExistentBooking() {
        Response response = authBookingClient.deleteBooking(NON_EXISTENT_ID);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
    }
}