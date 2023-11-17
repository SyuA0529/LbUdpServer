package syua.blog.ldudpserver.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class HealthCheckRequest {

	@Getter
	private static final HealthCheckRequest instance = new HealthCheckRequest();

	private final String cmd = "hello";

}
