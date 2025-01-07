package ie.atu.reviewservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRestaurantId(String restaurantId);
    List<Review> findByUserId(String userId);
}

