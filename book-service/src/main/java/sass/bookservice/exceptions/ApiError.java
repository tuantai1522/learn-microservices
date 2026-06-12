package sass.bookservice.exceptions;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record ApiError(int status, @NotBlank String message, List<String> validations) {
}
