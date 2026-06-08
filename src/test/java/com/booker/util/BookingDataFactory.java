package com.booker.util;

import com.booker.model.BookingDates;
import com.booker.model.request.BookingRequest;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.Locale;

public class BookingDataFactory {
    private static final Faker faker = new Faker(Locale.ENGLISH);

    public static BookingRequest.BookingRequestBuilder defaultBooking() {
        LocalDate checkin = LocalDate.now().plusDays(1);
        LocalDate checkout = checkin.plusDays(7);
        return BookingRequest.builder()
                .firstname(randomFirstName())
                .lastname(randomLastName())
                .totalprice(randomPrice())
                .depositpaid(randomBoolean())
                .bookingdates(new BookingDates(checkin, checkout))
                .additionalneeds(randomAdditionalNeeds());
    }

    public static String randomFirstName() {
        return faker.name().firstName();
    }

    public static String randomLastName() {
        return faker.name().lastName();
    }

    public static int randomPrice() {
        return faker.number().numberBetween(50, 500);
    }

    public static boolean randomBoolean() {
        return faker.bool().bool();
    }

    public static String randomAdditionalNeeds() {
        return faker.food().dish();
    }
}
