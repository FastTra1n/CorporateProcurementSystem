package com.team.corporate.backend.search;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class SearchPaging {
    private SearchPaging() {}

    public static Pageable of(
            int page,
            int size,
            Sort sort,
            Set<String> allowedFields,
            String uniqueField
    ) {
        if(page < 0 ) {
            throw new IllegalArgumentException(
                    "Номер страницы должен быть положительным числом."
            );
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Размер страницы должени быть не менее 1 и не более 100."
            );
        }

        if((long) page * size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Слишком большой номер страницы или размер."
            );
        }

        for (Sort.Order order : sort) {
            if (!allowedFields.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Сортировка по этому полю недоступна."
                );
            }
        }

        Sort stableSort = sort.getOrderFor(uniqueField) == null
                ? sort.and(Sort.by(uniqueField))
                : sort;
        return PageRequest.of(page, size, stableSort);
    }

}
