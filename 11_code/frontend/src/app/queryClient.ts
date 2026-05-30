import { QueryClient } from "@tanstack/react-query";

/**
 * 全局 QueryClient。
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false
    },
    mutations: {
      retry: 0
    }
  }
});
