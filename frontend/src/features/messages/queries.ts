import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getMessages, sendMessage } from '../../api/messages';

export function useMessages(applicationId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ['messages', applicationId],
    queryFn: () => getMessages(applicationId as string),
    enabled: Boolean(applicationId) && enabled,
    refetchInterval: enabled ? 10_000 : false,
    refetchIntervalInBackground: false,
  });
}

export function useSendMessage(applicationId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: string) => sendMessage(applicationId, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['messages', applicationId] });
      qc.invalidateQueries({ queryKey: ['notifications'] });
    },
  });
}
