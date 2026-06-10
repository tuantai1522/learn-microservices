package sass.bookservice.dtos;

import jakarta.validation.constraints.NotBlank;

public record BookRequestDto(
        @NotBlank(message = "Name is required") String name,
        String description) {
}
