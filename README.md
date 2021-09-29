#기능적 요구사항
1. 마케팅 팀에서 체험 서비스를 기획한다.
2. 고객이 상품을 선택하여 체험 신청을 한다.
3. 체험 신청한 상품을 배송한다.
4. 고객은 체험 신청을 취소할 수 있다.
5. 체험 신청을 취소하면 배송이 취소된다.
6. 고객은 체험 신청 상태를 중간중간 조회한다.
7. 체험 기간이 종료되면 상품을 반납 요청한다.
8. 고객이 상품을 반납 신청한다. (사용 중 반납 포함)
9. 반납이 신청되면 상품을 수거한다.

#비기능적 요구사항
1. 트랜잭션
    1. 체험 신청이 취소된 건은 배송되지 않아야 한다.
    2. 체험 신청이 종료된 건은 배송취소 요청이 되어야 한다.
2. 장애격리
    1. 대리점 영업시간이 아니더라도 체험 신청은 365일 24시간 가능해야 한다(온라인, 스토어 진행)  Event-driven, Eventual Consistency
    2. 구독 신청 요청이 과중되면 사용자를 잠시동안 받지 않고 잠시 후에 진행되도록 유도한다  Circuit breaker
3. 성능
    1. 체험 서비스가 종료 또는 취소되면 예정된 배송은 즉시 중지한다.  Event driven

# 체크포인트
- 분석 설계
  - 이벤트스토밍은 서비스 핵심 {이벤트}로 시작
    - {서비스시작}, {서비스종료}, {주문요청}, {주문취소}, {반납요청}, {배송시작}, {배송취소}, {배송완료}
  - 이후 재고와 관련된 이벤트를 추가
    - {서비스시작}, {서비스종료}, {주문요청}, {주문취소}, {반납요청}, {배송시작}, {배송취소}, {배송완료}, {재고변동}
  ![image](https://user-images.githubusercontent.com/10860105/133181206-fcbaae47-790d-41d8-a395-e45dedca47ec.jpg)
  
  - 이벤트에 대응하는 <액터>와 [커맨드] 추가
    - <마케팅담당자>가
      - [서비스 시작하기]로 {서비스시작}
      - [서비스 종료하기]로 {서비스종료}
    - <고객>이
      - [주문 요청하기]로 {주문요청}
      - [주문 취소하기]로 {주문취소}
      - [반납하기]로 {반납요청}
    - {배송시작}, {배송취소}, {배송완료}, {재고변동}은 커맨드 없음

  - 어그리게잇으로 묶은 후 바운디드 컨텍스트로 구성
    - 주문(Order): Core Domain
      - <고객>이 [주문 요청하기]로 {주문요청}, [주문 취소하기]로 {주문취소}, [반납하기]로 {반납요청}
    - 마케팅(Marketing): Core Domain
      - <마케팅담당자>가 [서비스 시작하기]로 {서비스시작}, [서비스 종료하기]로 {서비스종료}
    - 배송(Delivery): Core Domain
      - {배송시작}, {배송취소}, {배송완료}
    - 창고(Warehouse): Supporting Domain
      - {재고변동} * hsql 적용
    - 조회(Dashboard): Supporting Domain

  - (폴리시)와 컨텍스트 매핑 후 완성된 모형
  ![image](https://user-images.githubusercontent.com/10860105/133181645-11c9be2b-2a38-47a7-b0f0-2cdf59e630a7.jpg)

  - 요구사항 검증
    1. 마케팅 팀에서 체험 서비스를 기획한다.: <마케팅담당자>가 [서비스 시작하기]로 {서비스시작} ok
    2. 고객이 상품을 선택하여 체험 신청을 한다.: <고객>이 [주문 요청하기]로 {주문요청} ok
    3. 체험 신청한 상품을 배송한다.: <고객>이 [주문 요청하기]로 {주문요청} >Pub/Sub> (배송요청) ok
    4. 고객은 체험 신청을 취소할 수 있다.: <고객>이 [주문 취소하기]로 {주문취소} ok
    5. 체험 신청을 취소하면 배송이 취소된다.: <고객>이 [주문 취소하기]로 {주문취소} >Pub/Sub> (배송취소) ok
    6. 고객은 체험 신청 상태를 중간중간 조회한다.: 조회(Dashboard) ok
    7. 체험 기간이 종료되면 상품을 반납 요청한다.: <마케팅담당자>가 [서비스 종료하기]로 {서비스종료} >Req/Res> (배송취소) ok
    8. 고객이 상품을 반납 신청한다. (사용 중 반납 포함): <고객>이 [반납하기]로 {반납요청}
    9. 반납이 신청되면 상품을 수거한다.: <고객>이 [반납하기]로 {반납요청} >Pub/Sub> (픽업요청)을 받음 ok
    10. 체험 신청이 취소된 건은 배송되지 않아야 한다.: <고객>이 [주문 취소하기]로 {주문취소} >Pub/Sub> (배송취소) ok
    11. 체험 신청이 종료된 건은 배송취소 요청이 되어야 한다.: <마케팅담당자>가 [서비스 종료하기]로 {서비스종료} >Req/Res> (배송취소) ok


- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
      - 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 order 마이크로 서비스)
```java
package com.grp04.togosvc.order;

import java.util.Date;
import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Order_table")
public class Order {

    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;    
    private Long productId;
    private Long customerId;
    private String address;
    private Long orderQty;
    private Long planId;
    private Date returnDate;
    private String orderStatus;
    ...
```
  -
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
      - 설계에서 작성한 용어 그대 사용
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
      - Marketing의 종료 발새 시 Order 취소를 요청하는 부분에 FeignClient 방식 적용
```java
@FeignClient(name="Order", url="http://order:8080")
//@FeignClient(name="order", url="http://localhost:8082")
public interface OrderService {
    @RequestMapping(method= RequestMethod.GET, path="/orders/search/removeByPlanId?planId={id}")
    public void returnToGo(@PathVariable long id);
}
```
  -
      - Order에서는 취소된 Plan과 연관된 order 삭제 - Repository 활용
```java
package com.grp04.togosvc.order;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

public interface OrderRepository extends PagingAndSortingRepository<Order, Long>{
    @Transactional
    List<Order> removeByPlanId(Long planId);
}
```
- 
  -
    - 서킷브레이커를 통하여 장애를 격리시킬 수 있는가?
      - istio 적용: order, marketing 서비스에 적용
```    
NAME                                       READY   STATUS    RESTARTS   AGE   IP            NODE                                NOMINATED NODE   READINESS GATES
pod/istio-egressgateway-594c95cf64-bhhc5   1/1     Running   0          21h   10.244.3.12   aks-agentpool-91619433-vmss000000   <none>           <none>
pod/istio-ingressgateway-cff6cf59f-f84m7   1/1     Running   0          21h   10.244.3.11   aks-agentpool-91619433-vmss000000   <none>           <none>
pod/istiod-d75959968-vsjfp                 1/1     Running   0          21h   10.244.3.10   aks-agentpool-91619433-vmss000000   <none>           <none>

NAME                           TYPE           CLUSTER-IP     EXTERNAL-IP     PORT(S)                                                                      AGE   SELECTOR
service/istio-egressgateway    ClusterIP      10.0.255.35    <none>          80/TCP,443/TCP,15443/TCP                                                     21h   app=istio-egressgateway,istio=egressgateway
service/istio-ingressgateway   LoadBalancer   10.0.241.153   52.147.122.80   15021:30922/TCP,80:32677/TCP,443:31237/TCP,31400:30726/TCP,15443:30120/TCP   21h   app=istio-ingressgateway,istio=ingressgateway
service/istiod                 ClusterIP      10.0.110.77    <none>          15010/TCP,15012/TCP,443/TCP,15014/TCP,853/TCP                                21h   app=istiod,istio=pilot

NAME                                   READY   UP-TO-DATE   AVAILABLE   AGE   CONTAINERS    IMAGES                          SELECTOR
deployment.apps/istio-egressgateway    1/1     1            1           21h   istio-proxy   docker.io/istio/proxyv2:1.7.1   app=istio-egressgateway,istio=egressgateway
deployment.apps/istio-ingressgateway   1/1     1            1           21h   istio-proxy   docker.io/istio/proxyv2:1.7.1   app=istio-ingressgateway,istio=ingressgateway
deployment.apps/istiod                 1/1     1            1           21h   discovery     docker.io/istio/pilot:1.7.1     istio=pilot

NAME                                             DESIRED   CURRENT   READY   AGE   CONTAINERS    IMAGES                          SELECTOR
replicaset.apps/istio-egressgateway-594c95cf64   1         1         1       21h   istio-proxy   docker.io/istio/proxyv2:1.7.1   app=istio-egressgateway,istio=egressgateway,pod-template-hash=594c95cf64
replicaset.apps/istio-ingressgateway-cff6cf59f   1         1         1       21h   istio-proxy   docker.io/istio/proxyv2:1.7.1   app=istio-ingressgateway,istio=ingressgateway,pod-template-hash=cff6cf59f
replicaset.apps/istiod-d75959968                 1         1         1       21h   discovery     docker.io/istio/pilot:1.7.1     istio=pilot,pod-template-hash=d75959968
```
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
      - 각 폴리시는 명시적으로 이벤트명과 연결
```java
@StreamListener(KafkaProcessor.INPUT)
public void wheneverDeliveryStarted_ChangeOrderStatus(@Payload DeliveryStarted deliveryStarted){
```
  -
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
      - delivery 서비스 중지 후 order 요청 > 이후 delivery 서비스 시작 시 메시지 수신 확인
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
      - replica 추가 후 정상 동작 확인
```
delivery-5568445d94-v9gd6    1/1     Running   0          26h
gateway-68c7c679c4-99hst     1/1     Running   0          26h
marketing-55f496c8dd-zg89j   2/2     Running   0          21h
order-5686546c84-767g8       2/2     Running   0          21h
warehouse-8694d6c547-jxt8r   1/1     Running   0          26h
```
  -
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?
      - Order 서비스에서 delivery의 상태 변경에 따라 orderStatus를 업데이트 함으로써 delivery정보를 delivery의 데이터 원본에 접근하지 않게 구현
```
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryStarted_ChangeOrderStatus(@Payload DeliveryStarted deliveryStarted){
        if(!deliveryStarted.validate()) return;
        Order order = orderRepository.findById(deliveryStarted.getOrderId()).get();
        order.setOrderStatus("Delivery Started.");

        orderRepository.save(order);
    }
```
  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
      - java로 구현, 테스트 과정에서 서로 다른 java 버전 구성
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
      - Warehouse 서비스는 hSQL 사용
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가? gateway 서비스 적용
      - http://52.231.216.252/warehouses
      - http://52.231.216.252/orders
      - http://52.231.216.252/marketings
      - http://52.231.216.252/deliveries
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?

- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
      - liveness, readiness probe 적용
      - siege 테스트로 로컬의 minikube에서는 확인, Azure에서느 영향이 적읍
```
HTTP/1.1 400     0.05 secs:     588 bytes ==> POST http://52.231.216.252/orders
HTTP/1.1 400     0.28 secs:     588 bytes ==> POST http://52.231.216.252/orders

Lifting the server siege...
Transactions:		       23560 hits
Availability:		      100.00 %
Elapsed time:		       59.28 secs
Data transferred:	       13.21 MB
Response time:		        0.25 secs
Transaction rate:	      397.44 trans/sec
Throughput:		        0.22 MB/sec
Concurrency:		       99.70
Successful transactions:           0
Failed transactions:	           0
Longest transaction:	        0.97
Shortest transaction:	        0.00
```
-
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
      - istio 서킷브레이커 적용
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명
      - Azure DevOps 플랫폼 사용, 자동 배포 확인 및 pod 수명으로 무정지 배포 확인 완료
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?
