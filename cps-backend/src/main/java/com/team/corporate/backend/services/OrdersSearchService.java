package com.team.corporate.backend.services;

import com.team.corporate.backend.dto.OrderResponse;
import com.team.corporate.backend.repositories.SpringOrderItemsRepository;
import com.team.corporate.backend.repositories.SpringOrdersRepository;
import com.team.corporate.backend.search.OrderFilter;
import com.team.corporate.backend.search.SearchPaging;
import com.team.corporate.core.entities.Order;
import com.team.corporate.core.exceptions.OrderNotFoundException;
import com.team.corporate.core.repositories.OrdersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrdersSearchService {
    private static final Set<String> SORT_FIELD = Set.of("createdAt", "updatedAt", "status");
    private final SpringOrdersRepository orders;
    private final SpringOrderItemsRepository items;


    public OrdersSearchService(SpringOrdersRepository orders, SpringOrderItemsRepository items) {
        this.orders = orders;
        this.items = items;
    }

    @Transactional
    public Page<OrderResponse> search(OrderFilter filter, int page, int size, Sort sort){
        var pageable = SearchPaging.of(page, size, sort, SORT_FIELD, "id");
        var orderPage = orders.findAll(filter.toSpecification(), pageable);
        var orderIds = orderPage.getContent().stream()
                .map(Order::getId)
                .toList();

        var totals = loadTotals(orderIds);
        return orderPage.map(order -> OrderResponse.of(
                order,
                totals.getOrDefault(order.getId(), BigDecimal.ZERO)
        ));
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id){
        var order = orders.getById(id)
                .orElseThrow(()->new OrderNotFoundException("Заказ не найден."));

        var totals = loadTotals(List.of(id));
        return OrderResponse.of(
                order,
                totals.getOrDefault(id, BigDecimal.ZERO));
    }


    private Map<UUID, BigDecimal> loadTotals(List<UUID> orderIds){
        if(orderIds.isEmpty()){
            return Map.of();
        }

        return items.findTotalSumsByOrderId(orderIds).stream()
                .collect(Collectors.toMap(
                        SpringOrderItemsRepository.OrderTotal::getOrderId,
                        SpringOrderItemsRepository.OrderTotal::getTotalSum
                ));
    }


}
