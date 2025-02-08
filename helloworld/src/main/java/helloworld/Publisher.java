import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.util.HashMap;
import java.util.Map;

public class Publisher {
    private final static String QUEUE_NAME = "hello";
    private final static String EXCHANGE_NAME = "helloworld";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // Default virtual host
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);

            Map<String, Object> args = new HashMap<>();
            args.put("x-queue-type", "quorum");
            channel.queueDeclare(QUEUE_NAME, true, false, false, args);

            String message = "Ohayou sekai good morning world!";
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            System.out.println(" [x] Sent '" + message + "'");

        }
    }
}