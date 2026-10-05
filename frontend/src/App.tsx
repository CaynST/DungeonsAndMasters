import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { Home } from "@/pages/Home";
import { Login } from "@/pages/Login";
import { Lobby } from "@/pages/Lobby";
import { Game } from "@/pages/Game";
import { CharacterCreation } from "@/pages/CharacterCreation";
import { RequireAuth } from "@/components/RequireAuth";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 2_000,
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <div className="app-backdrop min-h-screen">
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route
              path="/"
              element={
                <RequireAuth>
                  <Home />
                </RequireAuth>
              }
            />
            <Route
              path="/characters/new"
              element={
                <RequireAuth>
                  <CharacterCreation />
                </RequireAuth>
              }
            />
            <Route
              path="/campaign/:id/lobby"
              element={
                <RequireAuth>
                  <Lobby />
                </RequireAuth>
              }
            />
            <Route
              path="/campaign/:id/play"
              element={
                <RequireAuth>
                  <Game />
                </RequireAuth>
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
