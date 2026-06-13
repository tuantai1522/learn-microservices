package sass.bookservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import sass.bookservice.dtos.BookResponseDto;
import sass.bookservice.grpc.AnalyticsClient;
import sass.bookservice.models.Book;
import sass.bookservice.models.BookStatus;
import sass.bookservice.repositories.BookRepository;

@ExtendWith(MockitoExtension.class)
class BookServiceTests {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AnalyticsClient analyticsClient;

    @InjectMocks
    private BookService bookService;

    @Captor
    private ArgumentCaptor<Book> savedBookCaptor;

    @Test
    void borrowBookSavesBorrowedBookAndTriggersAnalytics() {
        UUID bookId = UUID.randomUUID();
        Book book = new Book("Distributed Systems", "Patterns");
        book.setId(bookId);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponseDto response = bookService.borrowBook(bookId);

        verify(bookRepository, times(1)).save(savedBookCaptor.capture());
        verify(analyticsClient, times(1)).analyzeBorrow(savedBookCaptor.getValue());

        assertSame(savedBookCaptor.getValue(), book);
        assertEquals(BookStatus.BORROWED, savedBookCaptor.getValue().getBookStatus());
        assertNotNull(response);
        assertEquals(bookId, response.id());
        assertEquals(BookStatus.BORROWED, response.status());
    }
}
