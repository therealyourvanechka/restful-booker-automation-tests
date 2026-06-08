package com.booker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDates {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkin;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkout;
}
