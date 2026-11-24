package app.swapr.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
    "INTERNAL_TOKEN=test-internal-token",
    "SUPABASE_URL=https://test-project.supabase.co",
    "CORS_ALLOWED_ORIGINS=https://qa.swapr.test"
})
@ActiveProfiles("qa")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class FeedServiceApplicationTest {

  private final HealthEndpoint healthEndpoint;

  FeedServiceApplicationTest(HealthEndpoint healthEndpoint) {
    this.healthEndpoint = healthEndpoint;
  }

  @Test
  void reportsHealthyWithoutDatabaseComponent() {
    HealthDescriptor result = healthEndpoint.health();

    assertThat(result.getStatus()).isEqualTo(Status.UP);
    assertThat(((CompositeHealthDescriptor) result).getComponents()).doesNotContainKey("db");
  }

  @Test
  void reportsLive() {
    HealthDescriptor liveness = healthEndpoint.healthForPath("liveness");

    assertThat(liveness.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void failsToStartWhenInternalTokenIsEmpty() {
    SpringApplicationBuilder app = new SpringApplicationBuilder(FeedServiceApplication.class)
        .profiles("qa")
        .properties(
            "server.port=0",
            "INTERNAL_TOKEN=",
            "SUPABASE_URL=https://test-project.supabase.co",
            "CORS_ALLOWED_ORIGINS=https://qa.swapr.test");

    assertThatThrownBy(app::run).rootCause().hasMessageContaining("INTERNAL_TOKEN");
  }
}
