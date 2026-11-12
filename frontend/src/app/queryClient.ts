import { QueryClient } from "@tanstack/react-query";
import { AuthenticationRequiredError } from "../data/shared/apiClient";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      // Retry once on other errors. Without a login session a retry would only call
      // GET /token again and get another 401, so the Login button is shown right away.
      retry: (failureCount, error) =>
        !(error instanceof AuthenticationRequiredError) && failureCount < 1,
      refetchOnWindowFocus: false,
    },
  },
});
