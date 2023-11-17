package syua.blog.ldudpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import syua.blog.ldudpserver.dto.ControlRequest;
import syua.blog.ldudpserver.dto.ControlSuccessResponse;
import syua.blog.ldudpserver.dto.ControlType;
import syua.blog.ldudpserver.dto.Protocol;

@Slf4j
@Component
public class UdpServer {

	@Value("${loadbalancer.ip}")
	private String lbIpAddr;

	@Value("${loadbalancer.port}")
	private int lbPort;

	@Value("${server.port}")
	private int port;

	@Value("${udp.server.name}")
	private String name;

	private final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private final ObjectMapper objectMapper = new ObjectMapper();

	@PostConstruct
	public void init() throws IOException {
		new HealthCheckRequestHandler(5555).startHandle();
		doControlRequest(ControlType.REGISTER);
		startHandle();
	}

	public void startHandle() {
		new Thread(() -> {
			try (DatagramSocket serverSocket = new DatagramSocket(port)) {
				while (true) {
					DatagramPacket clientPacket = SocketReadUtils.readUdpAllBytes(serverSocket);
					threadPool.execute(() -> handleRequest(serverSocket, clientPacket.getData(),
						clientPacket.getAddress(), clientPacket.getPort()));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public void handleRequest(DatagramSocket socket, byte[] clientData, InetAddress clientAddr, int clientPort) {
		try {
			byte[] responseData = null;
			if (new String(clientData, StandardCharsets.UTF_8).equals("register")) {
				doControlRequest(ControlType.REGISTER);
				responseData = "success register".getBytes(StandardCharsets.UTF_8);
			} else if (new String(clientData, StandardCharsets.UTF_8).equals("unregister")) {
				doControlRequest(ControlType.UNREGISTER);
				responseData = "success unregister".getBytes(StandardCharsets.UTF_8);
			} else {
				responseData = ("[" + InetAddress.getLocalHost().getHostAddress() + " " + port + "] " + name + "\n")
					.getBytes(StandardCharsets.UTF_8);
			}
			DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length,
				clientAddr, clientPort);
			socket.send(responsePacket);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	private void doControlRequest(ControlType type) throws IOException {
		try (Socket socket = new Socket(InetAddress.getByName(lbIpAddr), lbPort)) {
			try (InputStream inputStream = socket.getInputStream();
				 OutputStream outputStream = socket.getOutputStream()) {
				outputStream.write(objectMapper.writeValueAsBytes(
					new ControlRequest(type, Protocol.UDP, port)));
				outputStream.flush();
				socket.shutdownOutput();
				byte[] bytes = inputStream.readAllBytes();
				log.info(new String(bytes, StandardCharsets.UTF_8));
				objectMapper.readValue(bytes, ControlSuccessResponse.class);
			}
		}
	}

}
