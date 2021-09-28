package com.grp04.togosvc.order;

import java.util.List;

import com.grp04.togosvc.order.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler {
    private OrderRepository orderRepository;
    
    @Autowired
    public PolicyHandler(OrderRepository orderRepository){
        this.orderRepository = orderRepository;
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryStarted_ChangeOrderStatus(@Payload DeliveryStarted deliveryStarted){

        if(!deliveryStarted.validate()) return;

        System.out.println("\n\n##### listener ChangeOrderStatus : " + deliveryStarted.toJson() + "\n\n");

        Order order = orderRepository.findById(deliveryStarted.getOrderId()).get();
        order.setOrderStatus("Delivery Started.");

        orderRepository.save(order);

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryCanceled_ChangeOrderStatus(@Payload DeliveryCanceled deliveryCanceled){

        if(!deliveryCanceled.validate()) return;

        System.out.println("\n\n##### listener ChangeOrderStatus : " + deliveryCanceled.toJson() + "\n\n");

        Order order = orderRepository.findById(deliveryCanceled.getOrderId()).get();
        order.setOrderStatus("Delivery Canceled.");
        orderRepository.save(order);

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}
}