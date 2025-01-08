package ie.atu.reviewservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.*;

class ReviewControllerTests {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @InjectMocks
    private ReviewController reviewController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createReview_ValidData_ShouldReturnCreatedResponse() {

        Review review = new Review(null, "restaurant123", "user456", 5, "Great food!");
        Review savedReview = new Review(1L, "restaurant123", "user456", 5, "Great food!");

        when(restTemplate.getForEntity("http://localhost:8081/api/restaurant/" + review.getRestaurantId(), String.class))
                .thenReturn(new ResponseEntity<>("Restaurant Name", HttpStatus.OK));
        when(restTemplate.getForEntity("http://localhost:8080/api/customer/" + review.getUserId(), String.class))
                .thenReturn(new ResponseEntity<>("User Name", HttpStatus.OK));
        when(reviewRepository.save(review)).thenReturn(savedReview);

        ResponseEntity<String> response = reviewController.createReview(review);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().contains("Review created successfully"));
        verify(rabbitTemplate, times(1)).convertAndSend(AppConfigRabbit.EXCHANGE_NAME, "review.created",
                "New review created for restaurant ID: restaurant123");
    }

    @Test
    void getReviewsByRestaurant_ValidRestaurantId_ShouldReturnReviewResponses() {
        String restaurantId = "restaurant123";
        Review review1 = new Review(1L, "restaurant123", "user456", 5, "Great food!");
        Review review2 = new Review(2L, "restaurant123", "user789", 4, "Good service!");

        when(reviewRepository.findByRestaurantId(restaurantId)).thenReturn(Arrays.asList(review1, review2));
        when(restTemplate.getForObject("http://localhost:8081/api/restaurant/restaurant123", String.class))
                .thenReturn("Restaurant Name");
        when(restTemplate.getForObject("http://localhost:8080/api/customer/user456/username", String.class))
                .thenReturn("User1");
        when(restTemplate.getForObject("http://localhost:8080/api/customer/user789/username", String.class))
                .thenReturn("User2");

        List<ReviewResponse> responses = reviewController.getReviewsByRestaurant(restaurantId);

        assertEquals(2, responses.size());
        assertEquals("User1", responses.get(0).getUserName());
        assertEquals("User2", responses.get(1).getUserName());
    }

    @Test
    void getReviewsByUser_ValidUserId_ShouldReturnReviewResponses() {
        String userId = "user456";
        Review review1 = new Review(1L, "restaurant123", "user456", 5, "Great food!");
        Review review2 = new Review(2L, "restaurant456", "user456", 4, "Good ambiance!");

        when(reviewRepository.findByUserId(userId)).thenReturn(Arrays.asList(review1, review2));
        when(restTemplate.getForObject("http://localhost:8081/api/restaurant/restaurant123", String.class))
                .thenReturn("Restaurant1");
        when(restTemplate.getForObject("http://localhost:8081/api/restaurant/restaurant456", String.class))
                .thenReturn("Restaurant2");

        List<ReviewResponse> responses = reviewController.getReviewsByUser(userId);

        assertEquals(2, responses.size());
        assertEquals("Restaurant1", responses.get(0).getRestaurantName());
        assertEquals("Restaurant2", responses.get(1).getRestaurantName());
    }

    @Test
    void createReview_InvalidRestaurant_ShouldReturnBadRequest() {
        Review review = new Review(null, "invalidRestaurantId", "user456", 5, "Great food!");

        when(restTemplate.getForEntity("http://localhost:8081/api/restaurant/" + review.getRestaurantId(), String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        ResponseEntity<String> response = reviewController.createReview(review);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid restaurantId"));
    }
}
