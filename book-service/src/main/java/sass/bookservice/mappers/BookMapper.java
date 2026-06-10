package sass.bookservice.mappers;

import sass.bookservice.dtos.BookResponseDto;
import sass.bookservice.models.Book;

public class BookMapper {
    public static BookResponseDto toDto(Book book) {
        return new BookResponseDto(book.getId(), book.getName(), book.getDescription(), book.getBookStatus());
    }
}
