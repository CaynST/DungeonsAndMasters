import { Link } from "react-router-dom";
import { ArrowRight, Crown, MapPinned, Users } from "lucide-react";
import type { Campaign } from "@/types/game";
import { Badge } from "@/components/ui/Badge";
import { Avatar } from "@/components/ui/Avatar";
import { campaignPhaseLabel, campaignStatusLabel, campaignStatusTone } from "@/lib/campaigns";
import { cn } from "@/lib/utils";

interface Props {
  campaign: Campaign;
  className?: string;
}

export function CampaignCard({ campaign, className }: Props) {
  const memberPreview = campaign.members.slice(0, 3);

  return (
    <Link
      to={`/campaign/${campaign.id}/lobby`}
      className={cn(
        "group relative flex h-full flex-col overflow-hidden rounded-xl border border-parchment/10 bg-surface/80 shadow-panel transition-all",
        "hover:-translate-y-1 hover:border-gold/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold/60 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0b0907]",
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
          <Badge tone={campaignStatusTone(campaign.status)}>{campaignStatusLabel(campaign.status)}</Badge>
        </div>
        <div className="absolute right-3 top-3" title={campaign.dmName}>
          <Avatar
            name={campaign.dmName}
            src={campaign.dmAvatar}
            size="xs"
            className="shadow-panel ring-2 ring-[#0b0907]"
          />
        </div>
        <h3 className="title-display absolute bottom-3 left-4 right-4 text-xl font-bold text-parchment text-glow-gold">
          {campaign.name}
        </h3>
      </div>

      <div className="flex flex-1 flex-col gap-3 p-4">
        <p className="line-clamp-2 text-sm leading-relaxed text-muted">{campaign.setting}</p>

        <div className="mt-auto space-y-3">
          <div className="flex items-center justify-between gap-2 text-xs text-muted">
            <span className="inline-flex min-w-0 items-center gap-1.5">
              <Crown className="h-3.5 w-3.5 shrink-0 text-gold" aria-hidden="true" />
              <span className="truncate">{campaign.dmName}</span>
            </span>
            <span className="inline-flex shrink-0 items-center gap-1.5">
              <MapPinned className="h-3.5 w-3.5 text-arcane-soft" aria-hidden="true" />
              {campaignPhaseLabel(campaign.phase)}
            </span>
          </div>

          <div className="flex items-center justify-between gap-3 border-t border-parchment/10 pt-3">
            <span className="inline-flex items-center gap-2 text-xs text-muted">
              {memberPreview.length > 0 ? (
                <span className="flex -space-x-2">
                  {memberPreview.map((member) => (
                    <Avatar
                      key={member.id}
                      name={member.displayName}
                      size="xs"
                      className="ring-2 ring-surface"
                    />
                  ))}
                </span>
              ) : (
                <Users className="h-3.5 w-3.5 text-muted" aria-hidden="true" />
              )}
              <span>
                {campaign.members.length} aventurero{campaign.members.length === 1 ? "" : "s"}
              </span>
            </span>
            <span className="inline-flex items-center gap-1 text-xs font-medium text-gold-soft opacity-70 transition group-hover:opacity-100">
              Entrar
              <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
            </span>
          </div>
        </div>
      </div>
    </Link>
  );
}
