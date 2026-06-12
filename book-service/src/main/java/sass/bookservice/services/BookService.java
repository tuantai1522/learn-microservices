package sass.bookservice.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import sass.bookservice.dtos.BookResponseDto;
import sass.bookservice.exceptions.BookNotFoundException;
import sass.bookservice.mappers.BookMapper;
import sass.bookservice.models.Book;
import sass.bookservice.repositories.BookRepository;
import sass.bookservice.dtos.BookRequestDto;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<BookResponseDto> getBooks() {
        List<Book> books = bookRepository.findAll();

        return books.stream().map(BookMapper::toDto).toList();
    }

    public BookResponseDto createBook(BookRequestDto request) {
        Book newBook = bookRepository.save(new Book(request.name(), request.description()));

        return BookMapper.toDto(newBook);
    }

    public BookResponseDto getBook(UUID id) {
        Book book = bookRepository.findById(id).orElseThrow(
                () -> new BookNotFoundException("Book not found with ID: " + id));

        return BookMapper.toDto(book);
    }

    public BookResponseDto updateBook(UUID id, BookRequestDto request) {
        Book book = bookRepository.findById(id).orElseThrow(
                () -> new BookNotFoundException("Book not found with ID: " + id));

        book.setName(request.name());
        book.setDescription(request.description());

        Book updatedBook = bookRepository.save(book);
        return BookMapper.toDto(updatedBook);
    }

    public void deleteBook(UUID id) {
        boolean verifyExist = bookRepository.existsById(id);

        if (verifyExist) {
            bookRepository.deleteById(id);
        } else {
            throw new BookNotFoundException("Book not found with ID: " + id);
        }
    }
}
