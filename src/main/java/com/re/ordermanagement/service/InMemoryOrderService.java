package com.re.ordermanagement.service;

import com.re.ordermanagement.model.Order;
import com.re.ordermanagement.model.OrderStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InMemoryOrderService implements OrderService {

    private final AtomicLong seq = new AtomicLong(0);
    private final Map<Long, Order> store = new LinkedHashMap<>();

    public InMemoryOrderService() {
        // Seed data for demo / grading screenshots.
        create(new Order(null, "DH000001", "Nguyễn Văn An", LocalDate.now(), 150000.0, OrderStatus.PENDING, "Giao trước 12h"));
        create(new Order(null, "DH000002", "Trần Thị Bích", LocalDate.now().minusDays(1), 320000.5, OrderStatus.SHIPPING, null));
        create(new Order(null, "DH000003", "Lê Quang Huy", LocalDate.now().minusDays(2), 999999.99, OrderStatus.COMPLETED, "Đã thanh toán"));
        create(new Order(null, "DH000004", "Phạm Minh Châu", LocalDate.now().minusDays(3), 45000.0, OrderStatus.CANCELED, "Khách hủy đơn"));
    }

    @Override
    public synchronized List<Order> findAll() {
        List<Order> list = new ArrayList<>(store.values());
        list.sort(Comparator.comparing(Order::getId));
        return list;
    }

    @Override
    public synchronized List<Order> search(String keyword) {
        String k = keyword == null ? "" : keyword.trim();
        if (k.isEmpty()) return findAll();
        String needle = k.toLowerCase(Locale.ROOT);

        List<Order> out = new ArrayList<>();
        for (Order o : store.values()) {
            if ((o.getOrderCode() != null && o.getOrderCode().toLowerCase(Locale.ROOT).contains(needle)) ||
                    (o.getCustomerName() != null && o.getCustomerName().toLowerCase(Locale.ROOT).contains(needle))) {
                out.add(o);
            }
        }
        out.sort(Comparator.comparing(Order::getId));
        return out;
    }

    @Override
    public synchronized Optional<Order> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public synchronized Order create(Order order) {
        long id = seq.incrementAndGet();
        Order toSave = copy(order);
        toSave.setId(id);
        store.put(id, toSave);
        return copy(toSave);
    }

    @Override
    public synchronized Order update(long id, Order order) {
        if (!store.containsKey(id)) {
            throw new IllegalArgumentException("Order not found: " + id);
        }
        Order toSave = copy(order);
        toSave.setId(id);
        store.put(id, toSave);
        return copy(toSave);
    }

    @Override
    public synchronized void delete(long id) {
        store.remove(id);
    }

    @Override
    public synchronized boolean isOrderCodeUnique(String orderCode, Long ignoreId) {
        if (orderCode == null) return true;
        String code = orderCode.trim().toLowerCase(Locale.ROOT);
        for (Order o : store.values()) {
            if (o.getOrderCode() == null) continue;
            if (o.getOrderCode().trim().toLowerCase(Locale.ROOT).equals(code)) {
                if (ignoreId != null && ignoreId.equals(o.getId())) continue;
                return false;
            }
        }
        return true;
    }

    private static Order copy(Order o) {
        if (o == null) return null;
        return new Order(o.getId(), o.getOrderCode(), o.getCustomerName(), o.getOrderDate(), o.getTotalAmount(), o.getStatus(), o.getNote());
    }
}
