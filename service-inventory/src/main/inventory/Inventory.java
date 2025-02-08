import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.rabbitmq.client.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Inventory {
    private static final String QUEUE_NAME = "order_queue";
    private static Map<String, Integer> inventory = new HashMap<>();

    public static void main(String[] args) throws Exception {
        inventory.put("item1", 10);
        inventory.put("item2", 5);

        HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);
        server.createContext("/inventory", new InventoryHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Inventory Service started on port 8001");

        new Thread(Inventory::consumeOrders).start();
    }

    static class InventoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = inventory.toString();
                exchange.sendResponseHeaders(200, response.length());
                OutputStream output = exchange.getResponseBody();
                output.write(response.getBytes());
                output.close();
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.getResponseBody().close();
            }
        }
    }

    private static void consumeOrders() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String item = new String(delivery.getBody(), "UTF-8");
                if (inventory.containsKey(item) && inventory.get(item) > 0) {
                    inventory.put(item, inventory.get(item) - 1);
                    System.out.println("Stock updated for: " + item);
                } else {
                    System.out.println("Out of stock: " + item);
                }
            };
            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}