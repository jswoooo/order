package com.grp04.togosvc.order;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

public interface OrderRepository extends PagingAndSortingRepository<Order, Long>{
    @Transactional
    List<Order> removeByPlanId(Long planId);
}
