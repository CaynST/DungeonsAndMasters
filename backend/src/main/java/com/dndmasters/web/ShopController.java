package com.dndmasters.web;

import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.economy.ShopService;
import com.dndmasters.security.SecurityUtils;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaigns/{id}/shop")
public class ShopController {

    private final ShopService shop;

    public ShopController(ShopService shop) {
        this.shop = shop;
    }

    public record BuyRequest(String itemId, Integer quantity) {
    }

    public record SellRequest(String itemName, Integer quantity) {
    }

    @GetMapping
    public Map<String, Object> view(@PathVariable Long id) {
        return shop.view(id, SecurityUtils.requireUserId());
    }

    @PostMapping("/buy")
    public PlayerCharacter buy(@PathVariable Long id, @RequestBody BuyRequest request) {
        return shop.buy(id, SecurityUtils.requireUserId(), request.itemId(),
                request.quantity() == null ? 1 : request.quantity());
    }

    @PostMapping("/sell")
    public PlayerCharacter sell(@PathVariable Long id, @RequestBody SellRequest request) {
        return shop.sell(id, SecurityUtils.requireUserId(), request.itemName(),
                request.quantity() == null ? 1 : request.quantity());
    }
}
