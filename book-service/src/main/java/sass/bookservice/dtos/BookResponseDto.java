package sass.bookservice.dtos;

import java.util.UUID;
import sass.bookservice.models.BookStatus;

public record BookResponseDto(UUID id, String name, String description, BookStatus status) {
}
