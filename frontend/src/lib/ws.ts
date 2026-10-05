import type { GameEvent } from "@/types/game";

const WS_URL = `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws`;

/**
 * Minimal STOMP-over-WebSocket client. We only need SUBSCRIBE on the campaign topic,
 * so a hand-rolled client avoids pulling in a full STOMP dependency.
 */
export interface GameSocket {
  close: () => void;
}

export function connectCampaignSocket(
  campaignId: number,
  onEvent: (event: GameEvent) => void,
  onReconnect?: () => void,
): GameSocket {
  let socket: WebSocket | null = null;
  let closed = false;
  let hasConnected = false;

  const connect = () => {
    if (closed) return;
    socket = new WebSocket(WS_URL);

    socket.onopen = () => {
      // STOMP CONNECT frame.
      socket?.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\u0000");
    };

    socket.onmessage = (message) => {
      const frame = message.data as string;
      if (frame.startsWith("CONNECTED")) {
        // Subscribe to the campaign topic.
        socket?.send(
          `SUBSCRIBE\nid:sub-${campaignId}\ndestination:/topic/campaign/${campaignId}\n\n\u0000`,
        );
        if (hasConnected) {
          onReconnect?.();
        }
        hasConnected = true;
        return;
      }
      if (frame.startsWith("MESSAGE")) {
        const separator = frame.indexOf("\n\n");
        if (separator === -1) return;
        const body = frame.slice(separator + 2).replace(/\u0000$/, "");
        try {
          onEvent(JSON.parse(body) as GameEvent);
        } catch {
          // Ignore malformed frames.
        }
      }
    };

    socket.onclose = () => {
      if (!closed) {
        setTimeout(connect, 2000);
      }
    };
    socket.onerror = () => socket?.close();
  };

  connect();

  return {
    close: () => {
      closed = true;
      socket?.close();
    },
  };
}
