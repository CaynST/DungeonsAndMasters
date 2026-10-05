import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { X } from "lucide-react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { cn } from "@/lib/utils";
import type { ShopItem } from "@/types/game";

const KIND_LABELS: Record<string, string> = {
  WEAPON: "Armas",
  ARMOR: "Armaduras",
  POTION: "Pociones",
  GEAR: "Equipo",
};

export function ShopModal({ campaignId, onClose }: { campaignId: number; onClose: () => void }) {
  const queryClient = useQueryClient();
  const shop = useQuery({ queryKey: ["shop", campaignId], queryFn: () => api.shop(campaignId) });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["shop", campaignId] });
    queryClient.invalidateQueries({ queryKey: ["state", campaignId] });
  };

  const buy = useMutation({
    mutationFn: (itemId: string) => api.buy(campaignId, itemId, 1),
    onSuccess: refresh,
  });
  const sell = useMutation({
    mutationFn: (itemName: string) => api.sell(campaignId, itemName, 1),
    onSuccess: refresh,
  });
  const equip = useMutation({
    mutationFn: (itemName: string) => api.equip(campaignId, itemName),
    onSuccess: refresh,
  });

  const equippable = new Set(
    (shop.data?.items ?? []).filter((i) => i.kind === "WEAPON" || i.kind === "ARMOR").map((i) => i.name),
  );

  const grouped = (shop.data?.items ?? []).reduce<Record<string, ShopItem[]>>((acc, item) => {
    (acc[item.kind] ??= []).push(item);
    return acc;
  }, {});

  return (
    <div className="absolute inset-0 z-40 flex items-center justify-center bg-black/70 p-4" onClick={onClose}>
      <div
        className="panel-raised flex max-h-[85vh] w-[min(94vw,820px)] flex-col overflow-hidden"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="flex items-center justify-between border-b border-[#2b2118] px-5 py-3">
          <div>
            <h2 className="title-display text-lg font-semibold text-ink">Mercader</h2>
            <p className="text-xs text-muted">
              Oro disponible:{" "}
              <span className="font-semibold text-[#e0b84a]">
                {shop.data ? `${shop.data.gold} po` : "—"}
              </span>
              {shop.data && (shop.data.silver > 0 || shop.data.copper > 0) && (
                <span className="ml-2 text-muted">
                  ({shop.data.silver} pp, {shop.data.copper} pc)
                </span>
              )}
            </p>
          </div>
          <Button variant="ghost" size="sm" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </header>

        <div className="grid min-h-0 flex-1 grid-cols-1 gap-0 overflow-hidden md:grid-cols-[1.6fr_1fr]">
          {/* Catalogue */}
          <div className="min-h-0 overflow-y-auto p-4">
            {shop.isLoading && (
              <p className="flex items-center gap-2 text-sm text-muted">
                <Spinner /> Abriendo el mercado...
              </p>
            )}
            {Object.entries(grouped).map(([kind, items]) => (
              <div key={kind} className="mb-4">
                <p className="hud-heading mb-2 text-sm">{KIND_LABELS[kind] ?? kind}</p>
                <div className="space-y-1.5">
                  {items.map((item) => {
                    const affordable = (shop.data?.gold ?? 0) >= item.price;
                    return (
                      <div
                        key={item.id}
                        className="flex items-center justify-between gap-3 rounded-md border border-[#2b2118] bg-[#100d0a] px-3 py-2"
                      >
                        <div className="min-w-0">
                          <p className="text-sm text-ink">{item.name}</p>
                          <p className="truncate text-xs text-muted">{item.description}</p>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className={cn("text-sm font-semibold", affordable ? "text-[#e0b84a]" : "text-[#d0566a]")}>
                            {item.price} po
                          </span>
                          <Button
                            size="sm"
                            variant="secondary"
                            loading={buy.isPending && buy.variables === item.id}
                            disabled={!affordable || buy.isPending}
                            onClick={() => buy.mutate(item.id)}
                          >
                            Comprar
                          </Button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>

          {/* Inventory */}
          <div className="min-h-0 overflow-y-auto border-t border-[#2b2118] p-4 md:border-l md:border-t-0">
            <p className="hud-heading mb-2 text-sm">Tu inventario</p>
            <div className="space-y-1.5">
              {(shop.data?.inventory ?? []).length === 0 && (
                <p className="text-xs text-muted">No llevas nada que vender.</p>
              )}
              {(shop.data?.inventory ?? []).map((item) => (
                <div
                  key={item.name}
                  className="flex items-center justify-between gap-2 rounded-md border border-[#2b2118] bg-[#100d0a] px-3 py-2"
                >
                  <span className="truncate text-sm text-ink">
                    {item.name}
                    {item.quantity > 1 && <span className="ml-1 text-xs text-muted">x{item.quantity}</span>}
                  </span>
                  <div className="flex items-center gap-1">
                    {equippable.has(item.name) && (
                      <Button
                        size="sm"
                        variant="secondary"
                        loading={equip.isPending && equip.variables === item.name}
                        disabled={equip.isPending}
                        onClick={() => equip.mutate(item.name)}
                      >
                        Equipar
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="ghost"
                      loading={sell.isPending && sell.variables === item.name}
                      disabled={sell.isPending}
                      onClick={() => sell.mutate(item.name)}
                    >
                      Vender
                    </Button>
                  </div>
                </div>
              ))}
            </div>
            {(buy.isError || sell.isError) && (
              <p className="mt-3 text-xs text-[#d0566a]">
                {((buy.error || sell.error) as Error)?.message}
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
