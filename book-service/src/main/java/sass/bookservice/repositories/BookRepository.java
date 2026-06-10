package sass.bookservice.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sass.bookservice.models.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {
}
