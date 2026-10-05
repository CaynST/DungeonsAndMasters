import { Link } from "react-router-dom";
import { Users, MapPinned } from "lucide-react";
import type { Campaign } from "@/types/game";
import { Badge } from "@/components/ui/Badge";
import { cn } from "@/lib/utils";

interface Props {
  campaign: Campaign;
  className?: string;
}

export function CampaignCard({ campaign, className }: Props) {
  const statusTone = campaign.status === "ACTIVE" ? "emerald" : campaign.status === "FINISHED" ? "muted" : "gold";
  const statusLabel =
    campaign.status === "ACTIVE" ? "En curso" : campaign.status === "FINISHED" ? "Completada" : "En el lobby";

  return (
    <Link
      to={`/campaign/${campaign.id}/lobby`}
      className={cn(
        "group relative block overflow-hidden rounded-xl border border-parchment/10 bg-surface/80 shadow-panel transition-all hover:-translate-y-1 hover:border-gold/40",
        className,
      )}
    >
      <div className="relative h-40 overflow-hidden">
        <div
          className="absolute inset-0 bg-cover bg-center opacity-70 transition-transform duration-500 group-hover:scale-105"
          style={{ backgroundImage: `url(${campaign.image ?? "/images/campaigns/valdros.png"})` }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-surface via-surface/40 to-transparent" />
        <div className="absolute left-3 top-3">
          <Badge tone={statusTone}>{statusLabel}</Badge>
        </div>
        <h3 className="title-display absolute bottom-3 left-4 right-4 text-xl font-bold text-parchment text-glow-gold">
          {campaign.name}
        </h3>
      </div>

      <div className="space-y-2 p-4">
        <p className="line-clamp-2 text-sm text-muted">{campaign.setting}</p>
        <div className="flex items-center gap-4 text-xs text-muted">
          <span className="inline-flex items-center gap-1.5">
            <Users className="h-3.5 w-3.5 text-gold" />
            {campaign.members.length} aventureros
          </span>
          <span className="inline-flex items-center gap-1.5">
            <MapPinned className="h-3.5 w-3.5 text-arcane-soft" />
            {campaign.phase}
          </span>
        </div>
      </div>
    </Link>
  );
}
