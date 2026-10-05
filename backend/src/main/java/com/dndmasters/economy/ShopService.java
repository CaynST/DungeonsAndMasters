package com.dndmasters.economy;

import com.dndmasters.domain.InventoryItem;
import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.repository.CharacterRepository;
import com.dndmasters.service.CampaignService;
import com.dndmasters.service.GameEventService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Buy/sell logic for the campaign shop. Prices come from {@link ItemCatalog}. */
@Service
public class ShopService {

    private final CampaignService campaigns;
    private final CharacterRepository characters;
    private final CharacterMapper mapper;
    private final GameEventService events;

    public ShopService(CampaignService campaigns, CharacterRepository characters, CharacterMapper mapper,
                       GameEventService events) {
        this.campaigns = campaigns;
        this.characters = characters;
        this.mapper = mapper;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> view(Long campaignId, Long userId) {
        PlayerCharacter character = characterOf(campaignId, userId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("characterId", character.getId());
        out.put("gold", character.getGold());
        out.put("silver", character.getSilver());
        out.put("copper", character.getCopper());
        out.put("items", ItemCatalog.all());
        out.put("inventory", character.getInventory());
        return out;
    }

    @Transactional
    public PlayerCharacter buy(Long campaignId, Long userId, String itemId, int quantity) {
        int qty = Math.max(1, quantity);
        ItemCatalog.ItemDef item = ItemCatalog.byId(itemId);
        PlayerCharacter character = characterOf(campaignId, userId);
        int cost = item.price() * qty;
        if (totalCopper(character) < cost * 100L) {
            throw new IllegalStateException("No tienes suficiente oro para " + item.name());
        }
        spendCopper(character, cost * 100L);
        addToInventory(character, item, qty);
        save(character);
        events.record(campaignId, null, "PURCHASE", character.getName(),
                character.getName() + " compra " + qty + "x " + item.name() + " por " + cost + " po.", Map.of());
        return character;
    }

    @Transactional
    public PlayerCharacter sell(Long campaignId, Long userId, String itemName, int quantity) {
        int qty = Math.max(1, quantity);
        PlayerCharacter character = characterOf(campaignId, userId);
        List<InventoryItem> inventory = new ArrayList<>(character.getInventory());
        InventoryItem found = inventory.stream()
                .filter(i -> i.name().equalsIgnoreCase(itemName)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No tienes " + itemName));
        int available = found.quantity();
        int sold = Math.min(qty, available);
        ItemCatalog.ItemDef catalogItem = ItemCatalog.byName(found.name());
        int earned = (catalogItem != null ? ItemCatalog.sellPrice(catalogItem) : 1) * sold;

        inventory.remove(found);
        if (available - sold > 0) {
            inventory.add(new InventoryItem(found.name(), found.description(), found.icon(), available - sold));
        }
        character.setInventory(inventory);
        addCopper(character, earned * 100L);
        save(character);
        events.record(campaignId, null, "SALE", character.getName(),
                character.getName() + " vende " + sold + "x " + found.name() + " por " + earned + " po.", Map.of());
        return character;
    }

    private void addToInventory(PlayerCharacter character, ItemCatalog.ItemDef item, int qty) {
        List<InventoryItem> inventory = new ArrayList<>(character.getInventory());
        InventoryItem existing = inventory.stream()
                .filter(i -> i.name().equalsIgnoreCase(item.name())).findFirst().orElse(null);
        if (existing != null) {
            inventory.remove(existing);
            inventory.add(new InventoryItem(existing.name(), existing.description(), existing.icon(),
                    existing.quantity() + qty));
        } else {
            inventory.add(new InventoryItem(item.name(), item.description(), item.icon(), qty));
        }
        character.setInventory(inventory);
    }

    private PlayerCharacter characterOf(Long campaignId, Long userId) {
        Long characterId = campaigns.characterIdForUser(campaignId, userId);
        if (characterId == null) {
            throw new IllegalStateException("No tienes un personaje en esta campaña");
        }
        CharacterEntity entity = characters.findById(characterId)
                .orElseThrow(() -> new IllegalStateException("Personaje no encontrado"));
        return mapper.toDomain(entity);
    }

    private void save(PlayerCharacter character) {
        CharacterEntity entity = mapper.toEntity(character);
        entity.setCreatedAt(entity.getCreatedAt() == null ? java.time.Instant.now().toString() : entity.getCreatedAt());
        characters.save(entity);
    }

    private long totalCopper(PlayerCharacter character) {
        return character.getGold() * 100L + character.getSilver() * 10L + character.getCopper();
    }

    private void spendCopper(PlayerCharacter character, long copper) {
        setFromCopper(character, totalCopper(character) - copper);
    }

    private void addCopper(PlayerCharacter character, long copper) {
        setFromCopper(character, totalCopper(character) + copper);
    }

    private void setFromCopper(PlayerCharacter character, long copper) {
        long remaining = Math.max(0, copper);
        character.setGold((int) (remaining / 100));
        character.setSilver((int) ((remaining % 100) / 10));
        character.setCopper((int) (remaining % 10));
    }
}
