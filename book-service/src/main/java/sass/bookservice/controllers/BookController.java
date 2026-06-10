package sass.bookservice.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import sass.bookservice.services.BookService;
import sass.bookservice.dtos.BookResponseDto;
import sass.bookservice.dtos.BookRequestDto;

import jakarta.validation.groups.Default;

@RestController
@RequestMapping("/books")
@Tag(name = "Books", description = "Book management endpoints")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(summary = "List books", description = "Returns all books currently stored in the system")
    public ResponseEntity<List<BookResponseDto>> getBooks() {
        List<BookResponseDto> response = bookService.getBooks();
        return ResponseEntity.ok().body(response);
    }

    @PostMapping
    @Operation(summary = "Create a new book")
    public ResponseEntity<BookResponseDto> createPatient(
            @Validated({ Default.class }) @RequestBody BookRequestDto request) {

        BookResponseDto response = bookService.createBook(request);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get current book by id")
    public ResponseEntity<BookResponseDto> getBook(@PathVariable UUID id) {

        BookResponseDto response = bookService.getBook(id);

        return ResponseEntity.ok().body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update current book")
    public ResponseEntity<BookResponseDto> upadateBook(@PathVariable UUID id,
            @Validated({ Default.class }) @RequestBody BookRequestDto request) {

        BookResponseDto response = bookService.updateBook(id, request);

        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
