package ie.atu.reviewservice;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public ReviewController(ReviewRepository reviewRepository, RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        this.reviewRepository = reviewRepository;
        this.restTemplate = restTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createReview(@Valid @RequestBody Review review) {

        // Validate restaurantId using RestTemplate
        ResponseEntity<String> restaurantResponse = restTemplate.getForEntity(
                "http://localhost:8081/api/restaurant/" + review.getRestaurantId(), String.class);

        if (restaurantResponse.getStatusCode() != HttpStatus.OK) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid restaurantId: " + review.getRestaurantId());
        }

        // Validate userId using RestTemplate
        ResponseEntity<String> userResponse = restTemplate.getForEntity(
                "http://localhost:8080/api/customer/" + review.getUserId(), String.class);

        if (userResponse.getStatusCode() != HttpStatus.OK) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid userId: " + review.getUserId());
        }

        // Save the review in the database
        Review savedReview = reviewRepository.save(review);

        // Publish a message to RabbitMQ using RabbitTemplate
        rabbitTemplate.convertAndSend(
                AppConfigRabbit.EXCHANGE_NAME, // Exchange name
                "review.created", // Routing key
                "New review created for restaurant ID: " + review.getRestaurantId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body("Review created successfully with ID: " + savedReview.getId());
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<ReviewResponse> getReviewsByRestaurant(@PathVariable String restaurantId) {
        List<Review> reviews = reviewRepository.findByRestaurantId(restaurantId);

        return reviews.stream().map(review -> {

            String restaurantName = restTemplate.getForObject(
                    "http://localhost:8081/api/restaurant/" + review.getRestaurantId(), String.class
            );

            String userName = restTemplate.getForObject(
                    "http://localhost:8080/api/customer/" + review.getUserId() + "/username", String.class
            );

            return new ReviewResponse(
                    review.getId(),
                    restaurantName,
                    userName,
                    review.getRating(),
                    review.getReviewText()
            );
        }).collect(Collectors.toList());
    }

    @GetMapping("/user/{userId}")
    public List<ReviewResponse> getReviewsByUser(@PathVariable String userId) {
        List<Review> reviews = reviewRepository.findByUserId(userId);

        return reviews.stream().map(review -> {

            String restaurantName = restTemplate.getForObject(
                    "http://localhost:8081/api/restaurant/" + review.getRestaurantId(), String.class
            );

            String userName = restTemplate.getForObject(
                    "http://localhost:8080/api/customer/" + review.getUserId() + "/username", String.class
            );

            return new ReviewResponse(
                    review.getId(),
                    restaurantName,
                    userName,
                    review.getRating(),
                    review.getReviewText()
            );
        }).collect(Collectors.toList());
    }

}
