package com.re.ordermanagement.controller;

import com.re.ordermanagement.model.Order;
import com.re.ordermanagement.model.OrderStatus;
import com.re.ordermanagement.service.OrderService;
import com.re.ordermanagement.web.OrderForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @ModelAttribute("statuses")
    public OrderStatus[] statuses() {
        return OrderStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        List<Order> orders = orderService.search(q);
        model.addAttribute("orders", orders);
        model.addAttribute("q", q == null ? "" : q);
        return "orders/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        OrderForm f = new OrderForm();
        f.setOrderDate(LocalDate.now());
        f.setStatus(OrderStatus.PENDING);
        model.addAttribute("orderForm", f);
        model.addAttribute("mode", "create");
        return "orders/form";
    }

    @PostMapping
    public String create(@ModelAttribute("orderForm") OrderForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        validate(form, bindingResult, null);

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "orders/form";
        }

        orderService.create(toOrder(null, form));
        redirectAttributes.addFlashAttribute("successMessage", "Thêm đơn hàng thành công.");
        return "redirect:/orders";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Optional<Order> opt = orderService.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng.");
            return "redirect:/orders";
        }
        model.addAttribute("orderForm", toForm(opt.get()));
        model.addAttribute("mode", "edit");
        model.addAttribute("orderId", id);
        return "orders/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") long id,
                         @ModelAttribute("orderForm") OrderForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        validate(form, bindingResult, id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("orderId", id);
            return "orders/form";
        }

        try {
            orderService.update(id, toOrder(id, form));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng.");
            return "redirect:/orders";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật đơn hàng thành công.");
        return "redirect:/orders";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        orderService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa đơn hàng thành công.");
        return "redirect:/orders";
    }

    private static Order toOrder(Long id, OrderForm f) {
        return new Order(id, f.getOrderCode(), f.getCustomerName(), f.getOrderDate(), f.getTotalAmount(), f.getStatus(), emptyToNull(f.getNote()));
    }

    private static OrderForm toForm(Order o) {
        OrderForm f = new OrderForm();
        f.setOrderCode(o.getOrderCode());
        f.setCustomerName(o.getCustomerName());
        f.setOrderDate(o.getOrderDate());
        f.setTotalAmount(o.getTotalAmount());
        f.setStatus(o.getStatus());
        f.setNote(o.getNote());
        return f;
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void validate(OrderForm form, BindingResult br, Long ignoreId) {
        // orderCode
        String code = form.getOrderCode() == null ? "" : form.getOrderCode().trim();
        if (code.isEmpty()) {
            br.rejectValue("orderCode", "required", "Mã đơn hàng là bắt buộc.");
        } else {
            if (code.length() < 6 || code.length() > 20) {
                br.rejectValue("orderCode", "length", "Mã đơn hàng phải từ 6 đến 20 ký tự.");
            } else if (!ORDER_CODE_PATTERN.matcher(code).matches()) {
                br.rejectValue("orderCode", "pattern", "Mã đơn hàng chỉ gồm chữ và số.");
            } else if (!orderService.isOrderCodeUnique(code, ignoreId)) {
                br.rejectValue("orderCode", "duplicate", "Mã đơn hàng đã tồn tại.");
            }
        }
        form.setOrderCode(code);

        // customerName
        String customerName = form.getCustomerName() == null ? "" : form.getCustomerName().trim();
        if (customerName.isEmpty()) {
            br.rejectValue("customerName", "required", "Tên khách hàng là bắt buộc.");
        } else if (customerName.length() < 5 || customerName.length() > 100) {
            br.rejectValue("customerName", "length", "Tên khách hàng phải từ 5 đến 100 ký tự.");
        }
        form.setCustomerName(customerName);

        // orderDate
        if (form.getOrderDate() == null) {
            br.rejectValue("orderDate", "required", "Ngày đặt là bắt buộc.");
        } else if (form.getOrderDate().isAfter(LocalDate.now())) {
            br.rejectValue("orderDate", "max", "Ngày đặt không được lớn hơn ngày hiện tại.");
        }

        // totalAmount
        Double amount = form.getTotalAmount();
        if (amount == null) {
            br.rejectValue("totalAmount", "required", "Tổng tiền là bắt buộc.");
        } else {
            if (amount <= 0) {
                br.rejectValue("totalAmount", "min", "Tổng tiền phải lớn hơn 0.");
            } else if (amount > 1_000_000_000d) {
                br.rejectValue("totalAmount", "max", "Tổng tiền phải nhỏ hơn hoặc bằng 1.000.000.000 VNĐ.");
            } else if (!hasAtMost2Decimals(amount)) {
                br.rejectValue("totalAmount", "scale", "Tổng tiền chỉ được có tối đa 2 chữ số thập phân.");
            }
        }

        // status
        if (form.getStatus() == null) {
            br.rejectValue("status", "required", "Trạng thái là bắt buộc.");
        }

        // note
        String note = form.getNote();
        if (note != null) {
            String trimmed = note.trim();
            if (trimmed.length() > 300) {
                br.rejectValue("note", "length", "Ghi chú tối đa 300 ký tự.");
            }
            form.setNote(trimmed);
        }
    }

    private static boolean hasAtMost2Decimals(Double v) {
        double scaled = v * 100.0;
        double rounded = Math.rint(scaled);
        return Math.abs(scaled - rounded) < 1e-9;
    }
}
