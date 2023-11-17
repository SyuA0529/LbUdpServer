package syua.blog.ldudpserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import syua.blog.ldudpserver.dto.HealthCheckRequest;
import syua.blog.ldudpserver.dto.HealthCheckResponse;
import syua.blog.ldudpserver.dto.Protocol;

@Slf4j
public class HealthCheckRequestHandler {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ExecutorService threadPool;
	private final int port;

	public HealthCheckRequestHandler(int port) {
		this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		this.port = port;
	}

	public void startHandle() {
		new Thread(() -> {
			try (DatagramSocket serverSocket = new DatagramSocket(port)) {
				while (true) {
					log.info("ready to health check - {}", port);
					DatagramPacket clientPacket = SocketReadUtils.readUdpAllBytes(serverSocket);
					threadPool.execute(() -> handleRequest(serverSocket, clientPacket));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void handleRequest(DatagramSocket socket, DatagramPacket clientPacket) {
		try {
			log.info("receive health check");
			HealthCheckRequest request = objectMapper.readValue(clientPacket.getData(), HealthCheckRequest.class);
			log.info("Health Check {}", request);
			HealthCheckResponse response = new HealthCheckResponse();
			response.setHealthy();
			DatagramPacket responsePacket = new DatagramPacket(new byte[Protocol.UDP.getMaxReceiveSize()],
				Protocol.UDP.getMaxReceiveSize(), clientPacket.getAddress(), clientPacket.getPort());
			socket.send(responsePacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
