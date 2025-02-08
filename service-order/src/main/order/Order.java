
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Order {
    private static final String QUEUE_NAME = "order_queue";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/order", new OrderHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("OrderService started on port 8000");
    }

    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String item = "item1"; // Hardcoded for simplicity
                try {
                    sendMessageToQueue(item);
                    String response = "Order placed for " + item;
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (Exception e) {
                    exchange.sendResponseHeaders(500, 0);
                    exchange.getResponseBody().close();
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.getResponseBody().close();
            }
        }
    }

    private static void sendMessageToQueue(String message) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            System.out.println("Sent order: " + message);
        }
    }
}
