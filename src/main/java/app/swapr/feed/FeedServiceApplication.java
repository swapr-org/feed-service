package app.swapr.feed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
class FeedServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(FeedServiceApplication.class, args);
  }
}
